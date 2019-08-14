package commercialtraffic.demandAssigment;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;

public class ActivityChecker {
	Logger LOG = Logger.getLogger(ActivityChecker.class);
	int age;
	Activity activity;
	Integer lowerAge;
	Integer upperAge;
	Set<String> activityTypeSet;
	String mode2beUsed;
	String usedMode;
	Double[] actInterval;
	Double[] businessOpeningInterval = new Double[] { 7 * 3600.0, 18 * 3600.0 };

	ActivityChecker(Activity activity, int[] ageInterval, Set<String> activityTypeSet, int age, Double[] actInterval,
			Double[] businessOpeningInterval) {
		this.age = age;
		this.activity = activity;
		this.lowerAge = ageInterval[0];
		this.upperAge = ageInterval[1];
		this.activityTypeSet = activityTypeSet;
		this.actInterval = actInterval;

		if (businessOpeningInterval != null) {
			this.businessOpeningInterval = businessOpeningInterval;

		}

	}

	ActivityChecker(Activity activity, Set<String> activityTypeSet, Double[] actInterval,
			Double[] businessOpeningInterval) {
		this.activity = activity;
		this.activityTypeSet = activityTypeSet;
		this.actInterval = actInterval;
		if (businessOpeningInterval != null) {
			this.businessOpeningInterval = businessOpeningInterval;

		}
	}

	ActivityChecker(Activity activity, int[] ageInterval, Set<String> activityTypeSet, String mode2beUsed,
			String usedMode, int age, Double[] actInterval, Double[] businessOpeningInterval) {

		this.age = age;
		this.activity = activity;
		this.lowerAge = ageInterval[0];
		this.upperAge = ageInterval[1];
		this.activityTypeSet = activityTypeSet;
		this.mode2beUsed = mode2beUsed;
		this.usedMode = usedMode;
		this.actInterval = actInterval;
		if (businessOpeningInterval != null) {
			this.businessOpeningInterval = businessOpeningInterval;

		}

	}

	boolean isValidAge() {
		if ((age > lowerAge) && (age < upperAge))
			return true;
		else
			return false;

	}

	boolean isValidActSet() {

		String cleanAct = activity.getType().split("_")[0];
		return activityTypeSet.contains(cleanAct);
	}

	boolean reachedActbyCar() {
		if (usedMode == null)
			return false;
		else if (usedMode.equals(TransportMode.car))
			return true;
		else
			return false;
	}

	boolean businessIntervalMatchesAgentTimeAvail() {
		// a --> AgentActitivyTimeWindow
		// b --> BusinessServiceTimeTimeWindow
		double aStart = actInterval[0];
		double aEnd = actInterval[1];
		double bStart = businessOpeningInterval[0];
		double bEnd = businessOpeningInterval[1];

		if (aEnd > bStart && aStart < bEnd) {
			// System.out.println("Intersect = true # agentTime: " +aStart+" , " +aEnd+" ||
			// businessTime: " +bStart+" , " +bEnd);
			return true;

		}
		// System.out.println("Intersect = false # agentTime: " +aStart+" , " +aEnd+" ||
		// businessTime: " +bStart+" , " +bEnd);
		return false;
	}

	boolean proof() {
		// Age and activity and mode set
		if ((lowerAge != null) && (activityTypeSet != null) && (mode2beUsed != null)) {

			return (isValidAge() && isValidActSet() && reachedActbyCar() && businessIntervalMatchesAgentTimeAvail());

		}

		// Activity set
		else if ((lowerAge == null) && (activityTypeSet != null) && (mode2beUsed == null)) {

			return isValidActSet() && businessIntervalMatchesAgentTimeAvail();
		}

		// Age and activity set
		else if ((lowerAge != null) && (activityTypeSet != null) && (mode2beUsed == null)) {

			return isValidActSet() && businessIntervalMatchesAgentTimeAvail();
		}

		// TODO: We need an ActivityTimeChecker (Start and End Time)!

		else {
			LOG.warn("Parameter combination failure, returned default value: false");
			return false;
		}

	}

}
