package hand;

import intel.rssdk.*;
import javax.swing.*;

import java.awt.event.*;
import java.awt.image.*;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.*;

public class HandCapture {
	public static pxcmStatus status;
	public static String[] gestures = { "v_sign", "fist", "spreadfingers", "thumb_down", "thumb_up" };
	public static int numOfHands = 0;
	public static int numOfLeftHands = 0;
	public static int numOfRightHands = 0;
	public static int numOfUnknow = 0;
	public static int numOfGesture = 0;
	public static DetectGestureTask detectGestureTask;

	private static int width = 640;
	private static int height = 480;
	private static boolean exit = false;
	private static Timer timer = new Timer();

	public static void main(String[] args) {
		// declare coap server
		Server server = null;
		try {
			// create coap server
			server = new Server();
			server.start();
			System.out.println("coap server start!");
		} catch (SocketException e) {
			System.err.println("Failed to initialize coap server: " + e.getMessage());
		}

		detectGestureTask = new DetectGestureTask();
		timer.schedule(detectGestureTask, 100, 1000);

		PXCMSession session = PXCMSession.CreateInstance();
		PXCMSenseManager senseManager = PXCMSenseManager.CreateInstance();
		// senseManager.EnableStream(PXCMCapture.StreamType.STREAM_TYPE_COLOR, width, height);
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

		GestureHandler handler = new GestureHandler();
		PXCMHandModule handModule = senseManager.QueryHand();
		PXCMHandConfiguration handConfig = handModule.CreateActiveConfiguration();
		handConfig.EnableAllAlerts();
		// handConfig.EnableAllGestures();

		for (String str : gestures) {
			handConfig.EnableGesture(str);
		}

		handConfig.EnableTrackedJoints(true);
		handConfig.EnableNormalizedJoints(true);
		handConfig.SubscribeGesture(handler);
		handConfig.ApplyChanges();
		handConfig.Update();

		PXCMHandData handData = handModule.CreateOutput();

		DrawFrame drawFrame = new DrawFrame(width, height);
		JFrame cframe = new JFrame("Intel(R) RealSense(TM)");
		Listener listener = new Listener();
		cframe.addWindowListener(listener);
		cframe.setSize(width, height);
		cframe.add(drawFrame);
		cframe.setVisible(true);

		while (listener.exit == false) {
			status = senseManager.AcquireFrame(true);
			if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
				System.out.println(status.name());
				break;
			}

			PXCMCapture.Sample sample = senseManager.QuerySample();
			PXCMImage.ImageData colorData = new PXCMImage.ImageData();
			if (sample.color != null) {
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
			}

			// PXCMCapture.Sample handSample = senseManager.QueryHandSample();
			handData.Update();
			numOfHands = handData.QueryNumberOfHands();
			int left = 0, right = 0, unknow = 0;
			if (numOfHands > 0) {
				for (int i = 0; i < numOfHands; i++) {
					PXCMHandData.IHand hand = new PXCMHandData.IHand();
					// PXCMHandData.JointData[] jointData = new
					// PXCMHandData.JointData[5];
					// for (int j = 0; j < 5; j++) {
					// jointData[j] = new PXCMHandData.JointData();
					// }

					handData.QueryHandData(PXCMHandData.AccessOrderType.ACCESS_ORDER_BY_TIME, i, hand);
					int currentHandId = GestureHandler.currentHandId = hand.QueryUniqueId();
					// hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_THUMB_TIP,
					// jointData[0]);
					// hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_INDEX_TIP,
					// jointData[1]);
					// hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_MIDDLE_TIP,
					// jointData[2]);
					// hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_RING_TIP,
					// jointData[3]);
					// hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_PINKY_TIP,
					// jointData[4]);

					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_LEFT) {
						// for (int k = 0; k < 5; k++)
						// drawFrame.fingers[k].setLocation(jointData[k].positionImage.x,
						// jointData[k].positionImage.y);
						GestureHandler.leftHand.handId = hand.QueryUniqueId();
						left += 1;
					}
					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_RIGHT) {
						// for (int k = 0; k < 5; k++)
						// drawFrame.fingers[k +
						// 5].setLocation(jointData[k].positionImage.x,
						// jointData[k].positionImage.y);
						GestureHandler.rightHand.handId = hand.QueryUniqueId();
						right += 1;
					}
					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_UNKNOWN) {
						unknow += 1;
					}
				}

				// if (left == 0) {
				// GestureHandler.leftHand.handId = -1;
				// GestureHandler.leftHand.gestureName = "none";
				// }
				// if (right == 0) {
				// GestureHandler.rightHand.handId = -1;
				// GestureHandler.rightHand.gestureName = "none";
				// }
				drawFrame.repaint();
			}
			
			numOfLeftHands = left;
			numOfRightHands = right;
			numOfUnknow = unknow;
			// find fingers

			// if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) == 0) {
			// PXCMHandData.JointData jointData = new PXCMHandData.JointData();
			// hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_INDEX_TIP,
			// jointData);
			// hand.QueryBoundingBoxImage();

			// drawFrame.image.setRGB(0, 0, width, height, buff, height,
			// height); drawFrame.x = (int) jointData.positionImage.x;
			// drawFrame.y = (int) jointData.positionImage.y;
			// drawFrame.repaint(); }

			senseManager.ReleaseFrame();
		}

		System.exit(0);
	}

	static class GestureHandler implements PXCMHandConfiguration.GestureHandler {
		public static class Hands {
			public String bodySide = "unknow";
			public String gestureName = "none";
			public int handId = -1;

			public Hands(String bodySide) {
				this.bodySide = bodySide;
			}
		}

		public static Hands leftHand = new Hands("left");
		public static Hands rightHand = new Hands("right");
		public static int currentHandId;

		public void OnFiredGesture(PXCMHandData.GestureData data) {
			if (data.state == PXCMHandData.GestureStateType.GESTURE_STATE_START) {
				for (String str : HandCapture.gestures) {
					if (data.name.compareTo(str) == 0) {
						if (currentHandId == leftHand.handId && numOfLeftHands != 0)
							leftHand.gestureName = str;
						if (currentHandId == rightHand.handId && numOfRightHands != 0)
							rightHand.gestureName = str;
					}
				}
			}
		}
	};
}

class DetectGestureTask extends TimerTask {

	@Override
	public void run() {
//		System.out.println(
//				HandCapture.numOfHands + " = " + HandCapture.numOfLeftHands + " + " + HandCapture.numOfRightHands);
	}

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
	// public Point fingers[] = new Point[10];

	public DrawFrame(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		// for (int i = 0; i < 10; i++) {
		// fingers[i] = new Point();
		// }
	}

	public void paint(Graphics g) {
		// ((Graphics2D) g).drawImage(image, 0, 0, null);
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(image, 0, 0, null);
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);

		// for (int i = 0; i < 10; i++) {
		// if (i >= 5)
		// g2.setColor(Color.RED);
		// else
		// g2.setColor(Color.BLUE);
		//
		// g2.fillOval(fingers[i].x, fingers[i].y, 20, 20);
		// }
		// System.out.println(" Image Position: (" + x + "," +y + ")");
	}
}
