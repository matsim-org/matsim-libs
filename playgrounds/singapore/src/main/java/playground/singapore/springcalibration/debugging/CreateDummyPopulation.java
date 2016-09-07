package playground.singapore.springcalibration.debugging;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.singapore.springcalibration.run.RunSingapore;

public class CreateDummyPopulation {
	
	private final static Logger log = Logger.getLogger(RunSingapore.class);
	private Scenario scenario;
	private Scenario newScenario;
	private Random random = MatsimRandom.getRandom();
	
	public static void main(String[] args) {
		log.info("Create dummy population"); 
		
		CreateDummyPopulation creator = new CreateDummyPopulation();
		creator.init(args[0], args[1], args[2]);
		creator.run(args[3]);   
	}
	
	private void init(String populationFile, String networkFile, String facilitiesFile) {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		newScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFile);	
		new PopulationReader(scenario).readFile(populationFile);
		
		newScenario.addScenarioElement("network", scenario.getNetwork());
	}
	
	public void run(String outputFile) {
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			if (random.nextDouble() <= 0.01) {
				this.newScenario.getPopulation().addPerson(person);
				this.addPlan(person);
			}
		}	
		this.writePop(outputFile);
		
	}
	
	private void addPlan(Person person) {
		person.getPlans().clear();
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		
		Activity homeAct0 = PopulationUtils.createActivityFromLinkId("home", Id.create("21016-21015", Link.class));
		plan.addActivity(homeAct0);
		homeAct0.setEndTime(6.0 * 3600.0);
		homeAct0.setCoord(this.scenario.getNetwork().getLinks().get(Id.create("21016-21015", Link.class)).getCoord());
		
		Leg leg = PopulationUtils.createLeg(TransportMode.walk);
		plan.addLeg(leg);
		
		Activity workAct = PopulationUtils.createActivityFromLinkId("work", Id.create("21010-24021", Link.class));
		plan.addActivity(workAct);
		workAct.setEndTime(18.0 * 3600.0);
		workAct.setCoord(this.scenario.getNetwork().getLinks().get(Id.create("21010-24021", Link.class)).getCoord());
	}
	
	
	private void writePop(String outputFile) {
		new PopulationWriter(this.newScenario.getPopulation()).write(outputFile);
	}

}
