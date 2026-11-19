
package comp3011.assignment1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class AudioUsageServiceTest {

    // verify the usage counter starts at zero
    @Test
    void testInitialUsage() {
        AudioUsageService service = new AudioUsageService();

        assertEquals(0.0, service.getUsageSeconds());
    }

    // verify successful transcription durations accumulate
    @Test
    void testAccumulatedUsage() {
        AudioUsageService service = new AudioUsageService();

        service.addUsage(5.5);
        service.addUsage(3.25);

        assertEquals(8.75, service.getUsageSeconds(), 0.000001);
    }

    // verify invalid durations do not change the usage total
    @Test
    void testInvalidUsage() {
        AudioUsageService service = new AudioUsageService();

        service.addUsage(-5.0);
        service.addUsage(Double.NaN);
        service.addUsage(Double.POSITIVE_INFINITY);

        assertEquals(0.0, service.getUsageSeconds());
    }
}
