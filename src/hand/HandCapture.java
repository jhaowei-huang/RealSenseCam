package hand;

import intel.rssdk.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;

public class HandCapture {
	static int width = 640;
	static int height = 480;
	static boolean exit = false;
	static pxcmStatus status;
	static int numOfHands = 0;
	static int numOfLeftHands = 0;
	static int numOfRightHands = 0;
	static int numOfUnknow = 0;
	static int numOfGesture = 0;

	public static void main(String[] args) {
		PXCMSession session = PXCMSession.CreateInstance();
		PXCMSenseManager senseManager = PXCMSenseManager.CreateInstance();
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

		MyHandler handler = new MyHandler();
		PXCMHandModule handModule = senseManager.QueryHand();
		PXCMHandConfiguration handConfig = handModule.CreateActiveConfiguration();
		handConfig.EnableAllAlerts();
		handConfig.EnableAllGestures();
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

			PXCMCapture.Sample handSample = senseManager.QueryHandSample();
			handData.Update();

			if (true) {
				numOfHands = handData.QueryNumberOfHands();
				int left = 0, right = 0, unknow = 0;

				for (int i = 0; i < numOfHands; i++) {
					PXCMHandData.IHand hand = new PXCMHandData.IHand();
					PXCMHandData.JointData[] jointData = new PXCMHandData.JointData[5];
					for (int j = 0; j < 5; j++) {
						jointData[j] = new PXCMHandData.JointData();
					}

					handData.QueryHandData(PXCMHandData.AccessOrderType.ACCESS_ORDER_BY_TIME, i, hand);
					hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_THUMB_TIP, jointData[0]);
					hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_INDEX_TIP, jointData[1]);
					hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_MIDDLE_TIP, jointData[2]);
					hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_RING_TIP, jointData[3]);
					hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_PINKY_TIP, jointData[4]);

					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_LEFT) {
						for (int k = 0; k < 5; k++)
							drawFrame.fingers[k].setLocation(jointData[k].positionImage.x,
									jointData[k].positionImage.y);
						handler.leftHand = true;
						left += 1;
					}
					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_RIGHT) {
						for (int k = 0; k < 5; k++)
							drawFrame.fingers[k + 5].setLocation(jointData[k].positionImage.x,
									jointData[k].positionImage.y);
						handler.rightHand = true;
						right += 1;
					}
					if (hand.QueryBodySide() == PXCMHandData.BodySideType.BODY_SIDE_UNKNOWN)
						unknow += 1;

				}

				if (numOfLeftHands != left || numOfRightHands != right || numOfUnknow != unknow) {
					numOfLeftHands = left;
					numOfRightHands = right;
					numOfUnknow = unknow;
					System.out.println("左手: " + numOfLeftHands + " 右手: " + numOfRightHands + " 未知: " + numOfUnknow);
				}

				drawFrame.repaint();
			}

			// find fingers
			/*
			 * if (status.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) == 0) {
			 * PXCMHandData.JointData jointData = new PXCMHandData.JointData();
			 * hand.QueryTrackedJoint(PXCMHandData.JointType.JOINT_INDEX_TIP,
			 * jointData); // extremityData.x // PXCMRectI32 rect =
			 * hand.QueryBoundingBoxImage();
			 * 
			 * // drawFrame.image.setRGB(0, 0, width, height, buff, height, //
			 * height); drawFrame.x = (int) jointData.positionImage.x;
			 * drawFrame.y = (int) jointData.positionImage.y;
			 * drawFrame.repaint(); }
			 */
			// draw colorful stream

			senseManager.ReleaseFrame();
		}

		System.exit(0);
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
	public Point fingers[] = new Point[10];

	public DrawFrame(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < 10; i++) {
			fingers[i] = new Point();
		}
	}

	public void paint(Graphics g) {
		// ((Graphics2D) g).drawImage(image, 0, 0, null);
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(image, 0, 0, null);
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);

		for (int i = 0; i < 10; i++) {
			if (i >= 5)
				g2.setColor(Color.RED);
			else
				g2.setColor(Color.BLUE);

			g2.fillOval(fingers[i].x, fingers[i].y, 20, 20);
		}
		// System.out.println(" Image Position: (" + x + "," +y + ")");
	}
}

class MyHandler implements PXCMHandConfiguration.GestureHandler {
	public boolean leftHand = false;
	public boolean rightHand = false;

	public void OnFiredGesture(PXCMHandData.GestureData data) {
		if (data.state == PXCMHandData.GestureStateType.GESTURE_STATE_START) {
			if (leftHand) {
				System.out.print("left: ");
				leftHand = false;
			}
			if (rightHand) {
				System.out.print("right: ");
				rightHand = false;
			}

			if (data.name.compareTo("spreadfingers") == 0) {
				System.out.println("spreadfingers");
			} else if (data.name.compareTo("v_sign") == 0) {
				System.out.println("v_sign");
			} else if (data.name.compareTo("fist") == 0) {
				System.out.println("fist");
			} else if (data.name.compareTo("click") == 0) {
				System.out.println("click");
			} else if (data.name.compareTo("full_pinch") == 0) {
				System.out.println("full_pinch");
			} else if (data.name.compareTo("swipe_down") == 0) {
				System.out.println("swipe_down");
			} else if (data.name.compareTo("swipe_left") == 0) {
				System.out.println("swipe_left");
			} else if (data.name.compareTo("swipe_right") == 0) {
				System.out.println("swipe_right");
			} else if (data.name.compareTo("swipe_up") == 0) {
				System.out.println("swipe_up");
			} else if (data.name.compareTo("tap") == 0) {
				System.out.println("tap");
			} else if (data.name.compareTo("thumb_down") == 0) {
				System.out.println("thumb_down");
			} else if (data.name.compareTo("thumb_up") == 0) {
				System.out.println("thumb_up");
			} else if (data.name.compareTo("two_fingers_pinch_open") == 0) {
				System.out.println("two_fingers_pinch_open");
			} else if (data.name.compareTo("wave") == 0) {
				System.out.println("wave");
			}
		}
	}
};