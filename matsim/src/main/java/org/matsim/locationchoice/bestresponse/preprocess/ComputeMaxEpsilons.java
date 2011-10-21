package org.matsim.locationchoice.bestresponse.preprocess;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.locationchoice.bestresponse.scoring.DestinationChoiceScoring;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class ComputeMaxEpsilons extends AbstractMultithreadedModule {	
	private ScenarioImpl scenario;	
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private Config config;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
		
	public ComputeMaxEpsilons(ScenarioImpl scenario, String type, Config config, 
			ObjectAttributes facilitiesKValues, ObjectAttributes personsKValues) {
		super(config.global().getNumberOfThreads());
		this.scenario = scenario;
		this.type = type;
		this.typedFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType(type);
		this.config = config; 
		this.facilitiesKValues = facilitiesKValues;
		this.personsKValues = personsKValues;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		DestinationChoiceScoring scorer = new DestinationChoiceScoring(this.scenario.getActivityFacilities(), config, 
				this.facilitiesKValues, this.personsKValues);
		return new EpsilonComputer(this.scenario, this.type, typedFacilities, scorer);
	}
}
