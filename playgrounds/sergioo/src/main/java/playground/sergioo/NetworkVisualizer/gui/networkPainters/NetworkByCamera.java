package playground.sergioo.NetworkVisualizer.gui.networkPainters;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworkVisualizer.gui.Camera;

public class NetworkByCamera {

	//Attributes
	private final Network network;
	protected Camera camera;
	
	//Methods
	public NetworkByCamera(Network network) {
		super();
		this.network = network;
	}
	public Network getNetwork() {
		return network;
	}
	public void setCamera(Camera camera) {
		this.camera = camera;
	}
	public Collection<? extends Link> getNetworkLinks() throws Exception {
		if(camera!=null) {
			double xMin = camera.getUpLeftCorner().getX();
			double yMin = camera.getUpLeftCorner().getY()+camera.getSize().getY();
			double xMax = camera.getUpLeftCorner().getX()+camera.getSize().getX();
			double yMax = camera.getUpLeftCorner().getY();
			Collection<Link> links =  new HashSet<Link>();
			for(Link link:network.getLinks().values()) {
				Coord from = link.getFromNode().getCoord();
				Coord to = link.getToNode().getCoord();
				if((xMin<from.getX()&&yMin<from.getY()&&xMax>from.getX()&&yMax>from.getY())||
						(xMin<to.getX()&&yMin<to.getY()&&xMax>to.getX()&&yMax>to.getY()))
					links.add(link);
			}
			return links;
		}
		else
			throw new Exception("No camera defined");
	}
	public Collection<? extends Node> getNetworkNodes() throws Exception {
		if(camera!=null) {
			double xMin = camera.getUpLeftCorner().getX();
			double yMin = camera.getUpLeftCorner().getY()+camera.getSize().getY();
			double xMax = camera.getUpLeftCorner().getX()+camera.getSize().getX();
			double yMax = camera.getUpLeftCorner().getY();
			Collection<Node> nodes =  new HashSet<Node>();
			for(Node node:network.getNodes().values()) {
				Coord point = node.getCoord();
				if(xMin<point.getX()&&yMin<point.getY()&&xMax>point.getX()&&yMax>point.getY())
					nodes.add(node);
			}
			return nodes;
		}
		else
			throw new Exception("No camera defined");
	}
	public int getIntX(double x) throws Exception {
		if(camera!=null)
			return camera.getIntX(x);
		else
			throw new Exception("No camera defined");
	}
	public int getIntY(double y) throws Exception {
		if(camera!=null)
			return camera.getIntY(y);
		else
			throw new Exception("No camera defined");
	}
	
}
