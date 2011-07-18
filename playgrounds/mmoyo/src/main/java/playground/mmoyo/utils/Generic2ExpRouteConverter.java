package playground.mmoyo.utils;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class Generic2ExpRouteConverter {
	private final static String SEP = "===";
	
	public ExperimentalTransitRoute convert(final GenericRouteImpl genericRoute, final TransitSchedule schedule){
		String[] strRouteEleme = genericRoute.getRouteDescription().split(SEP);
		
		//-> validate that it is a transit route
		TransitStopFacility accesFacility = schedule.getFacilities().get(new IdImpl(strRouteEleme[1]));
		TransitLine line = schedule.getTransitLines().get(new IdImpl(strRouteEleme[2]));
		TransitRoute route = line.getRoutes().get(new IdImpl(strRouteEleme[3]));
		TransitStopFacility egressFacility = schedule.getFacilities().get(new IdImpl(strRouteEleme[4]));
		return new ExperimentalTransitRoute (accesFacility, line, route, egressFacility);
	}
	
	public static void main(String[] args) {
	
	}
}
