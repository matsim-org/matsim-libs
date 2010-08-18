package playground.wrashid.parkingSearch.planLevel.scoring;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;

public class PersonGroupParkingScoreGraphicProducer {
	
	public static final String iterationScoreSum="scoreSum-iteration-";
	
	public static void generateGraphic(PersonGroups personGroups,String fileName){
		
		String xLabel = "Iteration";
		String yLabel = "utility score";
		String title="Average Person Group Parking Utilities";
		int numberOfXValues = GlobalRegistry.controler.getIterationNumber()+1;
		int numberOfFunctions = personGroups.getNumberOfGroups();
		double[] xValues=new double[numberOfXValues];
		String[] seriesLabels=new String[numberOfFunctions];
		
		int k=0;
		for (String groupLabel: personGroups.getGroupLabels()){
			seriesLabels[k]=groupLabel;
			k++;
		}
		
		double[][] matrix=new double[numberOfXValues][numberOfFunctions];
		
		for (int i=0;i<numberOfXValues;i++){
			
			String attribute = iterationScoreSum + i;
			for (int j=0;j<numberOfFunctions;j++){
				String groupName=seriesLabels[j];
				matrix[i][j]= (Double) personGroups.getAttributeValueForGroup(groupName, attribute) / personGroups.getGroupSize(groupName);
			}
	
			xValues[i]=i;
		}
		
		GeneralLib.writeGraphic(fileName, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}
	
}
