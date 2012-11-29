package org.matsim.contrib.freight.vrp.basics;

import java.util.HashMap;
import java.util.Map;


public class InsertionData {
	
	public static class NoInsertionFound extends InsertionData{

		public NoInsertionFound() {
			super(Double.MAX_VALUE, null);
		}

	}
	
	private final double insertionCost;
	
	private final int[] insertionIndeces;
	
	private Vehicle selectedVehicle;
	
	private Driver selectedDriver;
	
	private Map<String,Object> additionalInformation = new HashMap<String, Object>();
	
	public Map<String, Object> getAdditionalInformation() {
		return additionalInformation;
	}

	public InsertionData(double insertionCost, int[] insertionIndeces){
		this.insertionCost = insertionCost;
		this.insertionIndeces = insertionIndeces;
	}
	
	public static InsertionData createNoInsertionFound(){
		return new NoInsertionFound();
	}
	
	public int[] getInsertionIndeces(){
		return insertionIndeces;
	}
	
	public double getInsertionCost() {
		return insertionCost;
	}

	public void setVehicle(Vehicle bestVehicle) {
		selectedVehicle = bestVehicle;
	}

	public Vehicle getSelectedVehicle() {
		return selectedVehicle;
	}
	
	
	
}