package comp3011.assignment1.subtitle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AudioExcerptMatcherTest {

    // verify the matcher returns a nonzero offset in sample frames
    @Test
    void findsExcerptAtFrameOffset() {
        byte[] source = {10, 0, 20, 0, 30, 0, 40, 0};
        byte[] excerpt = {20, 0, 30, 0};

        assertEquals(1, AudioExcerptMatcher.findUniqueOffset(source, excerpt, 2));
    }

    // verify changed samples cause a cache miss
    @Test
    void rejectsDifferentSamples() {
        byte[] source = {10, 0, 20, 0, 30, 0, 40, 0};
        byte[] excerpt = {20, 0, 31, 0};

        assertEquals(-1, AudioExcerptMatcher.findUniqueOffset(source, excerpt, 2));
    }

    // verify repeated audio cannot select an ambiguous caption offset
    @Test
    void rejectsMultipleMatches() {
        byte[] source = {10, 0, 20, 0, 10, 0, 20, 0};
        byte[] excerpt = {10, 0, 20, 0};

        assertEquals(-1, AudioExcerptMatcher.findUniqueOffset(source, excerpt, 2));
    }
}