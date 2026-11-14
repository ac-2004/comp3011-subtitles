package comp3011.assignment1.model;

import java.util.List;

public record SubtitleResponse(
		double duration,
		List<SubtitleSegment> segments) {

}
