package playground.christoph.analysis.wardrop;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public interface WardropZones {
	
	public List<Integer> getZones(LinkImpl link);
	
	public List<Integer> getZones(NodeImpl node);

	public List<Integer> getZones(Coord coord);

	// returns the coordinates of the centre of the given zone
	public Coord getZoneCentre(int zone);
	
	public void createMapping();
	
	public int getZonesCount();
}
