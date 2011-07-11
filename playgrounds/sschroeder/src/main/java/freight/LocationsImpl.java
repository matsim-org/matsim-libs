package freight;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class LocationsImpl implements Locations {
	
	private Map<Id,Coord> coords = new HashMap<Id, Coord>();
	
	public void addLocation(Id id, Coord coord){
		coords.put(id, coord);
	}
	
	public void addAllNodes(Collection<Node> nodes){
		for(Node n : nodes){
			coords.put(n.getId(), n.getCoord());
		}
	}
	
	public void addAllLinks(Collection<Link> links){
		for(Link l : links){
			coords.put(l.getId(), l.getCoord());
		}
	}
	
	/* (non-Javadoc)
	 * @see core.run.testcases.cvrppdtw_lilim.Locations#getCoord(org.matsim.api.core.v01.Id)
	 */
	public Coord getCoord(Id id){
		return coords.get(id);
	}

}
