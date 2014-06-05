package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
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
		String outputFolder = "h:/data/experiments/parkingSearchOct2013/runs/run135/output1/";
		int startIteration = 499;
		boolean makeRoughStrategyGroups=false;
		boolean addPrefix=true;

		Matrix eventsMatrix = GeneralLib.readStringMatrix(getEventsFileName(
				outputFolder, startIteration));

		// strategy group, activity, frequencies
		TwoKeyHashMapWithDouble<String, String> activityStrategyGroupFrequencies = new TwoKeyHashMapWithDouble<String, String>();

		for (int i = 1; i < eventsMatrix.getNumberOfRows(); i++) {
			String facilityId = eventsMatrix.getString(i, 6);
			String groupName = eventsMatrix.getString(i, 11);
			String activity = eventsMatrix.getString(i, 14);
			if (facilityId.contains("stp") || facilityId.contains("gp")
					|| facilityId.contains("illegal")) {

				if (makeRoughStrategyGroups){
					groupName = makeRoughStrategyGroups(groupName); 
				}
				
				if (addPrefix){
					groupName = addGroupPrefix(groupName); 
				}
				
				
				if (activity.contains("work")) {
					activity = "work";
				}

				if (activity.contains("education")) {
					activity = "education";
				}

				if (groupName.contains("TakeClosestGarageParking")) {
					groupName = groupName.replace("TakeClosestGarageParking",
							"TCGP");
				}
				

				activityStrategyGroupFrequencies.increment(groupName, activity);
			}
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

	public static String addGroupPrefix(String groupName) {
		if (groupName.equalsIgnoreCase("ARD-G")){
			groupName="GP-" + groupName;
		} else if (groupName.equalsIgnoreCase("BRD(300m)-G")){
			groupName="GP-" + groupName;
		} else if (groupName.equalsIgnoreCase("ARD-TakeClosestGarageParking")){
			groupName="GP-" + groupName;
		}else if (groupName.equalsIgnoreCase("BRD-TakeClosestGarageParking")){
			groupName="GP-" + groupName;
		}else if (groupName.equalsIgnoreCase("BRD(300m)-S-G")){
			groupName="SP-" + groupName;
		}else if (groupName.equalsIgnoreCase("Parkagent")){
			groupName="SP-" + groupName;
		}else if (groupName.equalsIgnoreCase("ARD-S")){
			groupName="SP-" + groupName;
		}else if (groupName.equalsIgnoreCase("BRD(300m)-S")){
			groupName="SP-" + groupName;
		}else if (groupName.equalsIgnoreCase("ARD-waiting-S")){
			groupName="SP-" + groupName;
		}else if (groupName.equalsIgnoreCase("ARD-illegal-S")){
			groupName="IP-" + groupName;
		}else{
			DebugLib.stopSystemAndReportInconsistency();
		}
		return groupName;
	}

	public static String makeRoughStrategyGroups(String groupName) {
		if (groupName.equalsIgnoreCase("ARD-G")){
			groupName="garage";
		} else if (groupName.equalsIgnoreCase("BRD(300m)-G")){
			groupName="garage";
		} else if (groupName.equalsIgnoreCase("ARD-TakeClosestGarageParking")){
			groupName="garage";
		}else if (groupName.equalsIgnoreCase("BRD-TakeClosestGarageParking")){
			groupName="garage";
		}else if (groupName.equalsIgnoreCase("BRD(300m)-S-G")){
			groupName="street";
		}else if (groupName.equalsIgnoreCase("Parkagent")){
			groupName="street";
		}else if (groupName.equalsIgnoreCase("ARD-S")){
			groupName="street";
		}else if (groupName.equalsIgnoreCase("BRD(300m)-S")){
			groupName="street";
		}else if (groupName.equalsIgnoreCase("ARD-waiting-S")){
			groupName="street";
		}else if (groupName.equalsIgnoreCase("ARD-illegal-S")){
			groupName="illegal";
		}else{
			DebugLib.stopSystemAndReportInconsistency();
		}
		return groupName;
	}

}
