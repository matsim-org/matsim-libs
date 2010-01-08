package playground.gregor.sim2d.network;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import com.vividsolutions.jts.geom.MultiPolygon;

public interface NetworkLoader {

	Map<MultiPolygon, List<Link>> getFloors();

	Network loadNetwork();
}
