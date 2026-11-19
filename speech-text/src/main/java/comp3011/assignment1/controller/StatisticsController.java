package comp3011.assignment1.controller;

//provides the end-point used to return global transcription token statistics.

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.model.GlobalStatsResponse;
import comp3011.assignment1.service.GlobalStatisticsService;
import comp3011.assignment1.service.AudioUsageService;

import java.util.Map;

@RestController
public class StatisticsController {
	private final GlobalStatisticsService globalStatisticsService;
	private final AudioUsageService audioUsageService;
	
	public StatisticsController(GlobalStatisticsService globalStatisticsService,
			 AudioUsageService audioUsageService) {
		this.globalStatisticsService = globalStatisticsService;
		this.audioUsageService = audioUsageService;
	}

	@GetMapping("/api/v1/global/stats")
	public GlobalStatsResponse getGlobalStats() {
		return globalStatisticsService.getStats();
	}
	
	// report cumulative transcription seconds as a json object
	@GetMapping("/api/v1/global/usage")
	public Map<String, Double> getAudioUsage() {
	    return Map.of("usage", audioUsageService.getUsageSeconds());
	}
}
