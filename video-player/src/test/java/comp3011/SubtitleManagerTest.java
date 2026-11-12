package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;


public class SubtitleManagerTest {

	@Test
	void testSubtitleLookup() {
		SubtitleManager manager = new SubtitleManager();
		
		assertEquals("", manager.getSubtitleAt(1_000_000));
		assertEquals("Hello", manager.getSubtitleAt(2_000_000));
		assertEquals("Hello", manager.getSubtitleAt(4_000_000));
		assertEquals("Welcome", manager.getSubtitleAt(5_000_000));
		assertEquals("Goodbye", manager.getSubtitleAt(9_000_000));
		assertEquals("", manager.getSubtitleAt(11_000_000));
	}
}
