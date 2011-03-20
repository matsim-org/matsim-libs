package playground.anhorni.LEGO.miniscenario.create;

import java.util.Random;
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
	private DestinationChoiceScoring scorer;
		
	public ComputeMaxEpsilons(int numOfThreads, ScenarioImpl scenario, String type, Config config) {
		super(numOfThreads);
		this.scenario = scenario;
		this.type = type;
		this.typedFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType(type);
		this.scorer = new DestinationChoiceScoring(new Random(), this.scenario.getActivityFacilities(), config);	
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new EpsilonComputer(this.scenario, this.type, typedFacilities, this.scorer);
	}
}
