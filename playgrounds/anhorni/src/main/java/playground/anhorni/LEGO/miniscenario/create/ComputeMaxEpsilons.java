package playground.anhorni.LEGO.miniscenario.create;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.locationchoice.bestresponse.scoring.DestinationChoiceScoring;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ComputeMaxEpsilons extends AbstractMultithreadedModule {	
	private ScenarioImpl scenario;	
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private Config config;
		
	public ComputeMaxEpsilons(int numOfThreads, ScenarioImpl scenario, String type, Config config, long seed) {
		super(numOfThreads);
		this.scenario = scenario;
		this.type = type;
		this.typedFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType(type);
		this.config = config; 
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		DestinationChoiceScoring scorer = new DestinationChoiceScoring(this.scenario.getActivityFacilities(), config);
		return new EpsilonComputer(this.scenario, this.type, typedFacilities, scorer);
	}
}
