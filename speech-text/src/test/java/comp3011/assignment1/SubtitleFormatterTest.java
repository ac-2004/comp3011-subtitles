
package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.model.SubtitleSegment;

public class SubtitleFormatterTest {

    private final SubtitleFormatter formatter = new SubtitleFormatter();

    // verify long text wraps without exceeding 42 characters per line
    @Test
    void testWrapLongText() {
        String input = "Welcome everyone to today's demonstration "
            + "of our concurrent video processing application.";

        List<String> lines = formatter.wrapLines(input);

        assertTrue(lines.size() > 1);
        assertTrue(lines.stream().allMatch(line -> line.length() <= 42));

        // verify line wrapping preserves every word in its original order
        assertEquals(input, String.join(" ", lines));
    }

    // verify short text fits on one line without modification
    @Test
    void testShortText() {
        assertEquals(
            List.of("Hello everyone!"),
            formatter.wrapLines("Hello everyone!")
        );
    }

    // verify whitespace-only text produces no subtitle lines
    @Test
    void testEmptyText() {
        assertTrue(formatter.wrapLines("   ").isEmpty());
    }

    // verify a word containing exactly 42 characters is accepted
    @Test
    void testLineBoundary() {
        String input = "a".repeat(42);

        assertEquals(
            List.of(input),
            formatter.wrapLines(input)
        );
    }

    // verify long speech is split into captions of at most two lines
    @Test
    void testFormatLongSubtitle() {
        SubtitleSegment input = new SubtitleSegment(
            0,
            12_000_000,
            "Welcome everyone to today's demonstration where we "
            + "will explore how concurrent video processing works "
            + "and how subtitles can be generated automatically "
            + "using speech recognition and timestamps."
        );

        List<SubtitleSegment> result = formatter.format(input);

        assertTrue(result.size() > 1);

        // verify every caption follows the line and timing constraints
        for (SubtitleSegment segment : result) {
            String[] lines = segment.text().split("\n");

            assertTrue(lines.length <= 2);

            for (String line : lines) {
                assertTrue(line.length() <= 42);
            }

            assertTrue(segment.endUs() > segment.startUs());
        }

        // verify splitting does not remove or reorder the original words
        String reconstructed = result.stream()
            .map(segment -> segment.text().replace("\n", " "))
            .collect(Collectors.joining(" "));

        assertEquals(input.text(), reconstructed);
    }

    // verify a short subtitle retains its text and original timestamps
    @Test
    void testFormatShortSubtitle() {
        SubtitleSegment input = new SubtitleSegment(
            2_000_000,
            5_000_000,
            "Hello everyone!"
        );

        List<SubtitleSegment> result = formatter.format(input);

        assertEquals(1, result.size());
        assertEquals(input, result.get(0));
    }

    // verify consecutive captions have no gaps or overlapping timestamps
    @Test
    void testContinuousTimestamps() {
        SubtitleSegment input = new SubtitleSegment(
            1_000_000,
            11_000_000,
            "This is a longer subtitle containing enough words "
            + "to generate several lines that will need to be "
            + "displayed across multiple caption segments."
        );

        List<SubtitleSegment> result = formatter.format(input);

        assertTrue(result.size() > 1);

        // verify the complete interval matches Whisper's original timestamps
        assertEquals(input.startUs(), result.get(0).startUs());
        assertEquals(input.endUs(), result.get(result.size() - 1).endUs());

        // verify each caption starts when its predecessor finishes
        for (int i = 1; i < result.size(); i++) {
            assertEquals(
                result.get(i - 1).endUs(),
                result.get(i).startUs()
            );
        }
    }

    // verify blank subtitle segments are excluded from the result
    @Test
    void testEmptySubtitle() {
        SubtitleSegment input = new SubtitleSegment(
            0,
            3_000_000,
            "   "
        );

        assertTrue(formatter.format(input).isEmpty());
    }

    // verify invalid timestamp intervals are rejected
    @Test
    void testInvalidTimestamps() {
        SubtitleSegment input = new SubtitleSegment(
            5_000_000,
            2_000_000,
            "Invalid timing"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> formatter.format(input)
        );
    }
    

	 // verify an excessively long caption is split into shorter timed captions
	 @Test
	 void testSplitsLongDuration() {
	     SubtitleSegment input = new SubtitleSegment(
	         0,
	         14_000_000,
	         "This demonstration explains how automatic subtitles "
	         + "are generated and synchronised with video playback."
	     );
	
	     List<SubtitleSegment> result = formatter.format(input);
	
	     assertTrue(result.size() >= 2);
	
	     // verify every caption satisfies the duration limits
	     for (SubtitleSegment caption : result) {
	         long duration = caption.endUs() - caption.startUs();
	
	         assertTrue(duration >= 1_000_000);
	         assertTrue(duration <= 7_000_000);
	     }
	
	     // verify the split preserves every word
	     String reconstructed = result.stream()
	         .map(caption -> caption.text().replace("\n", " "))
	         .collect(Collectors.joining(" "));
	
	     assertEquals(input.text(), reconstructed);
	 }
	
	 // verify captions already within the duration limits remain unchanged
	 @Test
	 void testPreservesValidDuration() {
	     SubtitleSegment input = new SubtitleSegment(
	         2_000_000,
	         5_000_000,
	         "Hello everyone!"
	     );
	
	     assertEquals(List.of(input), formatter.format(input));
	 }
	
	 // verify a short Whisper interval is preserved rather than artificially extended
	 @Test
	 void testPreservesShortWhisperInterval() {
	     SubtitleSegment input = new SubtitleSegment(
	         0,
	         500_000,
	         "Yes!"
	     );
	
	     List<SubtitleSegment> result = formatter.format(input);
	
	     assertEquals(1, result.size());
	     assertEquals(500_000, result.get(0).endUs());
	 }
	
	 // verify the original time interval remains intact after duration splitting
	 @Test
	 void testPreservesOriginalTimeBoundaries() {
	     SubtitleSegment input = new SubtitleSegment(
	         3_000_000,
	         23_000_000,
	         "We are testing how automatically generated captions "
	         + "can be divided into shorter segments while preserving "
	         + "their original content and keeping the timestamps "
	         + "consistent throughout the complete video."
	     );
	
	     List<SubtitleSegment> result = formatter.format(input);
	
	     assertEquals(input.startUs(), result.get(0).startUs());
	     assertEquals(input.endUs(), result.get(result.size() - 1).endUs());
	
	     // verify consecutive captions do not overlap or leave gaps
	     for (int i = 1; i < result.size(); i++) {
	         assertEquals(
	             result.get(i - 1).endUs(),
	             result.get(i).startUs()
	         );
	     }
	 }
	 
	// verify an overlong word wraps without losing any characters
	 @Test
	 void testWrapsOverlongWord() {
	     String word = "a".repeat(95);

	     List<String> lines = formatter.wrapLines(word);

	     assertEquals(List.of(42, 42, 11),
	             lines.stream().map(String::length).toList());
	     assertEquals(word, String.join("", lines));
	 }
}
