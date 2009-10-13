package playground.mmoyo.PTRouter;

/**
 * This class contains common constant values for routing
 */
public class PTValues {
	
	public final static byte ACCESS 	= 1;
	public final static byte STANDARD 	= 2;
	public final static byte TRANSFER 	= 3;
	public final static byte DETTRANSFER= 4;
	public final static byte EGRESS 	= 5;
	
	public double timeCoeficient=0;
	public double distanceCoeficient=0;
	
	public final double AV_WALKING_SPEED = 0.836;  	//[Al-07]M. Al-Azzawi and R.Raeside. Modeling Pedestrian Walking Speeds on Sidewalks. Journal of Urban Planning and Development. ASCE. Sept. 2007.    or    1.34 m/s [Antonini2004]??
	public final double DETTRANSFER_RANGE = 300;	//distance to search station to build det transfer links
	public final double FIRST_WALKRANGE = 600;  	//initial distance for station search 
	public final double WALKRANGE_EXT = 300;   		//progressive extension distance of the station search
	public final int INI_STATIONS_NUM = 2;			//number of stations to find in order to start the route search

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
