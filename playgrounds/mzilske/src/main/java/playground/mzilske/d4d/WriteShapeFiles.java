package playground.mzilske.d4d;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;

public class WriteShapeFiles {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(D4DConsts.WORK_DIR + "network-simplified.xml");
		new MatsimPopulationReader(scenario).readFile(D4DConsts.WORK_DIR + "population.xml");
		SelectedPlans2ESRIShape s1 = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS(D4DConsts.TARGET_CRS), D4DConsts.WORK_DIR);
		s1.write();
		WriteActsInSpace s = new WriteActsInSpace(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS(D4DConsts.TARGET_CRS), D4DConsts.WORK_DIR);
		s.write();
		Links2ESRIShape l = new Links2ESRIShape(scenario.getNetwork(), D4DConsts.WORK_DIR + "links.shp", D4DConsts.TARGET_CRS);
		l.write();
	}

}
