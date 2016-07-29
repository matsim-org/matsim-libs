package playground.sergioo.gtfs2PTSchedule2011.auxiliar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import others.sergioo.util.geometry.Line2D;
import others.sergioo.util.geometry.Point2D;
import playground.sergioo.gtfs2PTSchedule2011.Stop;

public class LinkStops {
	
	//Constants
	private final static Logger log = Logger.getLogger(LinkStops.class);

	//Attributes
	private Link link;
	private List<Stop> stops;
	
	//Methods
	public LinkStops(Link link) {
		super();
		this.link = link;
		stops = new ArrayList<Stop>();
	}
	/**
	 * @return the link
	 */
	public Link getLink() {
		return link;
	}
	public int getNumStops() {
		return stops.size();
	}
	public Stop getStop(int pos) {
		return stops.get(pos);
	}
	public void addStop(Stop nStop) {
		boolean added = false;
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Line2D linkLine = new Line2D(fromPoint, toPoint);
		Point2D nPoint = new Point2D(nStop.getPoint().getX(),nStop.getPoint().getY());
		double nParameter = linkLine.getParameter(linkLine.getNearestPoint(nPoint));
		for(int i=0; i<stops.size() && !added; i++) {
			Point2D point = new Point2D(stops.get(i).getPoint().getX(),stops.get(i).getPoint().getY());
			if(nParameter<linkLine.getParameter(linkLine.getNearestPoint(point))) {
				stops.add(i, nStop);
				added = true;
			}
		}
		if(!added)
			stops.add(nStop);
	}
	public double getLastDistance() {
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Line2D linkLine = new Line2D(fromPoint, toPoint);
		Point2D point = new Point2D(stops.get(stops.size()-1).getPoint().getX(),stops.get(stops.size()-1).getPoint().getY());
		Point2D lPoint = new Point2D(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY());
		return linkLine.getNearestPoint(point).getDistance(lPoint);
	}
	public Link split(int i, Network network, CoordinateTransformation coordinateTransformation) {
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Line2D linkLine = new Line2D(fromPoint, toPoint);
		Point2D point = new Point2D(stops.get(i).getPoint().getX(),stops.get(i).getPoint().getY());
		Point2D nearestPoint = linkLine.getNearestPoint(point);
		if(linkLine.getParameter(nearestPoint)<0) {
			log.warn("Bad position of stop "+stops.get(i).getName()+" according to the link " + link.getId());
			return null;
		}
		Node toNode = network.getFactory().createNode(Id.createNodeId(link.getId().toString()+"_"+link.getToNode().getId().toString()+"_"+i), new Coord(nearestPoint.getX(), nearestPoint.getY()));
		if(network.getNodes().get(Id.createNodeId(link.getId().toString()+"_"+link.getToNode().getId().toString()+"_"+i))==null)
			network.addNode(toNode);
		double length = -1;
		if(NetworkUtils.getOrigId( ((Link)link) )!=null)
			length=link.getLength()*CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), toNode.getCoord())/CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
		else
			length = CoordUtils.calcEuclideanDistance(coordinateTransformation.transform(link.getFromNode().getCoord()),coordinateTransformation.transform(toNode.getCoord()));
		LinkFactoryImpl r = NetworkUtils.createLinkFactory();
		Link newLink = NetworkUtils.createLink(Id.createLinkId(link.getId().toString()+"_"+i), link.getFromNode(), toNode, network, length, link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
		if(NetworkUtils.getOrigId( ((Link)link) )!=null)
			NetworkUtils.setOrigId( ((Link)newLink), (String) NetworkUtils.getOrigId( ((Link)link) ) ) ;
		Set<String> modes = new HashSet<String>();
		for(String mode:link.getAllowedModes())
			modes.add(mode);
		newLink.setAllowedModes(modes);
		network.addLink(newLink);
		link.setFromNode(toNode);
		link.setLength(link.getLength()-newLink.getLength());
		stops.get(i).forceSetLinkId(link.getId().toString()+"_"+i);
		return newLink;
	}
}
