package playground.mmoyo.utils;

import org.matsim.api.core.v01.ScenarioImpl;
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
import org.matsim.pt.PtConstants;
import org.apache.log4j.Logger;

/**reads a pt routed population and convert each pt-connection into a plan, each new plan has a index suffix*/
public class PlanFragmenter {
	private static final Logger log = Logger.getLogger(PlanFragmenter.class);
	final String SEP = "_";
	
	public Population run(Population population){
		ScenarioImpl tempScenario =new ScenarioImpl();
		PopulationImpl newPopulation = new PopulationImpl(tempScenario);
		
		log.info("persons before fragmentation: " + population.getPersons().size());
		
		for (Person person : population.getPersons().values()) {
			char suffix = 'a';

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
									newPopulation.addPerson(clonPerson);
								}

							}
							//start new plan
							numOfLegs=0;
							clonPerson = new PersonImpl(new IdImpl(person.getId().toString() + SEP));
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
		}
		log.info("persons after fragmentation: " + newPopulation.getPersons().size());
		return newPopulation;
	}

	public static void main(String[] args) {
		String populationFile = "../playgrounds/mmoyo/output/merge/routedPlan_walk10.0_dist0.6_tran1020.0.xml.gz";
		populationFile = "../playgrounds/mmoyo/output/alltest/routedPlanplans1.xml";
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String outputFile = "../playgrounds/mmoyo/output/alltest/fragmentedPlan.xml";
		
		ScenarioImpl scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
		
		Population fragmPopulation = new PlanFragmenter().run(scenario.getPopulation());
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(fragmPopulation, scenario.getNetwork()).write(outputFile);
		System.out.println("done");
	}

}
