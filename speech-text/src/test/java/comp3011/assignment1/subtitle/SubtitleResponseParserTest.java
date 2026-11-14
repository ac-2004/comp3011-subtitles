
package comp3011.assignment1.subtitle;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.model.SubtitleResponse;
import comp3011.assignment1.model.SubtitleSegment;

public class SubtitleResponseParserTest {

    @Test
    void parsesAndFiltersWhisperSegments() {

        // Verifies timestamp conversion and unreliable segment filtering.
        String json = """
            {
              "duration": 10.0,
              "text": "Hello everyone. Welcome to the video.",
              "segments": [
                {
                  "start": 0.5,
                  "end": 2.2,
                  "text": "Hello everyone.",
                  "no_speech_prob": 0.03,
                  "avg_logprob": -0.2
                },
                {
                  "start": 3.0,
                  "end": 5.5,
                  "text": "Welcome to the video.",
                  "no_speech_prob": 0.02,
                  "avg_logprob": -0.3
                },
                {
                  "start": 6.0,
                  "end": 7.0,
                  "text": "Unreliable text",
                  "no_speech_prob": 0.9,
                  "avg_logprob": -1.5
                }
              ]
            }
            """;

        SubtitleResponseParser parser = new SubtitleResponseParser();
        SubtitleResponse response = parser.parse(json);

        assertEquals(10.0, response.duration());
        assertEquals(2, response.segments().size());

        SubtitleSegment first = response.segments().get(0);
        assertEquals(500_000L, first.startUs());
        assertEquals(2_200_000L, first.endUs());
        assertEquals("Hello everyone.", first.text());

        SubtitleSegment second = response.segments().get(1);
        assertEquals(3_000_000L, second.startUs());
        assertEquals(5_500_000L, second.endUs());
        assertEquals("Welcome to the video.", second.text());
    }
}
