package playground.wrashid.parkingSearch.ppSim;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.misc.Time;

// TODO: make parallel!
// TODO: read tt matrix
// TODO: make event ordering work properly (such that times are sorted)
public class PPSim implements Mobsim {

	private Scenario sc;
	private EventsManager eventsManager;

	public PPSim(Scenario sc, EventsManager eventsManager){
		this.sc = sc;
		this.eventsManager = eventsManager;
	}
	
	@Override
	public void run() {
		for (Person p:sc.getPopulation().getPersons().values()){
			Event event = null;
			
			ActivityImpl ai= (ActivityImpl) p.getSelectedPlan().getPlanElements().get(0);
			
			
			
			// process first activity
			double time=ai.getEndTime();
			event = new ActivityEndEvent(ai.getEndTime(), p.getId(), ai.getLinkId(), ai.getFacilityId(), ai.getType());
			eventsManager.processEvent(event);

			int planElemSize = p.getSelectedPlan().getPlanElements().size();
			for (int i=1;i<planElemSize-1;i++){
				PlanElement planElement = p.getSelectedPlan().getPlanElements().get(i);
				if ( planElement instanceof Activity){
					time=simulateActivity((Activity) planElement,time,p.getId());
				} else {
					time=simulateLeg((Leg) planElement,time,p.getId());
				}
			}
			
			ai= (ActivityImpl) p.getSelectedPlan().getPlanElements().get(planElemSize-1);
			
			// process last activity
			event = new ActivityStartEvent(time, p.getId(), ai.getLinkId(), ai.getFacilityId(), ai.getType());
			eventsManager.processEvent(event);
			
		}
		
	}
	
	private double simulateActivity(Activity act, double arrivalTime, Id personId){
		double time=arrivalTime;
		
		Event event = new ActivityStartEvent(time, personId, act.getLinkId(), act.getFacilityId(), act.getType());
		eventsManager.processEvent(event);
		
		double actDurBasedDepartureTime = Double.MAX_VALUE;
		double actEndTimeBasedDepartureTime = Double.MAX_VALUE;

		if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
			actDurBasedDepartureTime = time + act.getMaximumDuration();
		}

		if (act.getEndTime() != Time.UNDEFINED_TIME) {
			actEndTimeBasedDepartureTime = act.getEndTime();
		}

		double departureTime = actDurBasedDepartureTime < actEndTimeBasedDepartureTime ? actDurBasedDepartureTime
				: actEndTimeBasedDepartureTime;
		
		
		if (departureTime < time) {
			departureTime = time;
		}
		
		time=departureTime;
		
		event = new ActivityEndEvent(time, personId, act.getLinkId(), act.getFacilityId(), act.getType());
		eventsManager.processEvent(event);

		return time;
	}

	// 
	private double simulateLeg(Leg leg, double departureTime, Id personId){
		double time=departureTime;
		
		if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
			Event event = new PersonDepartureEvent(time, personId, leg.getRoute().getStartLinkId() , leg.getMode());
			eventsManager.processEvent(event);
			
			List<Id<Link>> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
			
			if (linkIds.size()>2){
				event=new Wait2LinkEvent(time,personId,leg.getRoute().getStartLinkId(),personId, leg.getMode(), 1.0);
				eventsManager.processEvent(event);
				
				for (int i=1;i<linkIds.size()-1;i++){
					Id linkId = linkIds.get(i);
					event=new LinkEnterEvent(time,personId,linkId,personId);
					eventsManager.processEvent(event);
					time+=getTravelTime(time, linkId);
					event=new LinkLeaveEvent(time,personId,linkId,personId);
					eventsManager.processEvent(event);
				}
			}
			
			Id endLinkId = leg.getRoute().getEndLinkId();
			event=new LinkEnterEvent(time,personId,endLinkId,personId);
			eventsManager.processEvent(event);
			time+=getTravelTime(time, endLinkId);
			event = new PersonArrivalEvent(time, personId, endLinkId , leg.getMode());
			eventsManager.processEvent(event);
		} else {
			Event event = new PersonDepartureEvent(time, personId, leg.getRoute().getStartLinkId() , leg.getMode());
			eventsManager.processEvent(event);
			
			time+=leg.getTravelTime();
			// TODO: auch oev, etc. uebernehmen.
			// bzw. non relevant for parking search.
			
			Id endLinkId = leg.getRoute().getEndLinkId();
			event = new PersonArrivalEvent(time, personId, endLinkId , leg.getMode());
			eventsManager.processEvent(event);
		}
		
		return time;
		
	}

	
	private double getTravelTime(double time, Id linkId){
		return 100;
	}
	

}
