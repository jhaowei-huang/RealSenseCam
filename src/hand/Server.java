package hand;

import java.net.SocketException;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.config.NetworkConfig;

public class Server extends CoapServer {
	public Server() throws SocketException {
		// provide an instance of a SkeletonTracking resource
        add(new Root("102"));
	}
}
