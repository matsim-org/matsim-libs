package org.matsim.locationchoice.bestresponse.preprocess;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.locationchoice.bestresponse.scoring.DestinationChoiceScoring;
import org.matsim.locationchoice.utils.ActTypeConverter;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class ComputeMaxEpsilons extends AbstractMultithreadedModule {	
	private ScenarioImpl scenario;	
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private Config config;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
	private static final Logger log = Logger.getLogger(ComputeMaxEpsilons.class);
		
	public ComputeMaxEpsilons(ScenarioImpl scenario, String type, Config config, 
			ObjectAttributes facilitiesKValues, ObjectAttributes personsKValues) {
		super(config.global().getNumberOfThreads());
		this.scenario = scenario;
		this.type = type;
		this.typedFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType(ActTypeConverter.convert2FullType(type));
		if (this.typedFacilities.size() == 0) {
			log.warn("There are no facilities for type : " + type);
		}
		this.config = config; 
		this.facilitiesKValues = facilitiesKValues;
		this.personsKValues = personsKValues;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		DestinationChoiceScoring scorer = new DestinationChoiceScoring(this.scenario.getActivityFacilities(), config, 
				this.facilitiesKValues, this.personsKValues);
		return new EpsilonComputer(this.scenario, this.type, typedFacilities, scorer, this.config);
	}
}
