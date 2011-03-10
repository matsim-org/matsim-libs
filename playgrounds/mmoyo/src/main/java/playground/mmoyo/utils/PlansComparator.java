package playground.mmoyo.utils;

import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.router.TransitActsRemover;

public class PlansComparator {
	private final static Logger log = Logger.getLogger(PlansComparator.class);
	final Population population1;
	final Population population2;
	private TransitActsRemover remover;
	
	protected PlansComparator(final Population population1, final Population population2){
		this.population1 = population1;
		this.population2 = population2;
	}
	
	protected void CompareScores(){
		int similar=0;
		int dissimilar = 0;
		String Equals = " equals";
		String NoEquals = " does not equal";
		for (Person person1 : population1.getPersons().values()){
			Person person2 = population2.getPersons().get(person1.getId());
			boolean scoreEquals = Double.compare(person1.getSelectedPlan().getScore() , person2.getSelectedPlan().getScore())==0;
			if (scoreEquals){
				similar++;
			}else{
				dissimilar++;
			}
			System.out.println(person1.getId() + (scoreEquals? Equals: NoEquals));
		}
		System.out.println(" similar: " + similar + "\ndisimilar: " + dissimilar);
	}

	/**
	 * For the two populations compares the adequacy of agents, their start times and departure times 
	 */
	private void CompareTimes(){
		remover = new TransitActsRemover();
		for(Person person1: this.population1.getPersons().values()){
			if (population2.getPersons().containsKey(person1.getId())){
				Person person2 = population2.getPersons().get(person1.getId());
				
				Plan plan1 = person1.getSelectedPlan();
				Plan plan2 = person2.getSelectedPlan();
				remover.run(plan1);
				remover.run(plan2);
				
				List<PlanElement> pes1= plan1.getPlanElements();
				List<PlanElement> pes2= plan2.getPlanElements();
				
				for (int i=0; i<pes1.size(); i++){
					PlanElement pe1 = pes1.get(i);
					PlanElement pe2 = pes2.get(i);
					
					if ((pe1 instanceof Activity)) {
						Activity act1 = (Activity)pe1;
						Activity act2 = (Activity)pe2;
						if (act1.getEndTime()!= act2.getEndTime()){
							log.error("diferent end time: " + person1.getId());
						}
					}else{
						Leg leg1 = (Leg)pe1;
						Leg leg2 = (Leg)pe2;
						if (!leg1.getMode().equals(leg2.getMode())){
							log.error("Diferent pt-mode: " + person1.getId());
						}
					}	
				}
			}else{
				log.error("this person is not present in the second population: " + person1.getId() );
			}
		}
		
		for(Person person2: this.population2.getPersons().values()){
			if (!population1.getPersons().containsKey(person2.getId())){
				log.error("this person is not present in the first population: " + person2.getId());
			}
		}

	}
	
	/**
	 * Compares person per person two populations 
	 */
	public static void main(String[] args) {
		final String popFilePath1;
		final String popFilePath2;
		
		if (args.length==2){
			popFilePath1= args[0];
			popFilePath2= args[1];
		}else{
			popFilePath1= "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_1x.xml.gz";
			popFilePath2= "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_1x.xml.gz";
		}
		
		DataLoader dLoader = new DataLoader();
		Population pop1 = dLoader.readPopulation(popFilePath1);
		Population pop2 = dLoader.readPopulation(popFilePath2);
		
		new PlansComparator(pop1, pop2).CompareTimes();
	}
}

