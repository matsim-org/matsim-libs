package playground.sergioo.NetworkVisualizer.gui.networkPainters;

import org.matsim.api.core.v01.network.Network;

import playground.sergioo.NetworkVisualizer.gui.Painter;

public abstract class NetworkPainter implements Painter {
	
	//Attributes
	protected final NetworkByCamera networkByCamera;
	protected final NetworkManager networkManager;
	
	//Methods
	public NetworkPainter(Network network) {
		networkByCamera =  new NetworkByCamera(network);
		networkManager = new NetworkManager(network);
	}
	public NetworkManager getNetworkManager() {
		return networkManager;
	}
	
}
