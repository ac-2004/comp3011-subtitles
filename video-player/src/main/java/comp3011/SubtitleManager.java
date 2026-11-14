package comp3011;

import java.util.List;





// store collection of SubtitleSegment objects
// find segment corresponding to given timestamp
// accept transcription results from server

public class SubtitleManager {
	// create empty list
	private volatile List<SubtitleSegment> segments = List.of();
	
	
	// allow subtitles to be loaded
	public void setSegments(List<SubtitleSegment> newSegments) {
		segments = List.copyOf(newSegments);
	}
	
	// get subtitle for a timestamp
	// loop through segments, check whether timestamp falls within segments start/end times
	// if it does, return segment's text
	public String getSubtitleAt(long timestampUs) {
		
		// loop through segments
		for (SubtitleSegment segment : segments) {
			if (timestampUs >= segment.startUs()
					&& timestampUs < segment.endUs()) {
				return segment.text();
			}
		}
		
		return "";
	}
}
