package playground.christoph.analysis.wardrop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class CircularWardropZones extends WardropZonesImpl{

	// 6000 x 6000
	protected int zonesX = 123;
	protected int zonesY = 51;
	protected double radius;
	
	protected Map<Integer, Coord> zoneCentres;
	protected boolean validMapping = false;
	
	public CircularWardropZones(Network network) 
	{
		super(network);
		createMapping();
	}
	
	@Override
	public int getZonesCount()
	{
		return zonesX * zonesY;
	}
	
	public void setZonesX(int zones)
	{
		zonesX = zones;
		validMapping = false;
	}
	
	public void setZonesY(int zones)
	{
		zonesY = zones;
		validMapping = false;
	}
	
	private void calcRadius()
	{
		double xLength = (xMax - xMin) / zonesX;
		double yLength = (yMax - yMin) / zonesY;
		
		radius = 0.5 * Math.sqrt(xLength * xLength + yLength * yLength);
	}
	
	
	@Override
	public void createMapping()
	{
		if (!validMapping)
		{		
			zoneCentres = new LinkedHashMap<Integer, Coord>();
			for (int i = 0; i < zonesX * zonesY; i++)
			{
				zoneCentres.put(i, getZoneCentre(i));
			}
		
			calcRadius();
			
			nodeMapping = new LinkedHashMap<Id, List<Integer>>();
			for (Node node : network.getNodes().values())
			{
				Coord coord = node.getCoord();
				nodeMapping.put(node.getId(), this.getZones(coord));
				
			}
			
			linkMapping = new LinkedHashMap<Id, List<Integer>>();
			for (Link link : network.getLinks().values())
			{
				Coord coord = link.getCoord();
				linkMapping.put(link.getId(), this.getZones(coord));
			}
			
			
			validMapping = true;
		}
	}
		
	@Override
	public List<Integer> getZones(LinkImpl link)
	{
		if (linkMapping.containsKey(link.getId())) return linkMapping.get(link.getId());
		else return super.getZones(link);
	}
	
	@Override
	public List<Integer> getZones(NodeImpl node)
	{
		if (nodeMapping.containsKey(node.getId())) return nodeMapping.get(node.getId());
		return super.getZones(node);
	}
	
	@Override
	public List<Integer> getZones(Coord coord)
	{
		List<Integer> zones = new ArrayList<Integer>();
		
		for (int i = 0; i < zoneCentres.size(); i++)
		{
			Coord centre = zoneCentres.get(i);
			double distance = ((CoordImpl)centre).calcDistance(coord);
			if (distance <= radius) zones.add(i);
		}
		return zones;
	}

	// returns the coordinates of the centre of the given zone
	@Override
	public Coord getZoneCentre(int zone)
	{
		if (zoneCentres.containsKey(zone)) return zoneCentres.get(zone);
		
		double dx = (xMax - xMin) / zonesX;
		double dy = (yMax - yMin) / zonesY;
		
		int xIndex = zone;
		int yIndex = 0;
		
		while (xIndex >= zonesX)
		{
			yIndex++;
			xIndex = xIndex - zonesX;
		}
		
		double xCoord = xMin + (xIndex * dx + dx/2);
		double yCoord = yMax - (yIndex * dy + dy/2);
		
		Coord coord = new CoordImpl(xCoord, yCoord);
		
		return coord;
	}

}
