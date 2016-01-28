package hand;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class HandResource extends CoapResource {
	public HandResource(String name) {
		super(name);
		this.setObservable(true);
	}

	public void handGestureChanged() {
		System.out.println("observe");
		this.changed();
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		// respond to the request
		exchange.respond("changed ");
//		if(HandCapture.GestureHandler.Hands.isLeft)
//			exchange.respond(HandCapture.GestureHandler.Hands.leftHand);
//		else if(HandCapture.GestureHandler.Hands.isRight)
//			exchange.respond(HandCapture.GestureHandler.Hands.rightHand);
	}
}
