package playground.wrashid.PSF.data.hubCoordinates.hubLinkMapper;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class EstimateRadiusOfHubSubMangers {

	/**
	 * We refer to "HubSubManagers" to those entries in the file, which belong to the same hub, but have different coordinates.
	 * 
	 * assumption: the file is sorted after the hubIds (this assumption is used to make the run faster).
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		StringMatrix matrix=GeneralLib.readStringMatrix("A:/data/ewz daten/GIS_coordinates_of_managers.txt");

		double sumOfMinimumDistancesOfHubSubMangers=0;
		int numberOfSamplesUsed=0;
		for (int i=0;i<matrix.getNumberOfRows()-1;i++){
			int indexNextHub=i+1;
			int currentHubId=matrix.convertDoubleToInteger(i, 0);
			int nextHubId=matrix.convertDoubleToInteger(indexNextHub, 0);
			
			if (currentHubId!=nextHubId){			
				sumOfMinimumDistancesOfHubSubMangers+=getMinimumDistanceBetweenHubSubMangersForHub(matrix, currentHubId);
				numberOfSamplesUsed++;
				// note: the last hub is not part of the calculations (to simplify the code => it is only an average)
			}
			
			
		}
		/*
		 * The intuition is, that we measure the distance between "HubSubManagers" in the same hub and thereafter
		 */
		System.out.println("averageHubSubManagers in [m]: " + (sumOfMinimumDistancesOfHubSubMangers/numberOfSamplesUsed)/2);
	}
	
	/**
	 * -1.0 means, that result should be discarded
	 * @param matrix
	 * @param targetHubId
	 * @return
	 */
	private static double getMinimumDistanceBetweenHubSubMangersForHub(StringMatrix matrix,int targetHubId){
		double minimumDistance=Double.MAX_VALUE;
		
		int minIndex=-1;
		int maxIndex=-1;
		
		for (int i=0;i<matrix.getNumberOfRows();i++){
			int currentHub=matrix.convertDoubleToInteger(i, 0);
			if (currentHub==targetHubId){
				if (minIndex==-1){
					minIndex=i;
				}
				maxIndex=i;
			}
		}
		

		if (minIndex==maxIndex){
			return -1.0;
		}
		
		// draw randomly two points from the hub and measure there distance (find minimum value probabilistically)
		
		Random random=new Random();
		for (int i=0;i<100;i++){
			int rowFirstHubSubManager=minIndex+random.nextInt(maxIndex-minIndex);
			int rowSecondHubSubManager=minIndex+random.nextInt(maxIndex-minIndex);
			
			if (rowFirstHubSubManager==rowSecondHubSubManager){
				// no distance can be calculated, if same sub manager selected
				continue;
			}
			
			Coord coordinateFirstHubSubManager=new CoordImpl(matrix.getDouble(rowFirstHubSubManager, 1),matrix.getDouble(rowFirstHubSubManager, 2));
			Coord coordinateSecondHubSubManager=new CoordImpl(matrix.getDouble(rowSecondHubSubManager, 1),matrix.getDouble(rowSecondHubSubManager, 2));
			
			if (GeneralLib.getDistance(coordinateFirstHubSubManager, coordinateSecondHubSubManager)<minimumDistance){
				minimumDistance=GeneralLib.getDistance(coordinateFirstHubSubManager, coordinateSecondHubSubManager);
			}
		}
		
		while (minimumDistance==Double.MAX_VALUE){
			// because of some very low probability always the same hub was selected, mark result as invalid 
			return -1.0;
		}
		
		return minimumDistance;
	}
	

}
