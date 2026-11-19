# COMP3011 Assignment 3: Video Player with Subtitles

This project adds subtitles to the video player from Assignment 2. The player extracts audio from an MP4 and sends it to a local Spring Boot server. The server uses Whisper to turn speech into text with timestamps. The player uses the timestamp of the frame on screen to choose which caption to show.

## Project folders

| Folder | What it does |
| --- | --- |
| `speech-text` | Accepts audio on port 8080. It requests captions from Whisper and caches the results for later plays. |
| `video-player` | Plays the MP4 in JavaFX. It shows captions and supports the video effects from Assignment 2. |

## What you need

1. JDK 25 and an MP4 with spoken audio.
2. A valid OpenAI API key for new transcriptions. Set it as `OPENAI_API_KEY` in the terminal where you start the server. Keep the key out of the source and submission ZIP.
3. Internet access for the first Maven run and for any audio that the server needs to send to Whisper.

Both project folders include a Maven wrapper.

## Run the project

### 1. Start the subtitle server

Open a terminal at the project root and run:

```bash
cd speech-text
./mvnw spring-boot:run
```

Wait for the server to start on port 8080. Leave this terminal open while you use the player.

### 2. Start the video player

Open a second terminal at the project root and run:

```bash
cd video-player
./mvnw javafx:run '-Djavafx.args="/absolute/path/to/video.mp4" -S'
```

Replace the sample path with the full path to your MP4. The `-S` option starts playback with subtitles enabled. Without it, subtitles start off.

## Subtitle controls

Press **S** or click **CC** to turn subtitles on or off. On the first play, captions can take time to appear while the server transcribes the audio. Playback can continue while the player waits for the result.

Captions use white text with a black outline. When you seek forward or back, the player chooses a caption based on the frame it displays. During a gap in speech, the caption area is empty.

To run subtitles with all the Assignment 2 effects, use:

```bash
./mvnw javafx:run '-Djavafx.args="/absolute/path/to/video.mp4" -nsfwyvdjmbpS'
```

The `n` option shows the frame number in the bottom right corner.

## Check the cache

1. Play a clip with subtitles enabled and wait for captions to appear.
2. Check the total number of seconds sent to Whisper:

   ```bash
   curl http://localhost:8080/api/v1/global/usage
   ```

3. Close the player and open the same clip again. Leave the server running so it keeps its cache.
4. Seek forward to a place with dialogue and check that captions appear. Run the usage command again. The total should stay the same if the server reused the cached captions.

You can also try the second play with internet access disconnected. Keep the local server running so the player can still reach it.

## If captions do not load

Check that the server is running on port 8080. For audio the server has not seen before, check that the server terminal has a valid API key and internet access. If a request failed, turn subtitles off and back on to retry once the server is available.

## Run the tests

Run this command from each project folder:

```bash
./mvnw test
```

## Credits

This project builds on the COMP3011 starter code and my work from Assignments 1 and 2. See the README in each project folder for more detail.