package playground.artemc.analysis;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.artemc.scoring.TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory;

public class QueueCostHandler implements IterationEndsListener{
	private static final Logger log = Logger.getLogger(QueueCostHandler.class);

	Network network;
	TransitSchedule transitSchedule; 
	double lastActivityEndTime = 0.0;
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.network = event.getControler().getNetwork();
		this.transitSchedule = event.getControler().getScenario().getTransitSchedule();
		
		for(Person person:event.getControler().getPopulation().getPersons().values()){
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements()){
				if(planElement instanceof Activity){
					Activity act = (Activity) planElement;
					lastActivityEndTime = act.getEndTime();
				}
				else if(planElement instanceof Leg){
					getDelay((Leg) planElement);
				}
				else{
				 log.error("Unknown PlanElement!");	
				}
			}
		}
		
	}
	
	public double getDelay(Leg leg) {
		
		Double freeSpeedTravelTime = 0.0;
		Double minInVehicleTime = 0.0;
		Double delay = 0.0;
		if(leg.getMode().equals(TransportMode.car)){
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			for(Id<Link> link:route.getLinkIds()){
				freeSpeedTravelTime = freeSpeedTravelTime + (network.getLinks().get(link).getLength() / network.getLinks().get(link).getFreespeed());
			}
			
		}else if(leg.getMode().equals(TransportMode.pt)){
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();	
			
			TransitStopFacility accessStop = transitSchedule.getFacilities().get(route.getAccessStopId());
			TransitStopFacility egressStop = transitSchedule.getFacilities().get(route.getEgressStopId());
			
			double scheduleDeparture = transitSchedule.getTransitLines().get(route.getLineId()).getRoutes().get(route.getRouteId()).getStop(accessStop).getDepartureOffset();
			double scheduleArrival = transitSchedule.getTransitLines().get(route.getLineId()).getRoutes().get(route.getRouteId()).getStop(egressStop).getArrivalOffset();
			minInVehicleTime = scheduleArrival = scheduleDeparture;
		
		}
		return delay;
	}




}
