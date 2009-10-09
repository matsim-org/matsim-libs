package playground.mmoyo.PTRouter;

/**
 * This class contains variables and constant values for pedestrian routing
 */

public class PTValues {
	/*
	public final static String STANDARD = "Standard";
	public final static String TRANSFER = "Transfer";
	public final static String DETTRANSFER = "DetTransfer";
	public final static String ACCESS = "Access";
	public final static String EGRESS = "Egress";
	*/
	public double timeCoeficient=0;
	public double distanceCoeficient=0;
	
	private final static double AV_WALKING_SPEED = 0.836;  //   [Al-07]M. Al-Azzawi and R.Raeside. Modeling Pedestrian Walking Speeds on Sidewalks. Journal of Urban Planning and Development. ASCE. Sept. 2007.    or    1.34 m/s [Antonini2004]??
	
	public static double getAvgWalkSpeed(){
		return AV_WALKING_SPEED;
	}
	
	public double walkTravelTime(final double distance){
		return distance * AV_WALKING_SPEED;
	}
	
	public int firstWalkRange(){
	//public int distToWalk(final int personAge){
		//-> complete personalized values.
		return 600;
	}
	
	public double walkSpeed(byte age, double time, double lenght){
		double speed=AV_WALKING_SPEED;
		//-> complete values according to Weidmann 1993 
		return speed;
	}

	public double getTimeCoeficient() {
		return timeCoeficient;
	}

	public void setTimeCoeficient(double timeCoeficient) {
		this.timeCoeficient = timeCoeficient;
	}

	public double getDistanceCoeficient() {
		return distanceCoeficient;
	}

	public void setDistanceCoeficient(double distanceCoeficient) {
		this.distanceCoeficient = distanceCoeficient;
	}	
	
}
