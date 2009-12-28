package playground.wrashid.PHEV.Utility;

//TODO: write tests for this class

public class AverageSpeedEnergyConsumption implements Comparable<AverageSpeedEnergyConsumption> {
	private double speed=0; // in [m/s]
	private double energyConsumption=0; // consumed energy in [J] (by driving one meter with the given speed)
	
	public AverageSpeedEnergyConsumption(double speed, double energyConsumption){
		this.speed=speed;
		this.energyConsumption=energyConsumption;
	}
	
	public double getSpeedDifference(double otherSpeed){
		return Math.abs(speed-otherSpeed);
	}
	
	public double getEnergyConsumption(){
		return energyConsumption;
	}
	
	public double getSpeed(){
		return speed;
	}

	public int compareTo(AverageSpeedEnergyConsumption otherConsumption) {
		if (this.speed<otherConsumption.speed){
			return -1;
		} else if (this.speed>otherConsumption.speed){
			return 1;
		}
		return 0;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public void setEnergyConsumption(double energyConsumption) {
		this.energyConsumption = energyConsumption;
	}
}
