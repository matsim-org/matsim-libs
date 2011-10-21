package org.matsim.locationchoice.bestresponse.preprocess;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.locationchoice.utils.RandomFromVarDistr;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class ComputeKValsAndMaxEpsilon {
	
	private final static Logger log = Logger.getLogger(ComputeKValsAndMaxEpsilon.class);
	private ScenarioImpl scenario;	
	private Config config;	
	RandomFromVarDistr rnd;
	
	private ObjectAttributes facilitiesKValues = new ObjectAttributes();
	private ObjectAttributes personsKValues = new ObjectAttributes();
	private ObjectAttributes personsMaxEps = new ObjectAttributes();
	
	public ComputeKValsAndMaxEpsilon(long seed, ScenarioImpl scenario, Config config) {
		rnd = new RandomFromVarDistr();
		rnd.setSeed(seed);
	}
	
	public void assignKValues() {				
		this.assignKValuesPersons();
		this.assignKValuesAlternatives();	
	}
		
	// does not matter which distribution is chosen here
	private void assignKValuesPersons() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			personsKValues.putAttribute(p.getId().toString(), "k", rnd.getUniform(1.0));
		}
		// write person k values
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.personsKValues);
		attributesWriter.writeFile(config.controler().getOutputDirectory() + "personsKValues.xml");
	}	
	private void assignKValuesAlternatives() {
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			facilitiesKValues.putAttribute(facility.getId().toString(), "k", rnd.getUniform(1.0));
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.facilitiesKValues);
		attributesWriter.writeFile(config.controler().getOutputDirectory() + "facilitiesKValues.xml");
	}
	
	public void run() {			
		log.info("Assigning k values ...");				
		this.assignKValues(); 
				
		log.info("Computing max epsilon ... for " + this.scenario.getPopulation().getPersons().size() + " persons");
		ComputeMaxEpsilons maxEpsilonComputer = new ComputeMaxEpsilons(this.scenario, "s", config, this.facilitiesKValues, this.personsKValues);
		maxEpsilonComputer.prepareReplanning();
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			maxEpsilonComputer.handlePlan(p.getSelectedPlan());
		}
		maxEpsilonComputer.finishReplanning();
		
		maxEpsilonComputer = new ComputeMaxEpsilons(this.scenario, "l", config, this.facilitiesKValues, this.personsKValues);
		maxEpsilonComputer.prepareReplanning();
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			maxEpsilonComputer.handlePlan(p.getSelectedPlan());
		}
		maxEpsilonComputer.finishReplanning();
		
		this.writeMaxEps();
	}
	
	private void writeMaxEps() {
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			double maxShop = Double.parseDouble(((PersonImpl)person).getDesires().getDesc().split("_")[0]);
			double maxLeisure = Double.parseDouble(((PersonImpl)person).getDesires().getDesc().split("_")[1]);
			this.personsMaxEps.putAttribute(person.getId().toString(), "s", maxShop);
			this.personsMaxEps.putAttribute(person.getId().toString(), "l", maxLeisure);
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.personsMaxEps);
		attributesWriter.writeFile(this.config.controler().getOutputDirectory() + "personsMaxEps.xml");
	}

	public ObjectAttributes getFacilitiesKValues() {
		return facilitiesKValues;
	}
	public void setFacilitiesKValues(ObjectAttributes facilitiesKValues) {
		this.facilitiesKValues = facilitiesKValues;
	}
	public ObjectAttributes getPersonsKValues() {
		return personsKValues;
	}
	public void setPersonsKValues(ObjectAttributes personsKValues) {
		this.personsKValues = personsKValues;
	}
	public ObjectAttributes getPersonsMaxEps() {
		return personsMaxEps;
	}
	public void setPersonsMaxEps(ObjectAttributes personsMaxEps) {
		this.personsMaxEps = personsMaxEps;
	}
}
