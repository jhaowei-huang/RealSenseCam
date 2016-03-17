package realsense;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
	
	@Override
	public void start(Stage stage) {
		// 產生Thread
		Thread t = new RSThread(); 
		t.start(); // 開始執行t.run()
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