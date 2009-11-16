package playground.wrashid.tryouts.emptyRoute;

import java.util.Random;

public class Main {

	static int numberOfFacilities=10000;
	static Random rand=new Random();
	static double linkLength=1000; // in meters
	static int numberOfLinks=10;
	
	/**
	 * Experiment are related to 
	 * https://wiki.ivt.ethz.ch/mediawiki/index.php/Comparison_of_MobSim_and_JDEQSim
	 * and
	 * C:\data\Projekte\MatSim\emptyCarRoute\experiment.xlsx
	 * 
	 * But the experiment could/should be extended to make the right statement.
	 * 
	 * - Create any number of links in the scenario.
	 * - oldJDEQSim references to the case, when in case of empty car route, the first link was simulated.
	 * - source and target links are chosen randomly from the given links available in the scenario.
	 * - averageMobSimDistance, averageEuclideanDistance and averageOldJDEQSimDistance are measured.
	 * 
	 */
	public static void main(String[] args) {
		
		double facilityXCoordinates[]= new double[numberOfFacilities];
		
		
		
		// assign coordinates to the facilities
		for (int i=0;i<numberOfFacilities;i++){
			facilityXCoordinates[i]=rand.nextDouble() *numberOfLinks*linkLength;
		}
		
		int numberOfExperiments=10000;
		double averageMobSimDistance=0;
		double averageEuclideanDistance=0;
		double averageOldJDEQSimDistance=0;
		
		// do measurements
		for (int i=0;i<numberOfExperiments;i++){
			int indexStartFacility=getIndexOfRandomFacility();
			int indexTargetFacility=getIndexOfRandomFacility();
			
			averageMobSimDistance+=getMobSimDistance(facilityXCoordinates[indexStartFacility], facilityXCoordinates[indexTargetFacility]);			
			averageEuclideanDistance+=getEuclideanDistance(facilityXCoordinates[indexStartFacility], facilityXCoordinates[indexTargetFacility]);
			averageOldJDEQSimDistance+=getOldJDEQSimDistance(facilityXCoordinates[indexStartFacility], facilityXCoordinates[indexTargetFacility]);
		}
		
		averageMobSimDistance/=numberOfExperiments;
		averageEuclideanDistance/=numberOfExperiments;
		averageOldJDEQSimDistance/=numberOfExperiments;
		
		System.out.println("averageMobSimDistance: " + averageMobSimDistance);
		System.out.println("averageEuclideanDistance: " + averageEuclideanDistance);
		System.out.println("averageOldJDEQSimDistance: " + averageOldJDEQSimDistance);
	}
	
	private static int getIndexOfRandomFacilityOnRoadOne(double facilityXCoordinates[]){
		int index=0;
		do{
			index=rand.nextInt(numberOfFacilities);
		} while (facilityXCoordinates[index]>1000);
		return index;
	}
	
	// on any of the two roads
	private static int getIndexOfRandomFacility(){
		return rand.nextInt(numberOfFacilities);
	}
	
	private static double getEuclideanDistance(double startFacilityXCoordinate, double targetFacilityXCoordinate){
		return Math.abs(startFacilityXCoordinate-targetFacilityXCoordinate);
	}
	
	private static double getMobSimDistance(double startFacilityXCoordinate, double targetFacilityXCoordinate){
		return Math.abs((Math.floor(startFacilityXCoordinate/linkLength) - Math.floor(targetFacilityXCoordinate/linkLength)))*linkLength;
	}
	
	private static double getOldJDEQSimDistance(double startFacilityXCoordinate, double targetFacilityXCoordinate){
		double distance=getMobSimDistance(startFacilityXCoordinate,targetFacilityXCoordinate);
		if (distance==0.0){
			return 1.0*linkLength;
		} else {
			return distance;
		}
	}

	

}
