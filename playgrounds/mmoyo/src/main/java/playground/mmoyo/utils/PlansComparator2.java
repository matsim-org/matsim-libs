package playground.mmoyo.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Compares two plans to find out if they have same activity types and leg-routes (ignores departure and duration times)
 */
public class PlansComparator2 {
	private final static Logger log = Logger.getLogger(PlansComparator.class);
	private Generic2ExpRouteConverter generic2ExpRouteConverter;
	
	public PlansComparator2(final TransitSchedule schedule){
		generic2ExpRouteConverter = new Generic2ExpRouteConverter(schedule);
	}
	
	private boolean compare(Plan plan1, Plan plan2){
		log.info(plan1.getPerson().getId());
		if(plan1.getPlanElements().size()!= plan2.getPlanElements().size()){
			log.error("different number of plan elements in: " + plan1.getPerson().getId());
			return false;
		}
			
		for (int i=0; i<plan1.getPlanElements().size(); i++){
				PlanElement pe1=plan1.getPlanElements().get(i);
				PlanElement pe2=plan2.getPlanElements().get(i);

				if ((pe1 instanceof Activity)) {
					if (!(pe2 instanceof Activity)){
						log.error("different plan element sequence");
						return false;
					}
					Activity act1 = (Activity)pe1;
					Activity act2 = (Activity)pe1;
						
					if (!act1.getType().equals(act2.getType()) ){  //compare act type
						log.error("different act types");
						return false;
					}
					
					if (act1.getCoord()!=null  && act2.getCoord()!=null){  // compare act coordinates
						if(act1.getCoord() != act2.getCoord()){
							log.error("different act coordinates");
							return false;
						}
					}
					
				}else{
					if (pe2 instanceof Activity){
						log.error("different plan element sequence");
						return false;
					}
					Leg leg1 = (Leg)pe1;
					Leg leg2 = (Leg)pe2;

					if(!leg1.getMode().equals(leg2.getMode())){
						log.error("different transport mode in leg");
						return false;
					}
					if (leg1.getMode().equals(TransportMode.pt))	{
						
						if ( (leg1.getRoute()==null && leg2.getRoute()!=null) || (leg1.getRoute()!=null && leg2.getRoute()==null)){
							log.error("one route was found, the other was not");
							return false;
						}
						
						if ( (leg1.getRoute()==null && leg2.getRoute()==null) ){ //in both cases, no routes were found but for this purposes it is considered as "same leg route" 
							return true;
						}

						ExperimentalTransitRoute expRoute1 = this.generic2ExpRouteConverter.convert((GenericRouteImpl) leg1.getRoute());
						ExperimentalTransitRoute expRoute2 = this.generic2ExpRouteConverter.convert((GenericRouteImpl) leg2.getRoute());	
					
						if(!expRoute1.getRouteDescription().equals(expRoute2.getRouteDescription())){
							log.error("different pt route in leg");
							return false;
						}
					}
				}
			}
		return true;
	}
	
	/**compares only selected plan of given persons*/
	private void compareSelectedPlan(Person person1, Person person2){
		compare (person1.getSelectedPlan() , person2.getSelectedPlan());
	}
	
	/**compares all plan of given persons*/
	private int countDifferentPlans(Person person1, Person person2){
		int diffCounter=0;
		if(person1.getPlans().size()!= person2.getPlans().size()){
			log.warn("different number of plans in agent: " + person1.getId() );	
		}
		for (int i=0; i< person1.getPlans().size(); i++){
			Plan plan1 = person1.getPlans().get(i);
			Plan plan2 = person2.getPlans().get(i);
			if (!compare (plan1, plan2)){
				diffCounter++;
			}
		}
		return diffCounter;
	}
	
	/**compares selected plans of every person*/
	private void compareSelectedPlans (Population pop1, Population pop2){
		for (Person person1 : pop1.getPersons().values()){
			Person person2 = pop2.getPersons().get(person1.getId());
			compareSelectedPlan(person1, person2);
		}
	}
	
	/**compares all plans of every person of the given populations*/
	private int countDiffPersons (Population pop1, Population pop2){
		int diffCounter=0;
		for (Person person1 : pop1.getPersons().values()){
			Person person2 = pop2.getPersons().get(person1.getId());
			if(countDifferentPlans(person1, person2)>0){
				diffCounter++;
			}
		}
		return diffCounter;
	}
	
	public static void main(String[] args) {
		final String popFilePath1;
		final String popFilePath2;
		final String transitScheduleFile;
		
		if (args.length==3){
			popFilePath1= args[0];
			popFilePath2= args[1];
			transitScheduleFile = args[2];
		}else{
			popFilePath1= "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/poponlyM44.xml.gz";
			popFilePath2= "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.plans.xml.gz";
			transitScheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		}
		
		DataLoader dLoader = new DataLoader();
		Population pop1 = dLoader.readPopulation(popFilePath1);
		Population pop2 = dLoader.readPopulation(popFilePath2);
		TransitSchedule transitSchedule = dLoader.readTransitSchedule(transitScheduleFile);
		
		int numDiffPersons= new PlansComparator2(transitSchedule).countDiffPersons(pop1, pop2);
		System.out.println("number of different persons in plans: " + numDiffPersons);
		
	}
}