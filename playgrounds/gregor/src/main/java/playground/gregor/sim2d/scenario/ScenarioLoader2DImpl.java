package playground.gregor.sim2d.scenario;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.MultiPolygon;

import playground.gregor.sim2d.controller.Sim2DConfig;
import playground.gregor.sim2d.gisdebug.StaticForceFieldToShape;
import playground.gregor.sim2d.network.NetworkLoader;
import playground.gregor.sim2d.simulation.Force;
import playground.gregor.sim2d.simulation.StaticForceField;
import playground.gregor.sim2d.simulation.StaticForceFieldGenerator;
import playground.gregor.sim2d.simulation.StaticForceFieldReader;
import playground.gregor.sim2d.simulation.StaticForceFieldWriter;

public class ScenarioLoader2DImpl extends ScenarioLoaderImpl {

	private Map<MultiPolygon, List<Link>> mps;

	private StaticForceField sff;
	
	public ScenarioLoader2DImpl(ScenarioImpl scenarioData) {
		super(scenarioData);
	}

	@Override
	public void loadNetwork() {
		NetworkLoader loader = new NetworkLoader(getScenario().getNetwork());
		this.mps = loader.getFloors();
		if (this.mps.size() > 1) {
			throw new RuntimeException("multiple floors are not supported yet");
		}
		loadStaticForceField();
	}
	
	private void loadStaticForceField() {
		if (Sim2DConfig.LOAD_STATIC_FORCE_FIELD_FROM_FILE) {
			this.sff = new StaticForceFieldReader(Sim2DConfig.STATIC_FORCE_FIELD_FILE).getStaticForceField();
		} else {
			this.sff = new StaticForceFieldGenerator(this.mps.keySet().iterator().next()).loadStaticForceField();
			new StaticForceFieldWriter().write(Sim2DConfig.STATIC_FORCE_FIELD_FILE, this.sff);
		}
		new StaticForceFieldToShape(this.sff).createShp();
	}

	public Map<MultiPolygon,List<Link>> getFloorLinkMapping() {
		return this.mps;
	}

	public StaticForceField getStaticForceField() {
		return this.sff;
	}

}
