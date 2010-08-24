package playground.wrashid.parkingSearch.planLevel.analysis;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.Collections;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingWalkingDistanceMeanAndStandardDeviationGraph {

	LinkedList<StatisticalValues> statisticalValues=new LinkedList<ParkingWalkingDistanceMeanAndStandardDeviationGraph.StatisticalValues>();
	
	public void updateStatisticsForIteration(int iternationNumber, HashMap<Id, Double> parkingWalkingDistance) {
		if (statisticalValues.size()!=iternationNumber){
			throw new Error("the iteration number is wrong!!!!");
		}
		
		
		int numberOfAgents=parkingWalkingDistance.size();
		double[] valueArray=Collections.convertDoubleCollectionToArray(parkingWalkingDistance.values());
		
		StatisticalValues iterationStatValues=new StatisticalValues();
		iterationStatValues.mean=new Mean().evaluate(valueArray);
		iterationStatValues.standardDeviation=new StandardDeviation().evaluate(valueArray);

		statisticalValues.add(iterationStatValues);
		
		
		
		if (ParkingRoot.getParkingWalkingDistanceOfPreviousIteration()!=null){
			double sumOfWalkingDistanceIncrease=0;
			double sumOfWalkingDistanceDecrease=0;
			
			for (Id personId:parkingWalkingDistance.keySet()){
				double walkingDistanceDifference=parkingWalkingDistance.get(personId)-ParkingRoot.getParkingWalkingDistanceOfPreviousIteration().get(personId);
			
				if (walkingDistanceDifference>=0){
					sumOfWalkingDistanceIncrease+=walkingDistanceDifference;
				} else {
					sumOfWalkingDistanceDecrease+=Math.abs(walkingDistanceDifference);
				}
			}
			
			iterationStatValues.averageIncreaseInParkingWalkingDistance=sumOfWalkingDistanceIncrease/numberOfAgents;
			iterationStatValues.averageDecreaseInParkingWalkingDistance=sumOfWalkingDistanceDecrease/numberOfAgents;
		}
	}
	
	
	public void writeGraphic(String fileName){
		String xLabel = "Iteration";
		String yLabel = "parking walking distance [m]";
		String title="Parking Walking Distance";
		int numberOfXValues = statisticalValues.size();
		int numberOfFunctions = 5;
		double[] xValues=new double[numberOfXValues];
		String[] seriesLabels=new String[numberOfFunctions];
		
		seriesLabels[0]="mean";
		seriesLabels[1]="mean+stdDev";
		seriesLabels[2]="mean-stdDev";
		seriesLabels[3]="averageIncreaseInParkingWalkingDistance";
		seriesLabels[4]="averageDecreaseInParkingWalkingDistance";
		
		double[][] matrix=new double[numberOfXValues][numberOfFunctions];
		
		double parkingDistanceScalingFactor=ParkingRoot.getParkingWalkingDistanceScalingFactorForOutput();
		
		for (int i=0;i<numberOfXValues;i++){
			matrix[i][0]=statisticalValues.get(i).mean*parkingDistanceScalingFactor;
			matrix[i][1]=(statisticalValues.get(i).mean+statisticalValues.get(i).standardDeviation)*parkingDistanceScalingFactor;
			matrix[i][2]=(statisticalValues.get(i).mean-statisticalValues.get(i).standardDeviation)*parkingDistanceScalingFactor;
			matrix[i][3]=statisticalValues.get(i).averageIncreaseInParkingWalkingDistance*parkingDistanceScalingFactor;
			matrix[i][4]=statisticalValues.get(i).averageDecreaseInParkingWalkingDistance*parkingDistanceScalingFactor;
			xValues[i]=i;
		}

		GeneralLib.writeGraphic(fileName, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}


	private class StatisticalValues {

		public double mean;
		public double standardDeviation;
		public double averageIncreaseInParkingWalkingDistance;
		public double averageDecreaseInParkingWalkingDistance;

	}

}
