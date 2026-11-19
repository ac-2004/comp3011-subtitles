
package comp3011.assignment1;

import java.util.ArrayList;
import java.util.List;

import comp3011.assignment1.model.SubtitleSegment;

public class SubtitleFormatter {

	// max characters as specified by assessment outline
    private static final int MAX_LINE_LENGTH = 42;
    // minimum preferred caption duration in microseconds
    private static final long MIN_DURATION_US = 1_000_000L;
    // maximum preferred caption duration in microseconds
    private static final long MAX_DURATION_US = 7_000_000L;
    

    // line warping for display
    public List<String> wrapLines(String text) {
        List<String> lines = new ArrayList<>();

        // ignore empty or missing subtitle text
        if (text == null || text.isBlank()) {
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();

        // split text into words, without breaking individual words
        for (String word : text.trim().split("\\s+")) {

            // if a single word is longer than 42 characters
        	if (word.length() > MAX_LINE_LENGTH) {
        	    // finish the current line before splitting the long word
        	    if (!currentLine.isEmpty()) {
        	        lines.add(currentLine.toString());
        	        currentLine.setLength(0);
        	    }

        	    int start = 0;
        	    while (word.length() - start > MAX_LINE_LENGTH) {
        	        int end = start + MAX_LINE_LENGTH;

        	        // keep both halves of a unicode surrogate pair together
        	        if (Character.isHighSurrogate(word.charAt(end - 1))
        	                && Character.isLowSurrogate(word.charAt(end))) {
        	            end--;
        	        }

        	        lines.add(word.substring(start, end));
        	        start = end;
        	    }

        	    // let the remaining piece share a line with the next word if it fits
        	    currentLine.append(word, start, word.length());
        	    continue;
        	}

            if (currentLine.isEmpty()) {
            	// start new line with first word
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length()
                    <= MAX_LINE_LENGTH) {
            	// append word ir line remains within 42 characters
                currentLine.append(" ").append(word);
            } else {
            	// save full line and begin another
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        // save final line after processing all words
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
    

	public List<SubtitleSegment> format(SubtitleSegment segment) {
	
	    // reject invalid timestamps before processing the caption
	    if (segment.startUs() < 0 ||
	            segment.endUs() <= segment.startUs()) {
	        throw new IllegalArgumentException("Invalid subtitle timestamps");
	    }
	
	    // wrap the original text into lines of at most 42 characters
	    List<String> lines = wrapLines(segment.text());
	
	    if (lines.isEmpty()) {
	        return List.of();
	    }
	
	    List<String> captions = new ArrayList<>();
	
	    // group wrapped lines into captions containing at most two lines
	    for (int i = 0; i < lines.size(); i += 2) {
	        String caption = lines.get(i);
	
	        if (i + 1 < lines.size()) {
	            caption += "\n" + lines.get(i + 1);
	        }
	
	        captions.add(caption);
	    }
	
	    long durationUs = segment.endUs() - segment.startUs();
	
	    // split long captions until the seven-second limit is achievable
	    while (durationUs > captions.size() * MAX_DURATION_US
	            && durationUs >= (captions.size() + 1L) * MIN_DURATION_US) {
	
	        if (!splitLongestCaption(captions)) {
	            break;
	        }
	    }
	
	    int count = captions.size();
	
	    // check whether every caption can fit within the duration limits
	    boolean feasible = durationUs >= count * MIN_DURATION_US
	            && durationUs <= count * MAX_DURATION_US;
	
	    List<SubtitleSegment> result = new ArrayList<>();
	
	    // use character counts to estimate the relative speaking time
	    long remainingWeight = captions.stream()
	            .mapToLong(caption -> caption.replace("\n", " ").length())
	            .sum();
	
	    long remainingDuration = durationUs;
	    long startUs = segment.startUs();
	
	    for (int i = 0; i < count; i++) {
	        String caption = captions.get(i);
	        long weight = caption.replace("\n", " ").length();
	        long allocatedUs;
	
	        if (i == count - 1) {
	            // give the final caption all remaining time
	            allocatedUs = remainingDuration;
	
	        } else {
	            // estimate duration using the caption's relative text length
	            long estimatedUs = Math.round(
	                    (double) remainingDuration * weight / remainingWeight
	            );
	
	            int remainingCaptions = count - i - 1;
	
	            if (feasible) {
	                // reserve enough time for the remaining captions
	                long minimum = Math.max(
	                        MIN_DURATION_US,
	                        remainingDuration - remainingCaptions * MAX_DURATION_US
	                );
	
	                long maximum = Math.min(
	                        MAX_DURATION_US,
	                        remainingDuration - remainingCaptions * MIN_DURATION_US
	                );
	
	                // keep the estimated duration within feasible limits
	                allocatedUs = Math.max(minimum,
	                        Math.min(estimatedUs, maximum));
	
	            } else {
	                // preserve positive intervals when the limits are impossible
	                allocatedUs = Math.max(1,
	                        Math.min(estimatedUs,
	                                remainingDuration - remainingCaptions));
	            }
	        }
	
	        long endUs = startUs + allocatedUs;
	
	        // create the formatted caption with its assigned timestamps
	        result.add(new SubtitleSegment(startUs, endUs, caption));
	
	        // update the remaining time and text for the next caption
	        startUs = endUs;
	        remainingDuration -= allocatedUs;
	        remainingWeight -= weight;
	    }
	
	    return List.copyOf(result);
	}
	
	// split the longest caption at a word boundary
	private boolean splitLongestCaption(List<String> captions) {
	    int longestIndex = -1;
	    int mostWords = 1;
	
	    // find a caption containing enough words to split
	    for (int i = 0; i < captions.size(); i++) {
	        String[] words = captions.get(i).split("\\s+");
	
	        if (words.length > mostWords) {
	            mostWords = words.length;
	            longestIndex = i;
	        }
	    }
	
	    if (longestIndex == -1) {
	        return false;
	    }
	
	    String[] words = captions.get(longestIndex).split("\\s+");
	    int midpoint = words.length / 2;
	
	    // preserve the original word order in both halves
	    String first = String.join(" ",
	            java.util.Arrays.copyOfRange(words, 0, midpoint));
	
	    String second = String.join(" ",
	            java.util.Arrays.copyOfRange(words, midpoint, words.length));
	
	    // reapply the line-length limit to both new captions
	    String firstCaption = String.join("\n", wrapLines(first));
	    String secondCaption = String.join("\n", wrapLines(second));
	
	    captions.set(longestIndex, firstCaption);
	    captions.add(longestIndex + 1, secondCaption);
	
	    return true;
	}

}
