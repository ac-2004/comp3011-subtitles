package comp3011;

// holds text, start time and end time

public record SubtitleSegment(
		long startUs,
		long endUs,
		String text
		) {
	
	public SubtitleSegment {
		
	}
}
