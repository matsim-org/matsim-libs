package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class GroupSwitches extends CompareSelectedParkingPropertyOneRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run199/output/";
		int startIteration = 0;
		int endIteration = startIteration + 9;
		int iterationStep = 1;
		boolean ignoreCasesWithNoGroupChange = false;

		Matrix currentIterationMatrix = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						startIteration));

		int indexPersonId = currentIterationMatrix.getColumnIndex("personId");
		int indexLeg = currentIterationMatrix.getColumnIndex("legIndex");
		int indexGroupName = currentIterationMatrix.getColumnIndex("groupName");

		TwoKeyHashMapWithDouble<String, String> groupSwitches = new TwoKeyHashMapWithDouble<String, String>();

		for (int i = startIteration + 1; i < endIteration; i += iterationStep) {
			Matrix eventsMatrixNextIter = GeneralLib
					.readStringMatrix(getEventsFileName(outputFolder, i + 1));

			TwoHashMapsConcatenated<Id<Person>, Integer, Integer> indexMatrixB = getIndex(eventsMatrixNextIter);

			for (int j = 1; j < currentIterationMatrix.getNumberOfRows(); j++) {
				String facilityId = currentIterationMatrix.getString(j, 6);
				Id<Person> personId = Id.create(currentIterationMatrix.getString(j,
						indexPersonId), Person.class);
				int legIndex = currentIterationMatrix.getInteger(j, indexLeg);
				String groupNameCurrentIter = currentIterationMatrix.getString(
						j, indexGroupName);
				String groupNameNextIter = eventsMatrixNextIter.getString(
						indexMatrixB.get(personId, legIndex), indexGroupName);

				if (facilityId.contains("stp") || facilityId.contains("gp")
						|| facilityId.contains("illegal")) {
					if (ignoreCasesWithNoGroupChange
							&& groupNameCurrentIter
									.equalsIgnoreCase(groupNameNextIter)) {
						continue;
					}
					
					groupNameCurrentIter=ActivityStrategyGroupShares.addGroupPrefix(groupNameCurrentIter);
					groupNameNextIter=ActivityStrategyGroupShares.addGroupPrefix(groupNameNextIter);
					
					groupNameCurrentIter=groupNameCurrentIter.replaceAll("\\(", "-").replaceAll("\\)", "-").replace("TakeClosestGarageParking",
							"TCGP").replaceAll("--", "-");
					groupNameNextIter=groupNameNextIter.replaceAll("\\(", "-").replaceAll("\\)", "-").replace("TakeClosestGarageParking",
							"TCGP").replaceAll("--", "-");
					
					groupSwitches.increment(groupNameCurrentIter,
							groupNameNextIter);
				}
			}

			currentIterationMatrix = eventsMatrixNextIter;
		}

		HashSet<String> allGroupNames = new HashSet<String>();

		for (String strategyGroupA : groupSwitches.getKeySet1()) {
			for (String strategyGroupB : groupSwitches
					.getKeySet2(strategyGroupA)) {
				allGroupNames.add(strategyGroupA);
				allGroupNames.add(strategyGroupB);
			}
		}

		LinkedList<String> orderedGroupNames = new LinkedList<String>(
				allGroupNames);

		System.out.print("labels");
		for (String strategyGroup : orderedGroupNames) {
			System.out.print("\t" + strategyGroup);
		}
		System.out.println();

		for (String strategyGroupA : orderedGroupNames) {
			System.out.print(strategyGroupA);
			for (String strategyGroupB : orderedGroupNames) {
				System.out.print("\t"
						+ Math.round(groupSwitches.get(strategyGroupA,
								strategyGroupB)));
			}
			System.out.println();
		}

	}

}
