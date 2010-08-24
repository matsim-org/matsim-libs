package playground.gregor.sim2d.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.network.NetworkLayer;

import com.vividsolutions.jts.geom.MultiPolygon;

public class Network2D {

	private final List<Floor> floors = new ArrayList<Floor>();
	
	public Network2D(NetworkLayer network, Map<MultiPolygon, NetworkLayer> floors, StaticForceField sff) {
		if (floors.size() > 1) {
			throw new RuntimeException("this has not been implemented yet!");
		}
		for (Entry<MultiPolygon,NetworkLayer> e : floors.entrySet()) {
			this.floors.add(new Floor(e.getKey(),e.getValue(),sff));
		}
	}
	
	public List<Floor> getFloors()  {
		return this.floors;
	}
	
}
