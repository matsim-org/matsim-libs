package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**compares pt connections of two planes and return a percentage of similar pt routes*/
public class PlansComparator4PtRoute {
	final String strPtInteraction = "pt interaction";
	private Generic2ExpRouteConverter generic2ExpRouteConverter;
	
	public PlansComparator4PtRoute(final TransitSchedule schedule){
		generic2ExpRouteConverter = new Generic2ExpRouteConverter(schedule);
	}
	
	private boolean haveSameNormalActs(Plan plan1, Plan plan2){
		List<Activity> normalActs1 = new ArrayList<Activity>();
		for (PlanElement pe1: plan1.getPlanElements()){
			if (pe1 instanceof Activity){
				Activity act = (Activity)pe1;
				if (!act.getType().equals(strPtInteraction)){
					normalActs1.add(act);	
				}
			}
		}
			
		List<Activity> normalActs2 = new ArrayList<Activity>();
		for (PlanElement pe2: plan2.getPlanElements()){
			if (pe2 instanceof Activity){
				Activity act = (Activity)pe2;
				if (!act.getType().equals(strPtInteraction)){
					normalActs2.add(act);	
				}
			}
		}

		if (normalActs1.size()!= normalActs2.size()){
			return false; //do not have the same number of acts
		}

		Iterator<Activity> acts2It = normalActs2.iterator();
		for (Activity act1 : normalActs1){
			Activity act2= acts2It.next();
			if (!act1.getType().equals(act2.getType())){  //we can't compare by coordinates or time because those attributes may be changed by replanning
				return false;//error : activities have different type
			}
		}
		return true;
	}
	
	
	private double getPtRouteSimilarity(final Plan plan1, final Plan plan2){
		double percentage=0;
		int maxRouteNum=0;
		int similarRoutes=0;
		
		if (this.haveSameNormalActs(plan1, plan2)){
			//get pt legs of plan1
			List<List<Route>> routeList1 = new ArrayList<List<Route>>();
			for (PlanElement pe1: plan1.getPlanElements()){
				if (pe1 instanceof Activity){
					Activity act1 = (Activity) pe1;
					if (!act1.getType().equals(strPtInteraction) && (plan1.getPlanElements().indexOf(pe1) < plan1.getPlanElements().size()-1)){  //don't do it for last act
						routeList1.add(new ArrayList<Route>());
					}
				}else{
					Leg leg = (Leg)pe1; 		
					if (leg.getMode().equals(TransportMode.pt)){
						List<Route> lastRouteList= routeList1.get(routeList1.size()-1);
						lastRouteList.add(leg.getRoute());
					}
				}
			}
			
			//get pt legs of plan2
			List<List<Route>> routeList2 = new ArrayList<List<Route>>();
			for (PlanElement pe2: plan2.getPlanElements()){
				if (pe2 instanceof Activity){
					Activity act2 = (Activity) pe2;
					if (!act2.getType().equals(strPtInteraction)  && (plan2.getPlanElements().indexOf(pe2) < plan2.getPlanElements().size()-1)){  //don't do it for last act
						routeList2.add(new ArrayList<Route>());
					}
				}else{
					Leg leg = (Leg)pe2; 		
					if (leg.getMode().equals(TransportMode.pt)){
						List<Route> lastRouteList= routeList2.get(routeList2.size()-1);
						lastRouteList.add(leg.getRoute());
					}
				}
			}
			
			
			Iterator<List<Route>> iter2=  routeList2.iterator();
			for (List<Route> routeListA: routeList1){  //List1 and List2 should have the same size: the number of acts-1
				List<Route> routeListB = iter2.next();
				List<Route> largerList = routeListA; 

				List<Route> shorterList =  routeListB;
				if(routeListB.size()>routeListA.size()){
					shorterList = routeListA; 
					largerList =  routeListB;
				}	
				maxRouteNum +=largerList.size();
				
				for (Route routeS : shorterList){
					ExperimentalTransitRoute expRouteS = this.generic2ExpRouteConverter.convert((GenericRouteImpl) routeS);
					for (Route routeL : largerList){
						ExperimentalTransitRoute expRouteL = this.generic2ExpRouteConverter.convert((GenericRouteImpl) routeL);
						if(expRouteS.getRouteDescription().equals(expRouteL.getRouteDescription())){
							similarRoutes++;	
						}
					}
				}
			}
			if (maxRouteNum>0){
				percentage = (similarRoutes/maxRouteNum)*100;	
			}
			
		}
		return percentage;
	} 

	
	public static void main(String[] args) {
		String scheduleFilePath = "../../";
		String netFilepath  = "../../";
		String planPath = "../../";
		
		DataLoader dataloader = new DataLoader();
		TransitSchedule schedule = dataloader.readTransitSchedule(scheduleFilePath);
		PlansComparator4PtRoute plansComparator4PtRoute = new PlansComparator4PtRoute(schedule);
		
		Population pop = dataloader.readNetwork_Population(netFilepath, planPath).getPopulation();
		
		final String tab = "\t";
		final String strIdAgent= "M";
		final String strPlan = "plan";
		final Id<Person> idAgent= Id.create(strIdAgent, Person.class);
		Person person = pop.getPersons().get(idAgent);
		for (int i=0; i< person.getPlans().size(); i++){
			Plan plan_i=   person.getPlans().get(i);
			for (int y=i+1; y< person.getPlans().size(); y++){
				Plan plan_y=   person.getPlans().get(y);
				double similarity = plansComparator4PtRoute.getPtRouteSimilarity(plan_i, plan_y);
				System.out.println (strPlan + tab + i + tab +  y + tab + similarity);
			}
		}
		
	}
}