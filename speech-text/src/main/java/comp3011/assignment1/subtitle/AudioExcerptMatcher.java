package comp3011.assignment1.subtitle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import comp3011.assignment1.model.SubtitleResponse;
import comp3011.assignment1.model.SubtitleSegment;

public class AudioExcerptMatcher {
	public Optional<SubtitleResponse> reuse(
			byte[] sourceWav, byte[] excerptWav, SubtitleResponse sourceResponse) {
		try (AudioInputStream source = openAudio(sourceWav);
	             AudioInputStream excerpt = openAudio(excerptWav)) {

	            AudioFormat format = source.getFormat();
	            AudioFormat excerptFormat = excerpt.getFormat();
	            
	            // compare sample frames only when both files use the same pcm layout
	            if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
	                    || format.getSampleSizeInBits() != 16
	                    || format.getFrameSize() <= 0
	                    || format.getFrameRate() <= 0
	                    || !format.matches(excerptFormat)
	                    || !excerptFormat.matches(format)) {
	                return Optional.empty();
	            }
	            
	            int frameSize = format.getFrameSize();
	            byte[] sourcePcm = source.readAllBytes();
	            byte[] excerptPcm = excerpt.readAllBytes();
	            int offset = findUniqueOffset(sourcePcm, excerptPcm, frameSize);
	            
	            if (offset < 0) {
	            	return Optional.empty();
	            }
	            
	            int excerptFrames = excerptPcm.length / frameSize;
	            double framesPerSecond = format.getFrameRate();
	            long startUs = Math.round(offset * 1_000_000.0 / framesPerSecond);
	            long durationUs = Math.round(excerptFrames * 1_000_000.0 / framesPerSecond);
	            long endUs = startUs + durationUs;
	            
	            // clip overlapping captions and express their times relative to the excerpt
	            List<SubtitleSegment> shifted = new ArrayList<>();
	            for (SubtitleSegment segment : sourceResponse.segments()) {
	                long start = Math.max(segment.startUs(), startUs);
	                long end = Math.min(segment.endUs(), endUs);
	                if (start < end) {
	                    shifted.add(new SubtitleSegment(
	                            start - startUs, end - startUs, segment.text()));
	                }
	            }

	            return Optional.of(new SubtitleResponse(
	                    excerptFrames / framesPerSecond, List.copyOf(shifted)));
	
		} catch (IOException | UnsupportedAudioFileException error) {
            // let the regular transcription path handle unsupported audio
            return Optional.empty();
        }
	}
	
	private static AudioInputStream openAudio(byte[] wav) 
            throws IOException, UnsupportedAudioFileException {
        return AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav));
    }

	static int findUniqueOffset(byte[] source, byte[] excerpt, int frameSize) {
        // require complete sample frames and a strictly shorter excerpt
        if (excerpt.length == 0 || excerpt.length >= source.length
                || excerpt.length % frameSize != 0 || source.length % frameSize != 0) {
            return -1;
        }

        int excerptFrames = excerpt.length / frameSize;
        int[] prefix = new int[excerptFrames];

        // remember how much of a partial match can be reused after a mismatch
        for (int frame = 1, matched = 0; frame < excerptFrames; frame++) {
            while (matched > 0 && !sameFrame(excerpt, frame, excerpt, matched, frameSize)) {
                matched = prefix[matched - 1];
            }
            if (sameFrame(excerpt, frame, excerpt, matched, frameSize)) {
                matched++;
            }
            prefix[frame] = matched;
        }

        int offset = -1;
        for (int frame = 0, matched = 0; frame < source.length / frameSize; frame++) {
            while (matched > 0 && !sameFrame(source, frame, excerpt, matched, frameSize)) {
                matched = prefix[matched - 1];
            }
            if (sameFrame(source, frame, excerpt, matched, frameSize)) {
                matched++;
            }
            if (matched == excerptFrames) {
                // reject repeated matches because they imply different caption offsets
                if (offset >= 0) {
                    return -1;
                }
                offset = frame - excerptFrames + 1;
                matched = prefix[matched - 1];
            }
        }

        return offset;
    }
	
	private static boolean sameFrame(
            byte[] first, int firstFrame, byte[] second, int secondFrame, int frameSize) {
        int firstStart = firstFrame * frameSize;
        int secondStart = secondFrame * frameSize;
        return Arrays.equals(
                first, firstStart, firstStart + frameSize,
                second, secondStart, secondStart + frameSize);
    }
}