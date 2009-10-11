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
	
	public final double AV_WALKING_SPEED = 0.836;  //   [Al-07]M. Al-Azzawi and R.Raeside. Modeling Pedestrian Walking Speeds on Sidewalks. Journal of Urban Planning and Development. ASCE. Sept. 2007.    or    1.34 m/s [Antonini2004]??
	public final double DETTRANSFER_RANGE = 100;    //300  values for zürich
	public final double FIRST_WALKRANGE = 1000;    //300   values for zürich
	
	public double getAV_WALKING_SPEED() {
		return AV_WALKING_SPEED;
	}


}

/*
public double walkSpeed(byte age, double time, double lenght){
	double speed=AV_WALKING_SPEED;
	//-> complete values according to Weidmann 1993 
	return speed;

	public int firstWalkRange(){
//public int distToWalk(final int personAge){
	//-> complete personalized values.
	return 600;
}*/
