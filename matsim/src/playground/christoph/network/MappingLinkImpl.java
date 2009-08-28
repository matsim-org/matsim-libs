package playground.christoph.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.world.Layer;

public class MappingLinkImpl extends MappingLink {

	private List<Link> mappedLinks;
	private Node fromNode;
	private Node toNode;
	private Id id;
	
	public MappingLinkImpl(Id id, Node fromNode, Node toNode, List<Link> mappedLinks)
	{
		this.id = id;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.mappedLinks = mappedLinks;
	}
	
	/*
	 * Common chase where exatly two Links are mapped.
	 * Mind the Order of the Links! The fromNode is the StartNode of the firstLink.
	 */
	public MappingLinkImpl(Id id, Node fromNode, Node toNode, Link firstLink, Link secondLink)
	{
		this(id, fromNode, toNode, null);
		
		List<Link> list = new ArrayList<Link>();
		list.add(firstLink);
		list.add(secondLink);
		this.setMappedLinks(list);
	}
	
	public List<Link> getMappedLinks()
	{
		return mappedLinks;
	}

	public void setMappedLinks(List<Link> links)
	{
		this.mappedLinks = links;
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

	public double getLength() {
		// TODO Auto-generated method stub
		return 0;
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

	public boolean setFromNode(BasicNode node) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setLength(double length) {
		// TODO Auto-generated method stub
		
	}

	public void setNumberOfLanes(double lanes) {
		// TODO Auto-generated method stub
		
	}

	public boolean setToNode(BasicNode node) {
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

	public Coord getCoord() {
		// TODO Auto-generated method stub
		return null;
	}

}
