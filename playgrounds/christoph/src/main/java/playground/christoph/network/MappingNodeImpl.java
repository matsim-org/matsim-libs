package playground.christoph.network;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.christoph.network.mapping.Mapping;

public class MappingNodeImpl extends MappingNode {

	private Id id;
	private Coord coord;
	
	private Mapping downMapping;
	private Mapping upMapping;
	
	private Map<Id, Link> inLinks;
	private Map<Id, Link> outLinks;
	
	public MappingNodeImpl(Id id, Coord coord)
	{
		this.id = id;
		
		this.coord = coord;
		
		/*
		 * We have to sort the Maps to get deterministic Results
		 * when doing the NetworkThinning. 
		 */
		this.inLinks = new TreeMap<Id, Link>(new IdComparator());
		this.outLinks = new TreeMap<Id, Link>(new IdComparator());
	}
	
	public Map<Id, ? extends Link> getInLinks()
	{
		return this.inLinks;
	}

	public Map<Id, ? extends Link> getOutLinks()
	{
		return this.outLinks;
	}

	public boolean addInLink(Link link)
	{
		return (this.inLinks.put(link.getId(), link) != null);
	}

	public boolean addOutLink(Link link)
	{
		return (this.outLinks.put(link.getId(), link) != null);
	}

	public Coord getCoord()
	{
		return this.coord;
	}

	public Id getId()
	{
		return this.id;
	}

	public Mapping getDownMapping()
	{
		return this.downMapping;
	}

	public void setDownMapping(Mapping mapping)
	{
		this.downMapping = mapping;
	}
	
	public Mapping getUpMapping()
	{
		return this.upMapping;
	}
	
	public void setUpMapping(Mapping mapping)
	{
		this.upMapping = mapping;
	}
	
	protected static class IdComparator implements Comparator<Id>, Serializable
	{
		private static final long serialVersionUID = 1L;
		
		public int compare(final Id l1, final Id l2)
		{
			return l1.compareTo(l2);
		}
	}
}