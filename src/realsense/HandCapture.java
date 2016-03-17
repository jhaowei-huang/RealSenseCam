package realsense;

import intel.rssdk.*;
import realsense.HandCapture.GestureHandler;

import javax.swing.*;

import java.awt.event.*;
import java.awt.image.*;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.*;

public class HandCapture {
	public pxcmStatus status;

	public String[] gestures = { "v_sign", "fist", "spreadfingers", "thumb_down", "thumb_up" };
	public int numOfHands = 0;
	public int numOfLeftHands = 0;
	public int numOfRightHands = 0;
	public int numOfUnknow = 0;
	public int numOfGesture = 0;

	private int width = 640;
	private int height = 480;
	private boolean exit = false;

	private GestureHandler gestureHandler;
	private PXCMSenseManager senseManager;
	private PXCMHandData handData;
	private PXCMHandModule handModule;
	private DrawFrame drawFrame;
	private JFrame cframe;
	private Listener listener;

	public HandCapture() {
		
		PXCMSession session = PXCMSession.CreateInstance();
		senseManager = PXCMSenseManager.CreateInstance();
		senseManager.EnableStream(PXCMCapture.StreamType.STREAM_TYPE_COLOR, width, height);
		status = senseManager.EnableHand(null);
		if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) != 0) {
			System.out.println("Failed to enable Hand Analysis");
			System.exit(1);
		} else if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) == 0) {
			System.out.println("RealSense camera hand analysis: ok");
		}

		status = senseManager.Init();
		if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) != 0) {
			System.out.println("RealSense camera initilize failed.");
			System.exit(1);
		} else {
			System.out.println("RealSense camera initlize: ok");
		}
		// listen for detect gesture event
		gestureHandler = new GestureHandler();
		handModule = senseManager.QueryHand();
		PXCMHandConfiguration handConfig = handModule.CreateActiveConfiguration();
		handConfig.EnableAllAlerts();
		// handConfig.EnableAllGestures();
		// enable gesture
		for (String str : gestures) {
			handConfig.EnableGesture(str);
		}
		// enable functions
		handConfig.EnableTrackedJoints(true);
		handConfig.EnableNormalizedJoints(true);
		handConfig.SubscribeGesture(gestureHandler);
		handConfig.ApplyChanges();
		handConfig.Update();
		// data structure
		handData = handModule.CreateOutput();
		// java awt
		drawFrame = new DrawFrame(width, height);
		listener = new Listener();
		cframe = new JFrame("Intel(R) RealSense(TM)");
		cframe.addWindowListener(listener);
		cframe.setSize(width, height);
		cframe.add(drawFrame);
		cframe.setVisible(true);
	}

	public void loop() {
		// if window closed then break
		while (listener.exit == false) {
			status = senseManager.AcquireFrame(true);
			if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
				System.out.println(status.name());
				break;
			}
			// get camera stream
			PXCMCapture.Sample sample = senseManager.QuerySample();
			// storage data structure
			PXCMImage.ImageData colorData = new PXCMImage.ImageData();
			/*if (sample.color != null) {
				status = sample.color.AcquireAccess(PXCMImage.Access.ACCESS_READ,
						PXCMImage.PixelFormat.PIXEL_FORMAT_RGB32, colorData);
				if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
					System.out.println("Failed to AcquireAccess of color image data");
					System.exit(3);
				}

				int colorBuffer[] = new int[colorData.pitches[0] / 4 * height];

				colorData.ToIntArray(0, colorBuffer);
				drawFrame.image.setRGB(0, 0, width, height, colorBuffer, 0, colorData.pitches[0] / 4);
				drawFrame.repaint();
				status = sample.color.ReleaseAccess(colorData);

				if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
					System.out.println("Failed to ReleaseAccess of color image data");
					System.exit(3);
				}
			}*/

			handData.Update();
			numOfHands = handData.QueryNumberOfHands();
			int left = 0, right = 0, unknow = 0;

			if (numOfHands > 0) {
				for (int i = 0; i < numOfHands; i++) {
					PXCMHandData.IHand hand = new PXCMHandData.IHand();

					handData.QueryHandData(PXCMHandData.AccessOrderType.ACCESS_ORDER_BY_TIME, i, hand);
					int currentHandId = hand.QueryUniqueId();

					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_LEFT) {
						gestureHandler.getLeftHand().setHandId(hand.QueryUniqueId());
						left += 1;
					}
					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_RIGHT) {
						gestureHandler.getRightHand().setHandId(hand.QueryUniqueId());
						right += 1;
					}
					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_UNKNOWN) {
						unknow += 1;
					}
				}

				drawFrame.repaint();
			}
			if (left == 0) {
				gestureHandler.getLeftHand().setGestureName("none");
			}
			if (right == 0) {
				gestureHandler.getRightHand().setGestureName("none");
			}

			numOfLeftHands = left;
			numOfRightHands = right;
			numOfUnknow = unknow;

			senseManager.ReleaseFrame();
		}

		System.exit(0);
	}

	class GestureHandler implements PXCMHandConfiguration.GestureHandler {
		private Hand leftHand = new Hand("left");
		private Hand rightHand = new Hand("right");

		public void OnFiredGesture(PXCMHandData.GestureData data) {
			if (data.state == PXCMHandData.GestureStateType.GESTURE_STATE_START) {
				for (String str : gestures) {
					if (data.name.equals(str)) {
						System.out.println("----------------------------------");
						if (data.handId == leftHand.getHandId() && numOfLeftHands != 0) {
							leftHand.setGestureName(str);
							System.out.println(" left: " + leftHand.getGestureName());
						}
						if (data.handId == rightHand.getHandId() && numOfRightHands != 0) {
							rightHand.setGestureName(str);
							System.out.println(" right: " + rightHand.getGestureName());
						}
					}
				}
			}
		}

		public Hand getLeftHand() {
			return leftHand;
		}

		public Hand getRightHand() {
			return rightHand;
		}
	};
}

class Listener extends WindowAdapter {
	public boolean exit = false;

	@Override
	public void windowClosing(WindowEvent e) {
		exit = true;
	}
}

class DrawFrame extends Component {
	public BufferedImage image;

	public DrawFrame(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}

	public void paint(Graphics g) {
		// ((Graphics2D) g).drawImage(image, 0, 0, null);
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(image, 0, 0, null);
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);
	}
}
