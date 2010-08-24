package playground.wrashid.parkingSearch.planLevel.occupancy;

import playground.wrashid.lib.obj.plan.PersonGroups;

public class PersonGroupWalkingDistanceGraphGenerator {

	public static final String iterationWalkingDistanceSum = "walkingDistanceSum-iteration-";

	public static void generateGraphic(PersonGroups personGroups,
			String fileName) {

		String xLabel = "Iteration";
		String yLabel = "walking distance [m]";
		String title = "Average Person Group Walking Distance";
		personGroups.generateIterationAverageGraph(xLabel, yLabel, title, iterationWalkingDistanceSum, fileName);
	}
}
