package playground.mmoyo.PTRouter;

/**
 * This class contains constant variables and constant values to pedestrian routing
 */

public class Walk {
	private final static double AV_WALKING_SPEED = 0.836;  //   0.836 m/s [Al-Azzawi2007]?      1.34 m/s [Antonini2004]?
	
	public Walk() {

	}

	public static double walkingSpeed(){
		return AV_WALKING_SPEED;
	}
	
	public double walkTravelTime(final double distance){
		return distance * AV_WALKING_SPEED;
	}
	
	public int distToWalk(final int personAge){
		// TODO [kn] Sehe das gerade. Ich bin wenig begeistert über solche ad-hoc Festlegungen von Verhaltensparametern. kai, mar09 
		int distance=0;
		if (personAge>=60)distance=300;
		if ((personAge>=40) && (personAge<60))distance=400;
		if ((personAge>=18) && (personAge<40))distance=600;
		if (personAge<18)distance=300;
		return distance;
	}
	
	public double walkSpeed(byte age, double time, double lenght){
		double speed=AV_WALKING_SPEED;
		//-> complete values according to Weidmann 1993 
		return speed;
	}

	
	
	
	
}
