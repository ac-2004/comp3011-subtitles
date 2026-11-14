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

@Service
public class TranscriptionService {
	private final TranscriptionClient transcriptionClient;
	private final GlobalStatisticsService globalStatisticsService;
	private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);
	
	private final SubtitleResponseParser subtitleResponseParser;

	public TranscriptionService(TranscriptionClient transcriptionClient,
			GlobalStatisticsService globalStatisticsService, SubtitleResponseParser subtitleResponseParser) {
		this.transcriptionClient = transcriptionClient;
		this.globalStatisticsService = globalStatisticsService;
		this.subtitleResponseParser = subtitleResponseParser;
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
	
	// with subtitles
	public Mono<SubtitleResponse> transcribeSubtitles(AudioData audio) {
		logger.info("Subtitle transcription request started");
		return transcriptionClient.transcribeWithTimestamps(audio)
				.map(subtitleResponseParser::parse)
				.doOnSuccess(response ->
				logger.info("Subtitle transcription completed"))
				.doOnError(error -> {
				    if (error instanceof
				            org.springframework.web.reactive.function.client.WebClientResponseException responseError) {

				        logger.error(
				            "Whisper API error: status={}, body={}",
				            responseError.getStatusCode(),
				            responseError.getResponseBodyAsString()
				        );

				    } else {
				        logger.error("Whisper request failed", error);
				    }
				});
	}
}
