package comp3011.assignment1.service;

//coordinates transcription requests between the external client and global statistics.
//it also records the main operational events without logging sensitive content.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import comp3011.assignment1.client.TranscriptionClient;
import comp3011.assignment1.model.AudioData;
import reactor.core.publisher.Mono;

import comp3011.assignment1.model.SubtitleResponse;
import comp3011.assignment1.subtitle.SubtitleResponseParser;
import comp3011.assignment1.SubtitleFormatter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import comp3011.assignment1.subtitle.AudioExcerptMatcher;
import java.util.List;
import java.util.Optional;
import reactor.core.scheduler.Schedulers;


@Service
public class TranscriptionService {
	private final TranscriptionClient transcriptionClient;
	private final GlobalStatisticsService globalStatisticsService;
	private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);
	private final AudioUsageService audioUsageService;
	
	private final SubtitleResponseParser subtitleResponseParser;
	
	private static final int MAX_CACHED_TRANSCRIPTIONS = 32;

	private static final long MAX_CACHED_AUDIO_BYTES = 64L * 1024 * 1024;

	private record CachedTranscription(SubtitleResponse response, byte[] audio) {
	}

	private final AudioExcerptMatcher excerptMatcher = new AudioExcerptMatcher();

	// bound the audio retained for future excerpt searches
	private final Map<String, CachedTranscription> cachedSubtitles =
	        new LinkedHashMap<>(16, 0.75f, true);
	private long cachedAudioBytes;
	
	private final Map<String, Mono<SubtitleResponse>> pendingSubtitles =
	        new HashMap<>();
	
	// format captions before the response is stored in the cache
	private final SubtitleFormatter subtitleFormatter = new SubtitleFormatter();

	public TranscriptionService(TranscriptionClient transcriptionClient,
			GlobalStatisticsService globalStatisticsService, SubtitleResponseParser subtitleResponseParser,
			AudioUsageService audioUsageService) {
		this.transcriptionClient = transcriptionClient;
		this.globalStatisticsService = globalStatisticsService;
		this.subtitleResponseParser = subtitleResponseParser;
		this.audioUsageService = audioUsageService;
	}

	public Mono<String> transcribe(AudioData audio) {
		logger.info("Transcription request started");

		return transcriptionClient.transcribe(audio).map(response -> {
			// record token usage only after a successful transcription
			globalStatisticsService.addUsage(
					response.usage().inputTokens(), 
					response.usage().outputTokens()
			);
			logger.info("Transcription request completed");

			return response.text();
		}).doOnError(error -> logger.error("Transcription request failed: {}", error.getClass().getSimpleName()));
	}
	
	// subtitles
	private Mono<SubtitleResponse> requestSubtitlesFromWhisper(AudioData audio) {
	    logger.info("Subtitle transcription request started");
	
	    return transcriptionClient.transcribeWithTimestamps(audio)
	            .map(subtitleResponseParser::parse)
	            .doOnNext(response -> {
	                // count audio duration only after successful transcription
	                audioUsageService.addUsage(response.duration());
	
	                logger.info(
	                        "Subtitle transcription completed: {} seconds",
	                        response.duration()
	                );
	            })
	            .map(response -> new SubtitleResponse(
	                    response.duration(),
	                    response.segments().stream()
	                            .flatMap(segment -> subtitleFormatter.format(segment).stream())
	                            .toList()))
	            .doOnError(error ->
	                    logger.error("Subtitle transcription failed", error));
	}
	
	public Mono<SubtitleResponse> transcribeSubtitles(AudioData audio) {
		// look up the audio when spring subscribes to the request
	    return Mono.defer(() -> findOrRequestSubtitles(audioKey(audio), audio));
	}
	
	private synchronized Mono<SubtitleResponse> findOrRequestSubtitles(
	        String key, AudioData audio) {

	    // serve identical uploads and share work already in progress
	    CachedTranscription cached = cachedSubtitles.get(key);
	    if (cached != null) {
	        return Mono.just(cached.response());
	    }

	    Mono<SubtitleResponse> pending = pendingSubtitles.get(key);
	    if (pending != null) {
	        return pending;
	    }

	    // take a snapshot so audio matching runs outside the cache lock
	    List<CachedTranscription> sources = List.copyOf(cachedSubtitles.values());

	    Mono<SubtitleResponse> request = Mono
	            .fromCallable(() -> findExcerpt(sources, audio.data()))
	            .subscribeOn(Schedulers.boundedElastic())
	            .flatMap(match -> {
	                if (match.isPresent()) {
	                    return Mono.just(match.get());
	                }

	                // retain full source audio only after a fresh transcription
	                return requestSubtitlesFromWhisper(audio)
	                        .doOnNext(response ->
	                                storeSubtitles(key, audio.data(), response));
	            })
	            .switchIfEmpty(Mono.error(
	                    new IllegalStateException("No subtitle response was produced")))
	            .doOnSuccess(response -> clearPendingSubtitles(key))
	            .doOnError(error -> clearPendingSubtitles(key))
	            .cache();

	    pendingSubtitles.put(key, request);
	    return request;
	}

	private Optional<SubtitleResponse> findExcerpt(
	        List<CachedTranscription> sources, byte[] excerpt) {
	    Optional<SubtitleResponse> match = Optional.empty();

	    for (CachedTranscription source : sources) {
	        Optional<SubtitleResponse> candidate =
	                excerptMatcher.reuse(source.audio(), excerpt, source.response());

	        if (candidate.isPresent()) {
	            // use whisper if more than one cached source matches
	            if (match.isPresent()) {
	                return Optional.empty();
	            }
	            match = candidate;
	        }
	    }

	    return match;
	}
	
	private synchronized void storeSubtitles(
	        String key, byte[] audio, SubtitleResponse response) {
	    if (audio.length > MAX_CACHED_AUDIO_BYTES) {
	        return;
	    }

	    // account for the bytes retained by the completed cache
	    CachedTranscription previous = cachedSubtitles.put(
	            key, new CachedTranscription(response, audio));
	    if (previous != null) {
	        cachedAudioBytes -= previous.audio().length;
	    }
	    cachedAudioBytes += audio.length;

	    // evict least recently used entries until both limits are met
	    while (cachedSubtitles.size() > MAX_CACHED_TRANSCRIPTIONS
	            || cachedAudioBytes > MAX_CACHED_AUDIO_BYTES) {
	        String oldestKey = cachedSubtitles.keySet().iterator().next();
	        CachedTranscription removed = cachedSubtitles.remove(oldestKey);
	        cachedAudioBytes -= removed.audio().length;
	    }
	}
	
	private synchronized void clearPendingSubtitles(String key) {
	    // allow a later upload to retry after a failed request
	    pendingSubtitles.remove(key);
	}
	
	private static String audioKey(AudioData audio) {
	    // hash the uploaded bytes so renamed files can share a cache entry
	    try {
	        byte[] digest = MessageDigest.getInstance("SHA-256").digest(audio.data());
	        return HexFormat.of().formatHex(digest);
	    } catch (NoSuchAlgorithmException error) {
	        throw new IllegalStateException("SHA-256 is unavailable", error);
	    }
	}
}
