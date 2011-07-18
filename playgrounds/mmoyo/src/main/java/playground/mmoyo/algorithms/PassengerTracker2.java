package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.population.filters.AbstractPersonFilter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
//import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

//import playground.mmoyo.utils.ExpTransRouteUtils;
import playground.mmoyo.utils.Generic2ExpRouteConverter;

/** tracks passenger traveling along the given stops of a transit route based on population*/
public class PassengerTracker2 extends AbstractPersonFilter {
	
	private Generic2ExpRouteConverter converter = new Generic2ExpRouteConverter();
	final TransitLine line;
	final Network net;
	final TransitSchedule schedule;
		
	public PassengerTracker2 (final TransitLine line, final Network net, final TransitSchedule schedule){
		this.line = line;
		this.net= net;
		this.schedule= schedule;
	}

	public List<Id> getTrackedPassengers(Population[] popArray) {
		List<Id> travelPersonList = new ArrayList<Id>();
		for (Population pop : popArray){
			List<Id> tmpPersonList = getTrackedPassengers(pop);
			for (Id tmpId : tmpPersonList){
				if(!travelPersonList.contains(tmpId)){
					travelPersonList.add(tmpId);
				}
			}
		}
		return travelPersonList;
	}
	
	public List<Id> getTrackedPassengers(Population population) {
		List<Id> travelPersonList = new ArrayList<Id>();		
		for (Person person : population.getPersons().values()) {
			if( judge(person)){
				if(!travelPersonList.contains(person.getId())){
				   travelPersonList.add(person.getId());	
				}
			} 
		}
		return travelPersonList;
	}
	
	@Override
	public boolean judge(Person person) {
		for (Plan plan : person.getPlans()){
			for (PlanElement pe: plan.getPlanElements()){
				if (pe instanceof Leg) {
					Leg leg = (Leg)pe;
					if(leg.getMode().equals(TransportMode.pt)){
						if (leg.getRoute()!= null && (leg.getRoute() instanceof org.matsim.api.core.v01.population.Route)){
							ExperimentalTransitRoute expRoute = converter.convert((GenericRouteImpl) leg.getRoute(), schedule);
							
							if (expRoute.getLineId().equals(line.getId())){  
								return true;
								//find out if the passenger travels along the stops
								/*
								ExpTransRouteUtils exputil = new ExpTransRouteUtils(net, schedule, expRoute);
								for (TransitRouteStop stop :stopList){
									if (exputil.getStops().contains(stop)){
										return true;
									}
								}
								*/
							}
						}
					}
				}
			}
		}
		return false;
	}		

}