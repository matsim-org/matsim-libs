package playground.mzilske.cdranalysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class IterationResource {
	
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
		new MatsimPopulationReader(baseScenario).readFile(wd + "/" + runId + "." + iteration + ".experienced_plans.xml.gz");
		return baseScenario;
	}

	public void postExperiencedPlans() {
		Scenario scenario = getRun().getOutputScenario();
		scenario.getConfig().planCalcScore().setWriteExperiencedPlans(true);
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		EventsToScore events2Score = new EventsToScore(scenario, new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getNetwork()));
		eventsManager.addHandler(events2Score);
		new MatsimEventsReader(eventsManager).readFile(getEventsFileName());
		events2Score.finish();
		events2Score.writeExperiencedPlans(wd + "/" + runId + "." + iteration + ".experienced_plans.xml.gz");
	}

	public String getEventsFileName() {
		return wd + "/" + runId + "." + iteration + ".events.xml.gz";
	}

}
