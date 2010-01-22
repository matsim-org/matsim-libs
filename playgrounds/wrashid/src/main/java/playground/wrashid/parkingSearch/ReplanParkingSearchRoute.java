package playground.wrashid.parkingSearch;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ReplanParkingSearchRoute implements LinkEnterEventHandler  {

	private Controler controler;

	public ReplanParkingSearchRoute(Controler controler){
		this.controler=controler;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		System.out.println(event.toString());
		Person person= controler.getPopulation().getPersons().get(event.getPersonId());
		Plan originalPlan=person.getSelectedPlan();
		PlanImpl newPlan = new PlanImpl(person);
		Activity homeAct= (Activity) originalPlan.getPlanElements().get(0);
		
		
		
		TravelCost travelCost=controler.getTravelCostCalculator() ;
		TravelTime travelTime=controler.getTravelTimeCalculator();
		
		
		PlanAlgorithm algorithm=new PlansCalcRoute(controler.getNetwork(), travelCost, travelTime);
		algorithm.run(newPlan);
		
		
		//TODO: for continuing, look at:
		// playground.christoph.events.algorithms.LeaveLinkReplanner
		
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
}
