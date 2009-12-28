package playground.christoph.analysis.wardrop;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public abstract class WardropZonesImpl implements WardropZones{

	protected Network network;
		
	protected double xMin;
	protected double xMax;
	protected double yMin;
	protected double yMax;
	
	protected Map<Id, List<Integer>> nodeMapping;
	protected Map<Id, List<Integer>> linkMapping;
	
	public WardropZonesImpl(Network network)
	{
		this.network = network;
		getNetworkRange();
	}
	
	private void getNetworkRange()
	{
		if (network != null)
		{
			for(Node node : network.getNodes().values())
			{
				Coord coord = node.getCoord();
				double xcoord = coord.getX();
				double ycoord = coord.getY();
				
				if (xcoord < xMin) xMin = xcoord;
				if (xcoord > xMax) xMax = xcoord;
				if (ycoord < yMin) yMin = ycoord;
				if (ycoord > yMax) yMax = ycoord;
			}
		}
	}
	
	public List<Integer> getZones(LinkImpl link)
	{
		return getZones(link.getCoord());
	}
	
	public List<Integer> getZones(NodeImpl node)
	{
		return getZones(node.getCoord());
	}
	
	public abstract List<Integer> getZones(Coord coord);

	// returns the coordinates of the centre of the given zone
	public abstract Coord getZoneCentre(int zone);
	
	public abstract void createMapping();
	
	public abstract int getZonesCount();
	
}
