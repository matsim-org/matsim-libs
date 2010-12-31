package playground.anhorni.LEGO.miniscenario.create;

import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.LEGO.miniscenario.run.scoring.DestinationChoiceScoring;

public class ComputeMaxEpsilons extends AbstractMultithreadedModule {	
	private ScenarioImpl scenario;	
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private DestinationChoiceScoring scorer;
	private ConfigReader configReader;
	
	public ComputeMaxEpsilons(int numOfThreads, ScenarioImpl scenario, String type, ConfigReader configReader) {
		super(numOfThreads);
		this.scenario = scenario;
		this.type = type;
		this.configReader = configReader;
		this.typedFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType(type);
		this.scorer = new DestinationChoiceScoring(new Random(), this.scenario.getActivityFacilities(), configReader);	
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new EpsilonComputer(this.scenario, this.configReader, this.type, typedFacilities, this.scorer);
	}
}
