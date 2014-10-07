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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

/**reads a pt routed population and convert each pt-connection into a plan, each new plan has a index suffix*/
public class PlanFragmenter {
	private static final Logger log = Logger.getLogger(PlanFragmenter.class);
	final String SEP = "_";
	final String STRTRIP = "_trip_";
	
	public Population run(Population population){
		ScenarioImpl tempScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population newPopulation = PopulationUtils.createPopulation(tempScenario.getConfig(), tempScenario.getNetwork());
		
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
                                final Id<Person> id = Id.create(clonPerson.getId().toString() + (suffix++), Person.class);
                                ((PersonImpl) clonPerson).setId(id);
                                personList.add(clonPerson);
							}

						}
						//start new plan
						numOfLegs=0;
						clonPerson = new PersonImpl(Id.create(person.getId().toString() + STRTRIP, Person.class));
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
        ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population outputPopulation = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
		final String SEP = "_plan";
		for (Person person : population.getPersons().values() ){
			int suffix = 1;
			for (Plan plan : person.getPlans()){
				Id<Person> newId = Id.create(person.getId().toString()+ SEP + suffix, Person.class);
				
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
