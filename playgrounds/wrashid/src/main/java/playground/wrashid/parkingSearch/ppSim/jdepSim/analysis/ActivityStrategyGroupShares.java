package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;

// see D:\data\Dropbox\ETH\Projekte\STRC2014\experiments\activityStrategyShare for visualization procedure
public class ActivityStrategyGroupShares extends
		CompareSelectedParkingPropertyOneRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run135/output/";
		int startIteration = 499;

		Matrix eventsMatrix = GeneralLib.readStringMatrix(getEventsFileName(
				outputFolder, startIteration));

		// strategy group, activity, frequencies
		TwoKeyHashMapWithDouble<String, String> activityStrategyGroupFrequencies = new TwoKeyHashMapWithDouble<String, String>();

		for (int i = 1; i < eventsMatrix.getNumberOfRows(); i++) {
			String facilityId = eventsMatrix.getString(i, 6);
			String groupName = eventsMatrix.getString(i, 11);
			String activity = eventsMatrix.getString(i, 14);
			// if (facilityId.contains("stp") || facilityId.contains("gp") ||
			// facilityId.contains("illegal") ){
			
			if (activity.contains("work")){
				activity="work";
			}

			activityStrategyGroupFrequencies.increment(groupName, activity);
			// }
		}

		System.out.print("strategyGroup");
		System.out.print("\t");
		System.out.print("activity");
		System.out.print("\t");
		System.out.println("frequency");
		for (String strategyGroup : activityStrategyGroupFrequencies
				.getKeySet1()) {
			for (String activity : activityStrategyGroupFrequencies
					.getKeySet2(strategyGroup)) {
				System.out.print(strategyGroup);
				System.out.print("\t");
				System.out.print(activity);
				System.out.print("\t");
				System.out.println(activityStrategyGroupFrequencies.get(
						strategyGroup, activity));
			}
		}
	}

}
