package comp3011.assignment1.subtitle;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import comp3011.assignment1.model.SubtitleResponse;
import comp3011.assignment1.model.SubtitleSegment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SubtitleResponseParser {
	private final JsonMapper mapper = new JsonMapper();
	
	public SubtitleResponse parse(String json) {
		// converts JSON text into tree of values
		JsonNode root = mapper.readTree(json);
		
		if (root == null || !root.isObject()) {
			throw new IllegalArgumentException("invalid whisper response");
		}
		
		// get duration and segments
		double duration = root.path("duration").asDouble();
        JsonNode segments = root.path("segments");
        
        if (!segments.isArray()) {
            throw new IllegalArgumentException("Missing subtitle segments");
        }
        
        List<SubtitleSegment> result = new ArrayList<>();
        
        for (JsonNode segment : segments) {
        	double start = segment.path("start").asDouble();
        	double end = segment.path("end").asDouble();
        	String text = segment.path("text").asText().trim();
        	
        	double noSpeechProb = 
        			segment.path("no_speech_prob").asDouble();
        	
        	double avgLogProb = 
        			segment.path("avg_logprob").asDouble();
        	
        	// ignore segments identified as unreliable silence
        	if (noSpeechProb > 0.6 && avgLogProb < -1.0) {
                continue;
            }
        	
        	if (text.isEmpty() || !Double.isFinite(start)
                    || !Double.isFinite(end)
                    || start < 0 || end <= start) {
                continue;
            }
        	
        	long startUs = Math.round(start * 1_000_000);
            long endUs = Math.round(end * 1_000_000);

            result.add(new SubtitleSegment(startUs, endUs, text));
        }
        
        return new SubtitleResponse(duration, List.copyOf(result));
	}

}
