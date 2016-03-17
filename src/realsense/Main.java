package realsense;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
	
	@Override
	public void start(Stage stage) {
		// ����Thread
		Thread t = new RSThread(); 
		t.start(); // �}�l����t.run()
	}

	public static void main(String[] args) throws Exception {
		launch(args);
	}
}

class RSThread extends Thread {
	public void run() {
		HandCapture hc = new HandCapture();
        hc.loop();
    }
}