package comp3011;

import java.io.File;
import java.io.IOException;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE;

public class AudioExtractor {
	public File extractAudio(File videoFile) throws Exception {
		// create audio file without modifying original
		File audioFile = File.createTempFile("subtitle-audio-", ".wav");
		
		try {
			// open independent decoder
			try (FFmpegFrameGrabber grabber = 
					new FFmpegFrameGrabber(videoFile)) {
				grabber.setSampleMode(
						org.bytedeco.javacv.FrameGrabber.SampleMode.SHORT);
				
				grabber.start();
				
				// check if audio exists
				if (!grabber.hasAudio()) {
					throw new IOException("Video contains no audio");
				}
				
				int sampleRate = grabber.getSampleRate();
				int channels = grabber.getAudioChannels();
				
				try (FFmpegFrameRecorder recorder =
						new FFmpegFrameRecorder(
								audioFile.getAbsolutePath(), channels)) {
					
					recorder.setFormat("wav");
                    recorder.setAudioCodec(AV_CODEC_ID_PCM_S16LE);
                    recorder.setSampleRate(sampleRate);
                    recorder.setAudioChannels(channels);

                    recorder.start();
                    
                    Frame frame;
                    
                    // retrieve audio samples instead of video frames
                    while ((frame = grabber.grabSamples()) != null) {
                        if (frame.samples != null) {
                            recorder.recordSamples(
                                    sampleRate,
                                    channels,
                                    frame.samples
                            );
                        }
                    }
				}
				
			} 
			
			return audioFile;
			
		} catch (Exception error) {
			audioFile.delete();
			throw error;
		}
	}
}
