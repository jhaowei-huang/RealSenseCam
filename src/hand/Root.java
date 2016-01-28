package hand;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class Root extends CoapResource {
	public Root(String name) {
		super(name);
		// peopleCount
		// add(new SkeletonTrackingResource());

		// postDetection not implement
		// add(new CoapResource("2") {
		// @Override
		// public void handleGET(CoapExchange exchange) {
		// exchange.respond("resourceID: 2");
		// }
		// });

		// voiceCommand not implement
		// add(new CoapResource("3") {
		// @Override
		// public void handleGET(CoapExchange exchange) {
		// exchange.respond("resourceID: 3");
		// }
		// });
		
		// left hand
		add(new HandResource("4"));
		// right hand
		add(new HandResource("5"));

		add(new CoapResource("flags") {
			@Override
			public void handleGET(CoapExchange exchange) {
				exchange.respond("1");
			}
		});

		add(new CoapResource("typeID") {
			@Override
			public void handleGET(CoapExchange exchange) {
				// respond to the request
				// Kinect/VedioCam typeID is 102
				exchange.respond("102");
			}
		});
	}
}
