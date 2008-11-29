package playground.mfeil;

public class DistanceCoefficients {
	
	private double primActsDistance;
	private double homeLocationDistance;
	
	public DistanceCoefficients (double primActsDistance, double homeLocationDistance){
		this.primActsDistance = primActsDistance;
		this.homeLocationDistance = homeLocationDistance;
	}
	
	public double getPrimActsDistance (){
		return this.primActsDistance;
	}
	
	public double gethomeLocationDistance (){
		return this.homeLocationDistance;
	}
	
	public void setPrimActsDistance (double a){
		this.primActsDistance = a;
	}
	
	public void sethomeLocationDistance (double a){
		this.homeLocationDistance = a;
	}
}
