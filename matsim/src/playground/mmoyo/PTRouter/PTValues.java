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
	
	public final double AV_WALKING_SPEED = 1/1.34;  //1.34 [Weidmann93], [Antonini2004].  0.836 by [Al-Azzawi 07]  
	public final double DETTRANSFER_RANGE = 100;	//300 original distance to search station to build det transfer links
	public final double FIRST_WALKRANGE = 600;  	//initial distance for station search 
	public final double WALKRANGE_EXT = 300;   		//progressive extension distance of the station search
	
	public final int INI_STATIONS_NUM = 2;			//number of stations to find in order to start the route search
}

/*
public double Person_WalkSpeed(final byte age, final double time, final double length){

	//-> complete values according to Weidmann 1993 and set arrays as final
	byte[] arrAges= [0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80];   
	double[] ageSpeed= [x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x];
	
	//-> validate results ou of range
	return ageSpeed[Arrays.binarySearch(arrAges, age)];
}

	public double firstWalkRange(){
	//public int distToWalk(final int personAge){
	//-> complete personalized values.
	return 600;
}*/
