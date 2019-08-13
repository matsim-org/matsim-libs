package org.matsim.contrib.accidents.computation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.data.berlin.PlanfreeLinkIDs;
import org.matsim.contrib.accidents.data.berlin.TunnelLinkIDs;

public class ModelTest {
	
	public static void main (String args[]){
	/*	//BVWP
		ArrayList<Integer> list = new ArrayList<>();
		list.add(0, 0); // Plangleich
		int numberOfActivities = 3;
		double freeSpeed = 17.;
		if (numberOfActivities > 1) { //probably built-up area
			if (freeSpeed > 16.) {
				//probably an Express-Highway
				list.add(1, 1);
			} else list.add(1, 3); // probably not an Express-Highway
		} else { //probably not built-up area
			if (freeSpeed > 16.) {
				//probably an Express-Highway
				list.add(1, 0);
			} else list.add(1, 2); // probably not an Express-Highway
		}
		
		int numberOfLanesBVWP = 4;
		list.add(2, numberOfLanesBVWP);
		
		System.out.println(AccidentCostComputationBVWP.computeAccidentCosts(1000,1000, list));
		//AccidentCostComputationBVWP funktioniert soweit
		*/
		ArrayList<Integer> list1 = new ArrayList<>();
		
		//Plan equal, Plan free or Tunnel?
		list1.add(0, 1); // Default: Plan equal
		list1.add(1, 1);
		list1.add(2, 7);
		//System.out.println(list1.size());
		System.out.println(String.valueOf(list1));
		
		String[] planfreeLinkIDs = PlanfreeLinkIDs.getPlanfreeLinkIDs();
		for(int j=0; j < planfreeLinkIDs.length; j++){
		    if(planfreeLinkIDs[j] == "52761861_259225122"){
		    	list1.set(0, 0); // Change to Plan free
		    	System.out.println("Changed to Plan free!");
		    	break;
		    }
		}
		//System.out.println(list1.size());
		System.out.println(String.valueOf(list1));
		
		String[] tunnelLinkIDs = TunnelLinkIDs.getTunnelLinkIDs();
		for(int i=0; i < tunnelLinkIDs.length; i++){
			if(tunnelLinkIDs[i] == "52761861_259225122"){
				list1.set(0, 2); // Change to Tunnel
				System.out.println("Changed to Tunnel");
				break;
			}
		}
		//System.out.println(list1.size());
		System.out.println(String.valueOf(list1));
		//list1.add(1, 1);
		//list1.add(2, 7);
		
		
		list1.set(1, 5);
		String probe2 = String.valueOf(list1);
		System.out.println(probe2);
		
		
//		String landUseTypeBB = "ret";
//		if (landUseTypeBB.matches("commercial|industrial|recreation_ground|residential|retail")) {
//			System.out.println("Wort gefunden!");
//		} else System.out.println("Wort nicht gefunden!");
//		
//		double timeBinSize = 15 * 60;
//		double numberOfTimeBinsPerDay = (24 * 3600) / timeBinSize;
//		double actualNumberOfTimeBins = (30 * 3600) / timeBinSize;
//		
//		double differenceOfTimeBins = actualNumberOfTimeBins - numberOfTimeBinsPerDay;
//		
//		System.out.println((int) differenceOfTimeBins);
//		
//		for (double endTime = timeBinSize ; endTime <= 30 * 3600; endTime = endTime + timeBinSize ) {
//			
//			double time = (endTime - timeBinSize/2.);
//			int timeBinNr = (int) (time / timeBinSize);
//			System.out.println(timeBinNr);
//			
//		}
/*				
		double accidentFrequencyProbe = AccidentFrequencyComputation.computeAccidentFrequency(1000, 50, 9, 0, ParkingType.Rarely, AccidentAreaType.ScatteredHousing);
		System.out.println(accidentFrequencyProbe);
		//AccidentFrequencyComputation funktioniert soweit
		
		double accidentCostPerAccidentProbe = AccidentCost30vs50.giveAccidentCostDependingOnActualSpeed(10);
		System.out.println(accidentCostPerAccidentProbe);
		//AccidentCost30vs50 funktioniert soweit
		
		double accidentCostProbe = accidentFrequencyProbe * accidentCostPerAccidentProbe;
		System.out.println("The calculated accident costs for this road are: " + accidentCostProbe + " €.");
	*/}

}
