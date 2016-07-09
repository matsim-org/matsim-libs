package besttimeresponse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RealizedTimeStructure {

	final List<RealizedActivity> realizedActivities;

	public RealizedTimeStructure(final List<PlannedActivity> activities, final RealVector plannedDptTimes_s,
			final InterpolatedTravelTimes travelTimes) {

		/*
		 * (1) Extract realized departure and arrival times.
		 */

		final RealVector realizedDptTimes_s = new ArrayRealVector(activities.size());
		final RealVector realizedArrivalTimes_s = new ArrayRealVector(activities.size());
		final List<InterpolatedTravelTimes.Entry> ttEntries = new ArrayList(activities.size());

		double realizedDptTime_s = plannedDptTimes_s.getEntry(0);
		for (int dptIndex = 0; dptIndex < activities.size(); dptIndex++) {
			final int nextDptIndex = ((dptIndex + 1 < activities.size()) ? dptIndex + 1 : 0);
			final InterpolatedTravelTimes.Entry ttEntry = travelTimes.getEntry(activities.get(dptIndex),
					activities.get(nextDptIndex), realizedDptTime_s);
			final double arrivalTime_s = realizedDptTime_s + ttEntry.getTravelTime_s(realizedDptTime_s);
			realizedDptTimes_s.setEntry(dptIndex, realizedDptTime_s);
			realizedArrivalTimes_s.setEntry(nextDptIndex, arrivalTime_s);
			ttEntries.add(ttEntry);
			realizedDptTime_s = Math.max(arrivalTime_s, plannedDptTimes_s.getEntry(nextDptIndex));
		}

		/*
		 * (2) Create realized activity time information
		 */
		this.realizedActivities = new ArrayList<>(activities.size());
		for (int dptIndex = 0; dptIndex < activities.size(); dptIndex++) {
			this.realizedActivities
					.add(new RealizedActivity(activities.get(dptIndex), realizedDptTimes_s.getEntry(dptIndex),
							realizedArrivalTimes_s.getEntry(dptIndex), ttEntries.get(dptIndex)));
		}
	}

	// public double getArrivalTime_s(final int activityIndex) {
	// return this.arrivalTimes_s.getEntry(activityIndex);
	// }
	//
	// public double getDepartureTime_s(final int activityIndex) {
	// return this.departureTimes_s.getEntry(activityIndex);
	// }
	//
	// private double timePressure(final int actIndex) {
	// return 1.0;
	// }
	//
	
	public Trip getTrip(final int originActivityIndex) {
		throw new UnsupportedOperationException();
	}
	
	// public Trip getTrip(final int originActivityIndex) {
	// final int destinationActivityIndex;
	// if (originActivityIndex == this.plannedAcitivites.size() - 1) {
	// destinationActivityIndex = 0;
	// } else {
	// destinationActivityIndex = originActivityIndex + 1;
	// }
	// return new Trip(this.departureTimes_s.getEntry(originActivityIndex),
	// this.arrivalTimes_s.getEntry(destinationActivityIndex),
	// this.timePressure(originActivityIndex),
	// this.timePressure(destinationActivityIndex),
	// this.plannedAcitivites.get(originActivityIndex).closingTime_s,
	// this.plannedAcitivites.get(destinationActivityIndex).openingTime_s,
	// this.plannedAcitivites.get(originActivityIndex).earliestDepartureTime_s,
	// this.plannedAcitivites.get(destinationActivityIndex).latestArrivalTime_s,
	// this.departureTimes_s.getEntry(destinationActivityIndex));
	// }
}
