package playground.ikaddoura.analysis.distance;

public class Line {

	private double distanceGroup;
	private int carLegs;
	private int bikeLegs;
	private int walkLegs;
	private int ptLegs;
	private int rideLegs;
	private int undefinedLegs;
	
	
	public Line(Double distanceGroup) {
		this.setDistanceGroup(distanceGroup);
	}

	public void setLegs(String modeName, int legs) {

		if (modeName.equals("car")){
			this.setCarLegs(legs);
		}

		if (modeName.equals("bike")){
			this.setBikeLegs(legs);
		}
		
		if (modeName.equals("pt")){
			this.setPtLegs(legs);
		}
		
		if (modeName.equals("walk")){
			this.setWalkLegs(legs);
		}
		
		if (modeName.equals("ride")){
			this.setRideLegs(legs);
		}
		
		if (modeName.equals("undefined")){
			this.setUndefinedLegs(legs);
		}
	}
	
	public int getCarLegs() {
		return carLegs;
	}

	public void setCarLegs(int carLegs) {
		this.carLegs = carLegs;
	}

	public int getBikeLegs() {
		return bikeLegs;
	}

	public void setBikeLegs(int bikeLegs) {
		this.bikeLegs = bikeLegs;
	}

	public int getWalkLegs() {
		return walkLegs;
	}

	public void setWalkLegs(int walkLegs) {
		this.walkLegs = walkLegs;
	}

	public int getPtLegs() {
		return ptLegs;
	}

	public void setPtLegs(int ptLegs) {
		this.ptLegs = ptLegs;
	}

	public int getRideLegs() {
		return rideLegs;
	}

	public void setRideLegs(int rideLegs) {
		this.rideLegs = rideLegs;
	}

	public void setUndefinedLegs(int undefined) {
		this.undefinedLegs = undefined;
	}

	public int getUndefinedLegs() {
		return undefinedLegs;
	}

	public void setDistanceGroup(double distanceGroup) {
		this.distanceGroup = distanceGroup;
	}

	public double getDistanceGroup() {
		return distanceGroup;
	}


}
