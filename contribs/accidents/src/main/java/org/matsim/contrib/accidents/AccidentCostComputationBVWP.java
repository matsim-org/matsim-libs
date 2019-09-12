package org.matsim.contrib.accidents;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;

/**
 * 
 * Computes the accident costs depending on the vehicle-km and the cost rate given for each road type
 * 
* @author mmayobre
*/
class AccidentCostComputationBVWP {
	private static final Logger log = Logger.getLogger(AccidentCostComputationBVWP.class);

	/**
	 * 
	 * Provides the accident costs in EUR based on a simplified version of Fig. 13 in the German 'BVWP Methodenhandbuch 2030'
	 * 
	 * @param demand
	 * @param link
	 * @param roadType
	 * @return accident costs in EUR
	 */
	public static double computeAccidentCosts(double demand, Link link, ArrayList<Integer> roadType){

		// in EUR per 1000 vehicle-km
		double costRateTable[][][] = {
				
			/*	position 1
			 * 		column 0: 'Außerhalb von bebauten Gebiet, Kfz-Straße'
			 * 		column 1: 'Innerhalb von bebauten Gebiet, Kfz-Straße'
			 * 		column 2: 'Außerhalb von bebauten Gebiet'
			 * 		column 3: 'Innerhalb von bebauten Gebiet'
			 * 
			 * 	position 2: number of lanes
			 */
			
			// 'planfrei': positon 0 --> 0
			{
				{ 0      , 0      , 0      , 0      }, // 1 lane
				{ 23.165 , 23.165 , 0      , 0      }, // 2 lane
				{ 23.79  , 23.79  , 0      , 0      }, // 3 lane
				{ 23.79  , 23.79  , 0      , 0      }  // 4 lane
			},
			// 'plangleich': positon 0 --> 1
			{
				{ 61.785 , 101.2  , 61.785 , 101.2  }, // 1 lane
				{ 31.63  , 101.53 , 31.63  , 101.53 }, // 2 lane
				{ 37.84  , 82.62  , 34.735 , 101.53 }, // 3 lane
				{ 0      , 0      , 0      , 101.53 }  // 4 lane
			},
			// tunnel: positon 0 --> 2
			{
				{ 9.56   , 15.09    , 9.56  , 15.09 }, // 1 lane
				{ 11.735 , 14.67425 , 0     , 17.57 }, // 2 lane
				{ 11.735 , 11.735   , 0     , 17.57 }, // 3 lane
				{ 9.11   , 9.11     , 0     , 17.57 }  // 4 lane
			}		
		};
		
		double costRate = costRateTable[roadType.get(0)][roadType.get(2)-1][roadType.get(1)]; 
		if (costRate == 0) {
			log.warn("Accident cost rate for link " + link.getId().toString() + " is 0. (roadtype: " + roadType.toString() + ")" );
		}
		
		double vehicleKm = demand * (link.getLength() / 1000.);
		
		double accidentCosts = costRate * (vehicleKm / 1000.);
		
		return accidentCosts;		
	}
	
}
