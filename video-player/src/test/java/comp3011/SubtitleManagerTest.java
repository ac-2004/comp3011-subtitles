
package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class SubtitleManagerTest {

	// correct subtitles appear at correct timestamps
    @Test
    void testSubtitleLookup() {
        // Verify subtitle lookup at different timestamps.
        SubtitleManager manager = new SubtitleManager();

        manager.setSegments(List.of(
            new SubtitleSegment(2_000_000, 5_000_000, "Hello"),
            new SubtitleSegment(5_000_000, 8_000_000, "Welcome"),
            new SubtitleSegment(8_000_000, 11_000_000, "Goodbye")
        ));

        assertEquals("", manager.getSubtitleAt(1_000_000));
        assertEquals("Hello", manager.getSubtitleAt(2_000_000));
        assertEquals("Hello", manager.getSubtitleAt(4_000_000));
        assertEquals("Welcome", manager.getSubtitleAt(5_000_000));
        assertEquals("Goodbye", manager.getSubtitleAt(9_000_000));
        assertEquals("", manager.getSubtitleAt(11_000_000));
    }

    // no hardcoded captions appear before transcription
    @Test
    void testInitiallyEmpty() {
        // Verify that subtitles are empty before transcription.
        SubtitleManager manager = new SubtitleManager();

        assertEquals("", manager.getSubtitleAt(2_000_000));
    }

    // new transcription results replace previous events
    @Test
    void testReplacingSubtitles() {
        // Verify that a new transcription replaces previous subtitles.
        SubtitleManager manager = new SubtitleManager();

        manager.setSegments(List.of(
            new SubtitleSegment(0, 3_000_000, "Old subtitle")
        ));

        manager.setSegments(List.of(
            new SubtitleSegment(0, 3_000_000, "New subtitle")
        ));

        assertEquals("New subtitle", manager.getSubtitleAt(1_000_000));
    }
}
