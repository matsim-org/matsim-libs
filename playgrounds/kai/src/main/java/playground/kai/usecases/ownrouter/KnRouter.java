package playground.kai.usecases.ownrouter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

public class KnRouter implements PlanStrategyModule, LinkEnterEventHandler, LinkLeaveEventHandler, 
PersonArrivalEventHandler {
	
	// PlanStrategModule implementation:

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlePlan(Plan plan) {
		for ( PlanElement pe : plan.getPlanElements() ) {
			if ( pe instanceof Activity ) {
				double endTime = ((Activity)pe).getEndTime() ;
				double newEndTime ;
				if ( Math.random() < 0.5 ) {
					newEndTime = endTime + Math.random() * 3600. ;
				} else {
					newEndTime = endTime - Math.random() * 3600. ;
				}
				((Activity)pe).setEndTime(newEndTime) ;
			}
		}
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub

	}
	
	// EventHandler implementation:

	private Map<Id,Double> linkEnterEventsMap = new HashMap<Id,Double>() ;

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id vehicleId = event.getVehicleId() ;
		double now = event.getTime() ;
		linkEnterEventsMap.put( vehicleId, now ) ;
	}
	
	private List<Double> sums = new ArrayList<Double>() ;
	private List<Double> cnts = new ArrayList<Double>() ;
	
	private int timeToBin( double time ) {
		return (int)(time/900.) ;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id vehicleId = event.getVehicleId() ;
		double now = event.getTime() ;
		Double linkEnterTime = this.linkEnterEventsMap.get(vehicleId) ;
		if ( linkEnterTime!=null ) {
			double linkTravelTime = now - linkEnterTime ;
			Integer timeBin = timeToBin( linkEnterTime ) ;
			
			Double sum = sums.get(timeBin) ;
			Double cnt = cnts.get(timeBin) ;
			
			sums.add( timeBin, sum+linkTravelTime ) ;
			cnts.add( timeBin, cnt+1 ) ;
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.linkEnterEventsMap.remove(event.getPersonId() ) ;
		
	}

	@Override
	public void reset(int iteration) {
		this.linkEnterEventsMap.clear() ;
	}

}
