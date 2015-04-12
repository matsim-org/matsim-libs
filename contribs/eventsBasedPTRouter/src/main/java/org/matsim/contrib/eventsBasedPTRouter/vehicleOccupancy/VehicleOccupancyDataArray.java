package org.matsim.contrib.eventsBasedPTRouter.vehicleOccupancy;

public class VehicleOccupancyDataArray implements VehicleOccupancyData {

	//Attributes
	private double[] vehicleOccupancies;
	private int[] numTimes;
	private int lastPosition;

	//Constructors
	public VehicleOccupancyDataArray(int numSlots) {
		vehicleOccupancies = new double[numSlots];
		numTimes = new int[numSlots];
		resetVehicleOccupancies();
	}

	//Methods
	@Override
	public double getVehicleOccupancy(int timeSlot) {
		if(timeSlot>lastPosition)
			return 1;
		else
			while(vehicleOccupancies[timeSlot]==-1)
				timeSlot++;
		return vehicleOccupancies[timeSlot];
	}
	@Override
	public synchronized void addVehicleOccupancy(int timeSlot, double vehicleOccupancy) {
		if(lastPosition<timeSlot)
			lastPosition = timeSlot;
		vehicleOccupancies[timeSlot] = ((vehicleOccupancies[timeSlot]!=-1?vehicleOccupancies[timeSlot]*numTimes[timeSlot]:0)+vehicleOccupancy)/++numTimes[timeSlot];		
	}
	@Override
	public void resetVehicleOccupancies() {
		for(int i=0; i<vehicleOccupancies.length; i++) {
			vehicleOccupancies[i] = -1;
			numTimes[i] = 0;
		}
		lastPosition = -1;
	}

}
