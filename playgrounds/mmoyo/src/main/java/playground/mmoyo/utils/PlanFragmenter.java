package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.PtConstants;

/**reads a pt routed population and convert each pt-connection into a plan, each new plan has a index suffix*/
public class PlanFragmenter {
	private static final Logger log = Logger.getLogger(PlanFragmenter.class);
	final String SEP = "_";
	final String STRTRIP = "_trip_";
	
	public Population run(Population population){
		ScenarioImpl tempScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationImpl newPopulation = new PopulationImpl(tempScenario);
		
		log.info("persons before fragmentation: " + population.getPersons().size());
		
		for (Person person : population.getPersons().values()) {
			List<Person> personList = this.run(person);
			for (Person newPerson : personList){
				newPopulation.addPerson(newPerson);
			}
		}
		log.info("persons after fragmentation: " + newPopulation.getPersons().size());
		return newPopulation;
	}

	public List<Person> run(Person person){
		char suffix = 'a';
		List<Person> personList = new ArrayList<Person>();
		for (Plan plan: person.getPlans()){
			Person clonPerson=null;
			Plan newPlan=null;
			Leg leg = null;
			
			int numOfLegs=0;
			
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;

					if (!PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())) {  // this is a normal act
						if (newPlan!=null){
							newPlan.addActivity(act);
							
							//add the clonPerson if he/she has a pt-connection
							if (leg.getMode().equals(TransportMode.transit_walk)){	
								clonPerson.setId(new IdImpl(clonPerson.getId().toString() + (suffix++)));
								personList.add(clonPerson);
							}

						}
						//start new plan
						numOfLegs=0;
						clonPerson = new PersonImpl(new IdImpl(person.getId().toString() + STRTRIP));
						newPlan = new PlanImpl(clonPerson);
						clonPerson.addPlan(newPlan);
					}
					newPlan.addActivity(act);
				}else{
					leg = (Leg)pe;
					newPlan.addLeg(leg);
					numOfLegs++;
				}
			}
		}
		return personList;
	}
	
	/**
	* converts all plans into new "persons", each with new suffix in Id 
	*/
	public Population plans2Persons (Population population){
		PopulationImpl outputPopulation = new PopulationImpl((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		final String SEP = "_plan";
		for (Person person : population.getPersons().values() ){
			int suffix = 1;
			for (Plan plan : person.getPlans()){
				Id newId = new IdImpl(person.getId().toString()+ SEP + suffix);
				
				Person newPerson = new PersonImpl(newId);
				newPerson.addPlan(plan);
				outputPopulation.addPerson(newPerson);
				
				suffix++;
			}
		}
		return outputPopulation;
	}
	
	
	public static void main(String[] args) {
		String populationFile = "../../";
		String networkFile = "../../";
		String outputFile = "../../";
		
		Scenario scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
		
		Population fragmPopulation = new PlanFragmenter().plans2Persons(scenario.getPopulation());
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(fragmPopulation, scenario.getNetwork()).write(outputFile);
		System.out.println("done");
	}

}
