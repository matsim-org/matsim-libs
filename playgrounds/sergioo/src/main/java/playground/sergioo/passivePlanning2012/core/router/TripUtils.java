package playground.sergioo.passivePlanning2012.core.router;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public class TripUtils {
	
	//Methods
	public static double calcTravelTime(List<? extends PlanElement> trip) {
		double totalTime = 0;
		for(PlanElement p:trip)
			totalTime += p instanceof Activity?((Activity)p).getMaximumDuration():((Leg)p).getTravelTime();
		return totalTime;
	}
	public static void moveInTime(List<? extends PlanElement> trip, double d) {
		for(PlanElement p:trip)
			if(p instanceof Activity) {
				((Activity)p).setStartTime(((Activity)p).getStartTime()+d);
				((Activity)p).setEndTime(((Activity)p).getEndTime()+d);
			}
			else
				((Leg)p).setDepartureTime(((Leg)p).getDepartureTime()+d);
	}

}
