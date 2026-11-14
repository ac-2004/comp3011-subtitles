package comp3011.assignment1.controller;

//accepts uploaded browser audio and passes it to the transcription service.
//the transcription result is returned asynchronously as a mono.

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
// imports
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.model.SubtitleResponse;
import comp3011.assignment1.service.TranscriptionService;
import reactor.core.publisher.Mono;

@RestController
public class TranscriptionController {
	// field
	private final TranscriptionService transcriptionService;

	// constructor
	public TranscriptionController(TranscriptionService transcriptionService) {
		this.transcriptionService = transcriptionService;
	}

	// handles audio uploaded (endpoint method)
	@PostMapping("/api/transcriptions")
	public Mono<String> receiveAudio(@RequestParam("audio") MultipartFile audio) {
		try {
			// copy the uploaded file into a small model before passing it to the service
			AudioData audioData = new AudioData(audio.getBytes(), audio.getOriginalFilename(), audio.getContentType());

			Mono<String> result = transcriptionService.transcribe(audioData);

			return result;

		} catch (IOException error) {
			// handle failure
			return Mono.just("error reading audio");
		}
	}
	
	// handles uploaded audio file, sends it to Whisper and return timestamped transcription HSON
	@PostMapping(
			value = "/api/v1/subtitles",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
			)
	public Mono<ResponseEntity<SubtitleResponse>> generateSubtitles(
			@RequestPart("file") MultipartFile file) {
		System.out.println("SUBTITLE ENDPOINT REACHED");
		System.out.println("Uploaded file: " + file.getOriginalFilename());
		System.out.println("File size: " + file.getSize() + " bytes");
		
		if (file.isEmpty()) {
			return Mono.just(
					ResponseEntity.<SubtitleResponse>badRequest().build());
		}
		
		try {
			AudioData audio = new AudioData(
					file.getBytes(),
					file.getOriginalFilename(),
					file.getContentType()
					);
			
			return transcriptionService.transcribeSubtitles(audio)
					.map(response -> ResponseEntity.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.body(response));
		} catch (IOException error) {
			// handle failure
			return Mono.just(ResponseEntity.<SubtitleResponse>internalServerError().build());
		}
	}
}
