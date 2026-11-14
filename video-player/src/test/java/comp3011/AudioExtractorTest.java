
package comp3011;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.junit.jupiter.api.Test;

public class AudioExtractorTest {

    @Test
    void extractsAudioFromVideo() throws Exception {

        // Verifies that a video with audio produces a valid WAV file.
    	File video = new File(
    		    "/Users/ayushchauhan/Desktop/test.mp4"
    		);

        assertTrue(video.isFile(), "Test video not found");

        AudioExtractor extractor = new AudioExtractor();
        File audio = extractor.extractAudio(video);

        try {
            assertTrue(audio.exists());
            assertTrue(audio.length() > 44, "WAV contains no audio data");

            try (FFmpegFrameGrabber grabber =
                    new FFmpegFrameGrabber(audio)) {

                grabber.start();

                assertTrue(grabber.hasAudio(), "WAV has no audio stream");
                assertTrue(grabber.getSampleRate() > 0);

                System.out.println(
                    "Extracted audio: " + audio.getAbsolutePath()
                );
                System.out.println(
                    "Audio size: " + audio.length() + " bytes"
                );
            }

        } finally {
            audio.delete();
        }
    }
}
