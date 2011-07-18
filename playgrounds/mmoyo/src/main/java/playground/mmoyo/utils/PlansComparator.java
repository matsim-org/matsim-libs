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

/**
 * Compares person per person two populations 
 */
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
	
	private boolean CompareAll(){
		if (this.population1.getPersons().size()!= this.population2.getPersons().size()){  //number of agents must be the same
			log.error("different number of agents");
			return false;
		}
		
		for (Person person1 : this.population1.getPersons().values()){
			Person person2= population2.getPersons().get(person1.getId());
			
			if (person2 == null){ //all persons of population1 are in population
				log.error("Id's do not match");
				return false;
			}

			if(person1.getPlans().size()!= person2.getPlans().size()){//agents must have same number of plans
				log.error("agents do not have the same number of plans");
				return false;
			}
			
			for (int i=0; i< person1.getPlans().size(); i++){
				Plan plan1 = person1.getPlans().get(i);
				Plan plan2 = person2.getPlans().get(i);
			
				if(plan1.getPlanElements().size()!= plan2.getPlanElements().size()){//plans must have the same number of pe
					log.error("agents have different numbre of plan elements");
					return false;
				}
			
				for (int j=0; j<plan1.getPlanElements().size(); j++){
					PlanElement pe1=plan1.getPlanElements().get(j);
					PlanElement pe2=plan2.getPlanElements().get(j);

					if ((pe1 instanceof Activity)) {
						if (!(pe2 instanceof Activity)){
							log.error("different plan element secquence");
							return false;
						}
						Activity act1 = (Activity)pe1;
						Activity act2 = (Activity)pe1;
						
						if (act1 != act2){
							log.error("different acts");
							return false;
						}
						
					}else{
						if (pe2 instanceof Activity){
							log.error("different plan element secquence");
							return false;
						}
						Leg leg1 = (Leg)pe1;
						Leg leg2 = (Leg)pe2;
						
						if (leg1.equals(leg2)){
							log.error("different legs");
							log.error(leg1.toString());
							log.error(leg2.toString());
							return false;
						}
						
					}
					
				}
			}
		}
		return true;
	}
	
	public static void main(String[] args) {
		final String popFilePath1;
		final String popFilePath2;
		
		if (args.length==2){
			popFilePath1= args[0];
			popFilePath2= args[1];
		}else{
			popFilePath1= "../../input/linkOffTest/10.plans.xml.gz";
			popFilePath2= "../../input/linkOffTest/200.plans.xml.gz";
		}
		
		DataLoader dLoader = new DataLoader();
		Population pop1 = dLoader.readPopulation(popFilePath1);
		Population pop2 = dLoader.readPopulation(popFilePath2);
		
		boolean areTheSame = new PlansComparator(pop1, pop2).CompareAll();
		
		System.out.println(areTheSame);
	}
}

