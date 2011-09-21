package playground.gregor.sim2d_v2.scenario;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import com.vividsolutions.jts.geom.LineString;

public class MyDataContainer {

	private final Map<Id, LineString> lsm = new HashMap<Id, LineString>();


	public Map<Id, LineString> getLineStringMap() {
		return this.lsm;
	}


}
