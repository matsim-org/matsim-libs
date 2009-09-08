package playground.mmoyo.PTRouter;

/**
 * This class contains variables and constant values for pedestrian routing
 */

public class Walk {
	private final static double AV_WALKING_SPEED = 0.836;  //   0.836 m/s [Al-Azzawi2007]?      1.34 m/s [Antonini2004]?
	
	public Walk() {

	}

	public static double getAvgWalkSpeed(){
		return AV_WALKING_SPEED;
	}
	
	public double walkTravelTime(final double distance){
		return distance * AV_WALKING_SPEED;
	}
	
	public int distToWalk(final int personAge){
		//-> complete personalized values.
		return 600;
	}
	
	public double walkSpeed(byte age, double time, double lenght){
		double speed=AV_WALKING_SPEED;
		//-> complete values according to Weidmann 1993 
		return speed;
	}	
	
}
