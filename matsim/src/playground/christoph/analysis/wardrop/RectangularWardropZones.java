package playground.christoph.analysis.wardrop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class RectangularWardropZones extends WardropZonesImpl{

	// 5000 x 5000
//	protected int zonesX = 148;
//	protected int zonesY = 62;

	// 6000 x 6000
	protected int zonesX = 123;
	protected int zonesY = 51;
	
	// max...	
//	protected int zonesX = 140;
//	protected int zonesY = 70;

	protected Map<Integer, Coord> zoneCentres;
	protected boolean validMapping = false;
	
	public RectangularWardropZones(NetworkLayer network) 
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
		double dx = (xMax - xMin) / zonesX;
		double dy = (yMax - yMin) / zonesY;

		int xZone = 0;
		while(coord.getX() > xMin + dx*(xZone + 1))
		{
			xZone++;
		}
		
		int yZone = 0;
		while(coord.getY() < yMax - dy*(yZone + 1))
		{
			yZone++;
		}
		List<Integer> result = new ArrayList<Integer>();
		result.add(xZone + yZone * zonesX);
		return result;
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
