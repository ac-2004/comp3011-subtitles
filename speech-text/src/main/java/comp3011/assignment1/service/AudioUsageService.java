package comp3011.assignment1.service;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class AudioUsageService {
	
	// track successful transcription duration in Us
	private final AtomicLong totalMicroseconds = new AtomicLong();
	
	public void addUsage(double seconds) {
        // ignore invalid durations
        if (!Double.isFinite(seconds) || seconds <= 0) {
            return;
        }

        // store integer microseconds to avoid concurrent update losses
        long microseconds = Math.round(seconds * 1_000_000);
        totalMicroseconds.addAndGet(microseconds);
    }
	
	public double getUsageSeconds() {
        // return the accumulated duration in seconds
        return totalMicroseconds.get() / 1_000_000.0;
    }
}
