package playground.sergioo.CapacitiesChanger.gui;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainterManager;

public class CarNetworkPainterManager extends NetworkPainterManager  {
	
	//Methods
	/**
	 * @param network
	 */
	public CarNetworkPainterManager(Network network) {
		super(network);
	}
	public void selectLink(double x, double y) {
		selectedLinkId = getIdNearestCarLink(x, y);
	}
	protected Id getIdNearestCarLink(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Link nearest = null;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Link link: network.getLinks().values()) {
			double distance = ((LinkImpl) link).calcDistance(coord); 
			if(link.getAllowedModes().contains("car") && distance<nearestDistance) {
				nearest = link;
				nearestDistance = distance;
			}
		}
		return nearest.getId();
	}
	public String refreshLink() {
		Link link = network.getLinks().get(selectedLinkId);
		return selectedLinkId==null?"":selectedLinkId.toString()+" modes:"+link.getAllowedModes()+" ("+link.getNumberOfLanes()+" lanes)";
	}

}
