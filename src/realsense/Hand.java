package realsense;

public class Hand {
	private String bodySide = "unknow";
	private String gestureName = "none";
	private int handId = -1;

	public Hand(String bodySide) {
		this.bodySide = bodySide;
	}

	public String getBodySide() {
		return bodySide;
	}

	public void setBodySide(String bodySide) {
		this.bodySide = bodySide;
	}

	public String getGestureName() {
		return gestureName;
	}

	public void setGestureName(String gestureName) {
		this.gestureName = gestureName;
	}

	public int getHandId() {
		return handId;
	}

	public void setHandId(int handId) {
		this.handId = handId;
	}
}
