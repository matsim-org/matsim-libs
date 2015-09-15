package playground.mzilske.ant2014;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class IterationResource {

    public String getWd() {
        return wd;
    }

    private String wd;
	
	private int iteration;

	private String runId;

	public IterationResource(String wd, String runId, int iteration) {
		this.wd = wd;
		this.runId = runId;
		this.iteration = iteration;
	}

	private RunResource getRun() {
		return new RunResource(wd + "../../", runId);
	}

	public Scenario getExperiencedPlansAndNetwork() {
		Scenario baseScenario = getRun().getConfigAndNetwork();
		new MatsimPopulationReader(baseScenario).readFile(wd + "/" + iterationPrefix() + "experienced_plans.xml.gz");
		return baseScenario;
	}

	public void postExperiencedPlans() {
		Scenario scenario = getRun().getOutputScenario();
		scenario.getConfig().planCalcScore().setWriteExperiencedPlans(true);
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		EventsToScore events2Score = new EventsToScore(scenario, new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getConfig().scenario(), scenario.getNetwork()));
		eventsManager.addHandler(events2Score);
		new MatsimEventsReader(eventsManager).readFile(getEventsFileName());
		events2Score.finish();
		events2Score.writeExperiencedPlans(wd + "/" + iterationPrefix() + "experienced_plans.xml.gz");
	}

	public String getEventsFileName() {
		return wd + "/" + iterationPrefix() + "events.xml.gz";
	}
	
	private String iterationPrefix() {
		if (runId == null) {
			return iteration + ".";
		} else {
			return runId + "." + iteration + ".";
		}
	}

	public Population getExperiencedPlans() {
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReaderMatsimV5(scenario).readFile(wd + "/" + iterationPrefix() + "experienced_plans.xml.gz");
		return scenario.getPopulation();
	}

    public Population getPlans() {
        final Config config = ConfigUtils.createConfig();
        final Scenario scenario = ScenarioUtils.createScenario(config);
        new PopulationReaderMatsimV5(scenario).readFile(wd + "/" + iterationPrefix() + "plans.xml.gz");
        return scenario.getPopulation();
    }

}
