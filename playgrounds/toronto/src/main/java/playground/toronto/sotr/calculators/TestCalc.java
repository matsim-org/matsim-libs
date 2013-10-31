package playground.toronto.sotr.calculators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.toronto.sotr.config.SOTRConfig;
import playground.toronto.sotr.routernetwork2.RoutingInVehicleLink;
import playground.toronto.sotr.routernetwork2.AbstractRoutingLink;
import playground.toronto.sotr.routernetwork2.RoutingWalkLink;

public class TestCalc implements SOTRDisutilityCalculator, SOTRTimeCalculator {

	private SOTRConfig config;
	
	@Override
	public double getLinkTravelTime(AbstractRoutingLink link, double now,
			Person person, Vehicle vehicle) {
		
		if (link instanceof RoutingWalkLink){
			return ((RoutingWalkLink) link).getLength() / config.beelineWalkSpeed_m_s; // m / (m/s) => s
		}
		else if (link instanceof RoutingInVehicleLink){
			return ((RoutingInVehicleLink) link).getNextTravelTime(now);
		}else{
			throw new UnsupportedOperationException("Unrecognized link type " + link.getClass().getName());
		}
	}
	
	@Override
	public double getTurnTravelTime(AbstractRoutingLink fromLink, AbstractRoutingLink toLink,
			double now, Person person, Vehicle vehicle) {
		// TODO Auto-generated method stub
		
		if (fromLink instanceof RoutingWalkLink){
			RoutingWalkLink wrappedFrom = (RoutingWalkLink) fromLink;
			if (toLink instanceof RoutingInVehicleLink){
				//Waiting from walking
				double waiting = ((RoutingInVehicleLink) toLink).getNextDepartureTime(now);
			}
			
		}else if (fromLink instanceof RoutingInVehicleLink){
			if (toLink instanceof RoutingInVehicleLink){				
				if (((RoutingInVehicleLink) fromLink).getRoute() == ((RoutingInVehicleLink) toLink).getRoute()){
					//Dwelling in-vehicle at a stop
				}else{
					//Transfer across two routes
				}
				
				
			}
			
		}else{
			throw new UnsupportedOperationException("Unrecognized link type " + fromLink.getClass().getName());
		}
		
		return 0;
	}

	@Override
	public double getLinkTravelDisutility(AbstractRoutingLink link, double now,
			Person person, Vehicle vehicle) {
		
		if (link instanceof RoutingWalkLink){
			double time = ((RoutingWalkLink) link).getLength() / config.beelineWalkSpeed_m_s;
			
			switch (((RoutingWalkLink) link).getType()){
			case ACCESS:
				//First boarding
				break;
			default:
				//All other cases
				break;
			}
		}else if (link instanceof RoutingInVehicleLink){
			double time = ((RoutingInVehicleLink) link).getNextTravelTime(now);
			//In-vehicle link
		}
		
		//TODO
		return 0;
	}

	@Override
	public double getTurnTravelDisutility(AbstractRoutingLink fromLink,
			AbstractRoutingLink toLink, double time, Person person, Vehicle vehicle) {
		// TODO Auto-generated method stub
		return 0;
	}

}
