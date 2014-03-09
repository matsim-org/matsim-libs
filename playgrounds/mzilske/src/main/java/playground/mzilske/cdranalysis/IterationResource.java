package playground.mzilske.cdranalysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class IterationResource {
	
	private String wd;
	
	private int iteration;

	public IterationResource(String wd, int iteration) {
		this.wd = wd;
		this.iteration = iteration;
	}

	public Scenario getExperiencedPlansAndNetwork() {
		Scenario baseScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(baseScenario).readFile(wd + "/" + iteration + ".experienced_plans.xml.gz");
		new MatsimNetworkReader(baseScenario).readFile(wd + "../../output_network.xml.gz");
		return baseScenario;
	}

}
