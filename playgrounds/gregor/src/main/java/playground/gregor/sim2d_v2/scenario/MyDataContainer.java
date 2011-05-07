package playground.gregor.sim2d_v2.scenario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

public class MyDataContainer {

	private final Map<Id, LineString> lsm = new HashMap<Id, LineString>();

	private final Map<MultiPolygon, List<Link>> mps = new HashMap<MultiPolygon, List<Link>>();

	public Map<Id, LineString> getLineStringMap() {
		return this.lsm;
	}

	public Map<MultiPolygon, List<Link>> getMps() {
		return this.mps;
	}

}
