package playground.wrashid.parkingSearch.planLevel.analysis;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.GeneralLib;

public class ParkingWalkingTimesAnalysis {

	private final HashMap<Id, Double> parkingWalkingTime;

	public ParkingWalkingTimesAnalysis(HashMap<Id, Double> parkingWalkingTime) {
		super();
		this.parkingWalkingTime = parkingWalkingTime;
	}

	public void writeTxtFile(String filePath) {
		double[][] matrix = getMatrix();
		String headerLine = "personId" + "\t" + "averageWalkingDistancePerWalkingLeg" + "\t";

		GeneralLib.writeMatrix(matrix, filePath, headerLine);
	}

	private double[][] getMatrix() {
		int numberOfPersonsUsingCar=parkingWalkingTime.size();
		double[][] matrix = new double[numberOfPersonsUsingCar][2];
		
		int i=0;
		for (Id personId:parkingWalkingTime.keySet()) {
			matrix[i][0] = Integer.parseInt(personId.toString());
			matrix[i][1] = parkingWalkingTime.get(personId);
			i++;
		}
		
		return matrix;
	}
	
}
