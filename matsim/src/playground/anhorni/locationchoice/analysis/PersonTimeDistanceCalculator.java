package playground.anhorni.locationchoice.analysis;

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.population.Leg;
import org.matsim.population.Person;

/*
 * This is a helper class for TravelTimeandDistanceStats
 * TODO: move this functionality to Person.java
 */

public class PersonTimeDistanceCalculator {

	private static double planTravelTime;
	private static double planTravelDistance;
	private static int numberOfLegs;


	private static void init() {
		planTravelTime=0.0;
		planTravelDistance=0.0;
		numberOfLegs=0;
	}

	public static void run(final Person person){

		init();

		final LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			final Leg leg = (Leg)leg_it.next();
			planTravelTime+=leg.getTravTime();
			planTravelDistance+=leg.getRoute().getDist();
			numberOfLegs++;
		}
	}

	public static double getPlanTravelTime() {
		return planTravelTime;
	}

	public static double getPlanTravelDistance() {
		return planTravelDistance;
	}

	public static int getNumberOfLegs() {
		return numberOfLegs;
	}
}
