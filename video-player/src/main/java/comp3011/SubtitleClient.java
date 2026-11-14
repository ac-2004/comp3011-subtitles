package comp3011;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SubtitleClient {
	
	private static final URI SERVER_URL = 
			URI.create("http://localhost:8080/api/v1/subtitles");
	
	private final HttpClient httpClient = HttpClient.newHttpClient();
	
	public CompletableFuture<List<SubtitleSegment>> requestSubtitles(File audioFile) throws IOException {
		// separates parts of multipart HTTP request
		String boundary = "SubtitleBoundary" + UUID.randomUUID();
		
		// content-disposition: names the uploaded file, matching Spring Boot's @RequestPart("file")
		String header = 
				"--" + boundary + "\r\n" +
		"Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n" +
		"Content-Type: audio/wav\r\n\r\n";
		
		String footer = "\r\n--" + boundary + "--\r\n";
		
		
		HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
	            HttpRequest.BodyPublishers.ofByteArray(
	                    header.getBytes(StandardCharsets.UTF_8)),
	            HttpRequest.BodyPublishers.ofFile(audioFile.toPath()),
	            HttpRequest.BodyPublishers.ofByteArray(
	                    footer.getBytes(StandardCharsets.UTF_8))
	    );
		
		HttpRequest request = HttpRequest.newBuilder()
	            .uri(SERVER_URL)
	            .header("Content-Type",
	                    "multipart/form-data; boundary=" + boundary)
	            .POST(body)
	            .build();

	    return httpClient.sendAsync(
	            request,
	            HttpResponse.BodyHandlers.ofString()
	    ).thenApply(response -> {

	        if (response.statusCode() != 200) {
	            throw new IllegalStateException(
	                    "Subtitle server returned HTTP " +
	                    response.statusCode()
	            );
	        }

	        return parseSubtitles(response.body());
	    });
	}
	
	private List<SubtitleSegment> parseSubtitles(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
	        JsonNode root = mapper.readTree(json); // converts server's JSON string into a tree
	        JsonNode segments = root.path("segments"); // finds subtitle array

	        if (!segments.isArray()) {
	            throw new IllegalArgumentException(
	                    "Subtitle response is missing segments"
	            );
	        }
	        
	        List<SubtitleSegment> result = new ArrayList<>();
	        
	        for (JsonNode segment : segments) {
	        	// reads timestamps already converted into microseconds
	            long startUs = segment.path("startUs").asLong();
	            long endUs = segment.path("endUs").asLong();
	            String text = segment.path("text").asText();

	            // rejects invalid subtitle intervals and empty captions
	            if (startUs >= 0 && endUs > startUs
	                    && !text.isBlank()) {

	                result.add(new SubtitleSegment(
	                        startUs,
	                        endUs,
	                        text.trim()
	                ));
	            }
	        }
	        
	        // returns an immutable list
	        return List.copyOf(result);
	        
		} catch (IOException error) {
	        throw new IllegalArgumentException(
	                "Could not parse subtitle response", error
	        );
	    }
	}
}
