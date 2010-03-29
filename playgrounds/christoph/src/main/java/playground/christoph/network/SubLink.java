package playground.christoph.network;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.world.Layer;

//public class SubLink implements BasicLink, Link{
public class SubLink implements Link{

	private Network network;
	private Node from;
	private Node to;
	private Link parentLink;

	public SubLink(Network network, Node from, Node to, Link link)
	{
		this.network = network;
		this.from = from;
		this.to = to;
		this.parentLink = link;
	}

	public Link getParentLink()
	{
		return this.parentLink;
	}

	@Override
	public Node getFromNode()
	{
		return this.from;
	}

	@Override
	public Node getToNode()
	{
		return this.to;
	}

	@Override
	public double getCapacity() {
		return this.parentLink.getCapacity();
	}

	@Override
	public double getCapacity(final double time)
	{
		return this.parentLink.getCapacity(time);
	}

	@Override
	public double getFreespeed() {
		return this.parentLink.getFreespeed();
	}

	@Override
	public double getFreespeed(final double time)
	{
		return this.parentLink.getFreespeed(time);
	}

	@Override
	public double getLength()
	{
		return this.parentLink.getLength();
	}

	@Override
	public double getNumberOfLanes() {
		return this.parentLink.getNumberOfLanes();
	}

	@Override
	public double getNumberOfLanes(final double time)
	{
		return this.parentLink.getNumberOfLanes(time);
	}

	@Override
	public Set<TransportMode> getAllowedModes()
	{
		return this.parentLink.getAllowedModes();
	}

	@Override
	public void setAllowedModes(Set<TransportMode> modes)
	{
		// nothing to do...
	}

	@Override
	public void setCapacity(double capacity)
	{
		// nothing to do...
	}

	@Override
	public void setFreespeed(double freespeed)
	{
		// nothing to do...
	}

	@Override
	public boolean setFromNode(Node node)
	{
		this.from = node;
		return true;
	}

	@Override
	public void setLength(double length)
	{
		// nothing to do...
	}

	@Override
	public void setNumberOfLanes(double lanes)
	{
		// nothing to do...
	}

	@Override
	public boolean setToNode(Node node)
	{
		this.to = node;
		return true;
	}

	@Override
	public Id getId()
	{
		return this.parentLink.getId();
	}

	@Override
	@Deprecated
	public Layer getLayer()
	{
		return this.parentLink.getLayer();
	}

	@Override
	public Coord getCoord()
	{
		return this.parentLink.getCoord();
	}

	@Override
	public boolean equals(final Object other)
	{
		if (other instanceof Link)
		{
			return this.getId().equals(((Link)other).getId());
		}
		return false;
	}
}