package playground.christoph.network;

import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.network.BasicLinkImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

public class SubLink extends BasicLinkImpl implements Link{

	private LinkImpl parentLink;
	
	public SubLink(NetworkLayer network, BasicNode from, BasicNode to, LinkImpl link)
	{
		super(network, link.getId(), from, to);
		this.parentLink = link;
	}
	
	@Override
	public SubNode getFromNode()
	{ 
		return (SubNode)this.from;
	}
	
	@Override
	public SubNode getToNode()
	{
		return (SubNode)this.to;
	}
	
	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 *
 	 * @param time - the current time
	 * @return the capacity per network's capperiod timestep
	 */
	@Override
	public double getCapacity(final double time)
	{
		return parentLink.getCapacity(time);
	}

	/**
	 * This method returns the freespeed velocity in meter per seconds.
	 *
	 * @param time - the current time
	 * @return freespeed
	 */
	@Override
	public double getFreespeed(final double time)
	{
		return parentLink.getFreespeed(time);
	}

	@Override
	public double getLength()
	{
		return parentLink.getLength();
	}

	@Override
	public double getNumberOfLanes(final double time)
	{
		return parentLink.getNumberOfLanes(time);
	}
	
}