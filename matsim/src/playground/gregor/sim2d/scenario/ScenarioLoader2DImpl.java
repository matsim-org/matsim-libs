package playground.gregor.sim2d.scenario;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import com.vividsolutions.jts.geom.MultiPolygon;

import playground.gregor.sim2d.network.NetworkLoader;

public class ScenarioLoader2DImpl extends ScenarioLoaderImpl {

	private Map<MultiPolygon, List<Link>> mps;

	public ScenarioLoader2DImpl(ScenarioImpl scenarioData) {
		super(scenarioData);
	}

	@Override
	public void loadNetwork() {
		NetworkLoader loader = new NetworkLoader(getScenario().getNetwork());
		this.mps = loader.getFloors();
	}
	
	public Map<MultiPolygon,List<Link>> getFloorLinkMapping() {
		return this.mps;
	}


}
