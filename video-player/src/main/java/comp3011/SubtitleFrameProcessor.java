package comp3011;

import org.bytedeco.javacv.Frame;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacv.Java2DFrameConverter;

// draws subtitle text directly onto video frames

public class SubtitleFrameProcessor extends FrameProcessor {
	private final SubtitleManager subtitleManager;
	private String currentSubtitle = "";
	
	private final Java2DFrameConverter converter = new Java2DFrameConverter();
	

	@Override
	public void process(Frame frame, InfoFrame info) throws Exception {
		// TODO Auto-generated method stub
		long timestampUs = info.timestampUs();
		String currentSubtitle = subtitleManager.getSubtitleAt(timestampUs);
		
	}
	
	public String getCurrentSubtitle() {
	    return currentSubtitle;
	}

	public SubtitleFrameProcessor(SubtitleManager subtitleManager) {
		this.subtitleManager = subtitleManager;
	}
	
}
