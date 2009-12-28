package playground.christoph.network;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.world.Layer;

import playground.christoph.network.mapping.Mapping;

public class MappingLinkImpl extends MappingLink {

	private static final Logger log = Logger.getLogger(MappingLinkImpl.class);
	
	private Node fromNode;
	private Node toNode;
	private Id id;
	private Coord coord;
	
	private double length = 0.0;
	
	private Mapping downMapping;
	private Mapping upMapping;
	
	public MappingLinkImpl(Id id, Node fromNode, Node toNode)
	{
		this.id = id;
		this.fromNode = fromNode;
		this.toNode = toNode;
		
		double x = (fromNode.getCoord().getX() + toNode.getCoord().getX()) / 2;
		double y = (fromNode.getCoord().getY() + toNode.getCoord().getY()) / 2;
		this.coord = new CoordImpl(x, y);
	}
		
	public Node getFromNode()
	{
		return fromNode;
	}

	public Node getToNode()
	{
		return toNode;
	}

	public Set<TransportMode> getAllowedModes() {
		// TODO Auto-generated method stub
		return null;
	}

	public double getCapacity(double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getFreespeed(double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getLength()
	{
//		return this.mapping.getLength();
		return this.length;
	}

	public double getNumberOfLanes(double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setAllowedModes(Set<TransportMode> modes) {
		// TODO Auto-generated method stub
		
	}

	public void setCapacity(double capacity) {
		// TODO Auto-generated method stub
		
	}

	public void setFreespeed(double freespeed) {
		// TODO Auto-generated method stub
		
	}

	public boolean setFromNode(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setLength(double length) {
		// TODO Auto-generated method stub
		
	}

	public void setNumberOfLanes(double lanes) {
		// TODO Auto-generated method stub
		
	}

	public boolean setToNode(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	public Id getId()
	{
		return id;
	}

	public Layer getLayer()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Coord getCoord()
	{
		return this.coord;
	}

	public Mapping getDownMapping()
	{
		return this.downMapping;
	}

	public void setDownMapping(Mapping mapping)
	{
		this.downMapping = mapping;
		
		this.length = mapping.getLength();
		
//		Object object = mapping.getInput();
//		if (object instanceof List)
//		{
//			List<Object> list = (List) object;
//			
//			for (Object element : list)
//			{
//				if (element instanceof Link)
//				{
//					Link link = (Link) element;
//					this.length = this.length + link.getLength();
//				}
//			}
//		}
//		else if (object instanceof Link)
//		{
//			Link link = (Link) object;
//			this.length = this.length + link.getLength();
//		}
//		else
//		{
//			log.warn("Could not map the Length of the MappingLink.");
//		}
	}

	public Mapping getUpMapping()
	{
		return this.upMapping;
	}
	
	public void setUpMapping(Mapping mapping)
	{
		this.upMapping = mapping;
	}
}
