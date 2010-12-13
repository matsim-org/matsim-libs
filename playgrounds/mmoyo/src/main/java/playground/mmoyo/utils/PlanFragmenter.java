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

/**reads a pt routed population and convert each pt-connection into a plan, each new plan has a index suffix*/
public class PlanFragmenter {

	public Population run(Population population){
		ScenarioImpl tempScenario =new ScenarioImpl();
		PopulationImpl newPopulation = new PopulationImpl(tempScenario);
		
		System.out.println("persons before fragmentation: " + population.getPersons().size());
		
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

						if (!PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())) {
							if (newPlan!=null){

								//add the last clonPerson, but only if he/she has a pt-connection
								newPlan.addActivity(act);
								if (!(numOfLegs==1  && !leg.getMode().equals(TransportMode.pt))){
									newPopulation.addPerson(clonPerson);
								}

							}
							//start new plan
							numOfLegs=0;
							clonPerson = new PersonImpl(new IdImpl(person.getId().toString() + "_" + suffix++));
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
		System.out.println("persons after fragmentation: " + newPopulation.getPersons().size());
		
		return newPopulation;
	}

	public static void main(String[] args) {
		String populationFile = "../playgrounds/mmoyo/output/merge/routedPlan_walk10.0_dist0.6_tran1020.0.xml.gz";
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String outputFile = "../playgrounds/mmoyo/output/merge/Fragmented_routedPlan_walk10.0_dist0.6_tran1020.0.xml.gz";
		
		ScenarioImpl scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
		
		Population fragmPopulation = new PlanFragmenter().run(scenario.getPopulation());
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(fragmPopulation, scenario.getNetwork()).write(outputFile);
		System.out.println("done");
	}

}
