package playground.sergioo.capacitiesChanger2012.gui;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainterManager;

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
	protected Id<Link> getIdNearestCarLink(double x, double y) {
		Coord coord = new Coord(x, y);
		Link nearest = null;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Link link: network.getLinks().values()) {
			final Coord coord1 = coord;
			Link r = ((Link) link);
			double distance = CoordUtils.distancePointLinesegment(r.getFromNode().getCoord(), r.getToNode().getCoord(), coord1); 
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
