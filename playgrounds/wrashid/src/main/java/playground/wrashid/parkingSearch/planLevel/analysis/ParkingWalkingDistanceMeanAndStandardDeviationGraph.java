package playground.wrashid.parkingSearch.planLevel.analysis;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.Collections;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

public class ParkingWalkingDistanceMeanAndStandardDeviationGraph {

	LinkedList<StatisticalValues> statisticalValues=new LinkedList<ParkingWalkingDistanceMeanAndStandardDeviationGraph.StatisticalValues>();
	
	public void updateStatisticsForIteration(int iternationNumber, HashMap<Id, Double> parkingWalkingTime) {
		if (statisticalValues.size()!=iternationNumber){
			throw new Error("the iteration number is wrong!!!!");
		}
		
		double[] valueArray=Collections.convertDoubleCollectionToArray(parkingWalkingTime.values());
		
		StatisticalValues iterationStatValues=new StatisticalValues();
		iterationStatValues.mean=new Mean().evaluate(valueArray);
		iterationStatValues.standardDeviation=new StandardDeviation().evaluate(valueArray);

		statisticalValues.add(iterationStatValues);
	}
	
	
	public void writeGraphic(String fileName){
		String xLabel = "Iteration";
		String yLabel = "parking walking distance";
		String title="Parking Walking Distance";
		int numberOfXValues = statisticalValues.size();
		int numberOfFunctions = 3;
		double[] xValues=new double[numberOfXValues];
		String[] seriesLabels=new String[numberOfFunctions];
		
		seriesLabels[0]="mean";
		seriesLabels[1]="mean+stdDev";
		seriesLabels[2]="mean-stdDev";
		
		double[][] matrix=new double[numberOfXValues][numberOfFunctions];
		
		for (int i=0;i<numberOfXValues;i++){
			matrix[i][0]=statisticalValues.get(i).mean;
			matrix[i][1]=statisticalValues.get(i).mean+statisticalValues.get(i).standardDeviation;
			matrix[i][2]=statisticalValues.get(i).mean-statisticalValues.get(i).standardDeviation;
			xValues[i]=i;
		}

		GeneralLib.writeGraphic(fileName, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}


	private class StatisticalValues {

		public double mean;
		public double standardDeviation;

	}

}
