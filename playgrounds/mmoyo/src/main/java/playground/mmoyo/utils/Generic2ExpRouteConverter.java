package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class Generic2ExpRouteConverter {
	private final static String SEP = "===";
	final TransitSchedule schedule;
	
	public Generic2ExpRouteConverter(final TransitSchedule schedule){
		this.schedule = schedule;
	}
	
	public ExperimentalTransitRoute convert(final GenericRouteImpl genericRoute){
		String[] strRouteEleme = genericRoute.getRouteDescription().split(SEP);
		
		//-> validate that it is a transit route
		TransitStopFacility accesFacility = schedule.getFacilities().get(Id.create(strRouteEleme[1], TransitStopFacility.class));
		TransitLine line = schedule.getTransitLines().get(Id.create(strRouteEleme[2], TransitLine.class));
		TransitRoute route = line.getRoutes().get(Id.create(strRouteEleme[3], TransitRoute.class));
		TransitStopFacility egressFacility = schedule.getFacilities().get(Id.create(strRouteEleme[4], TransitStopFacility.class));
		return new ExperimentalTransitRoute (accesFacility, line, route, egressFacility);
	}
	
	public static void main(String[] args) {
	
	}
}
