package playground.wrashid.parkingSearch.planLevel.scoring;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;

public class PersonGroupParkingScoreGraphicGenerator {
	
	public static final String iterationScoreSum="scoreSum-iteration-";
	
	public static void generateGraphic(PersonGroups personGroups,String fileName){
		
		String xLabel = "Iteration";
		String yLabel = "utility score";
		String title="Average Person Group Parking Utilities";
		
		personGroups.generateIterationAverageGraph(xLabel, yLabel, title, iterationScoreSum, fileName);
		
	}
	
}
