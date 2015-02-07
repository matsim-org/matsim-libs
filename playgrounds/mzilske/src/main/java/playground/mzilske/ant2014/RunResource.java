package playground.mzilske.ant2014;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;

public class RunResource {

	private String wd;
	private String runId;

	public RunResource(String wd, String runId) {
		this.wd = wd;
		this.runId = runId;
	}

	public IterationResource getIteration(int iteration) {
		return new IterationResource(wd + "/ITERS/it." + iteration + "/", runId, iteration);
	}
	
	public IterationResource getLastIteration() {
		Config config = getOutputConfig();
		return getIteration(config.controler().getLastIteration());
	}

	public Config getOutputConfig() {
		final Config config = ConfigUtils.loadConfig(wd + "/" + runPrefix() + "output_config.xml");
		return config;
	}
	
	public Scenario getConfigAndNetwork() {
		Scenario baseScenario = ScenarioUtils.createScenario(getOutputConfig());
		new MatsimNetworkReader(baseScenario).readFile(wd + "/" + runPrefix() + "output_network.xml.gz");
		return baseScenario;
	}
	
	public Scenario getOutputScenario() {
		Scenario scenario = getConfigAndNetwork();
		new MatsimPopulationReader(scenario).readFile(wd + "/" + runPrefix() + "output_plans.xml.gz");
		if (scenario.getConfig().scenario().isUseTransit()) {
			new TransitScheduleReader(scenario).readFile(wd + "/" + runPrefix() + "output_transitSchedule.xml.gz");
			new VehicleReaderV1(scenario.getTransitVehicles()).readFile(wd + "/" + runPrefix() + "output_vehicles.xml.gz");
		}
		return scenario;
	}

	private String runPrefix() {
		if (runId == null) {
			return "";
		} else {
			return runId + ".";
		}
	}

}
