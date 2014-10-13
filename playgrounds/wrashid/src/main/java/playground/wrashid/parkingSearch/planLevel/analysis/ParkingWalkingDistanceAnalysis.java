package playground.wrashid.parkingSearch.planLevel.analysis;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingWalkingDistanceAnalysis {

	private final HashMap<Id<Person>, Double> parkingWalkingDistance;

	public ParkingWalkingDistanceAnalysis(HashMap<Id<Person>, Double> parkingWalkingTime) {
		super();
		this.parkingWalkingDistance = parkingWalkingTime;
	}

	public void writeTxtFile(String filePath) {
		double[][] matrix = getMatrix();
		String headerLine = "personId" + "\t" + "averageWalkingDistancePerWalkingLeg" + "\t";

		GeneralLib.writeMatrix(matrix, filePath, headerLine);
	}

	private double[][] getMatrix() {
		int numberOfPersonsUsingCar=parkingWalkingDistance.size();
		double[][] matrix = new double[numberOfPersonsUsingCar][2];
		
		int i=0;
		for (Id personId:parkingWalkingDistance.keySet()) {
			matrix[i][0] = Integer.parseInt(personId.toString());
			matrix[i][1] = parkingWalkingDistance.get(personId)*ParkingRoot.getParkingWalkingDistanceScalingFactorForOutput();
			i++;
		}
		
		return matrix;
	}
	
}
