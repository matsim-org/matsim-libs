package org.matsim.contrib.accidents.computation;

import java.util.ArrayList;

import org.jfree.util.Log;
import org.matsim.api.core.v01.network.Link;

/**
* @author mmayobre
*/

public class AccidentCostComputationBVWP {
//Computes the expected Accident Costs depending on the expected vehicle-km an the Cost Rate of each Road Type

	public static double computeAccidentCosts(double demand, Link link, ArrayList<Integer> roadType){
		//costRateTable in €/T.vehicle-km
		
		// TODO: Check if this is 100% equal to Abb. 13 BVWP Methodenhandbuch 2030
		// it is equal but simplified (Hugo)
		// TODO: runtime exception instead of 0
		// Since in this class the link ID can not be getted I would rather throw the exeption or warning in the class AccidentControlerListener notifying the ID of the link where the value would be 0 (Hugo)
		// TO THINK ABOUT: doing an alternative Method in case all the data is available (Hugo)
		
		double costRateTable[][][] = {
			/*	2. Ziffer
			 * 		1.Spalte: Außerhalb von bebauten Gebiet, Kfz-Straße
			 * 		2.Spalte: Innerhalb von bebauten Gebiet, Kfz-Straße
			 * 		3.Spalte: Außerhalb von bebauten Gebiet
			 * 		4.Spalte: Innerhalb von bebauten Gebiet
			 * 
			 * 	3. Ziffer: Anzahl an Fahrstreifen
			 */
			//Planfrei: 1.Ziffer --> 0
			{
				{ 0      , 0      , 0      , 0      }, // 1 Fahrstreifen pro Richtung
				{ 23.165 , 23.165 , 0      , 0      }, // 2 Fahrstreifen pro Richtung
				{ 23.79  , 23.79  , 0      , 0      }, // 3 Fahrstreifen pro Richtung
				{ 23.79  , 23.79  , 0      , 0      }  // 4 Fahrstreifen pro Richtung
			},
			//Plangleich: 1.Ziffer --> 1
			{
				{ 61.785 , 101.2  , 61.785 , 101.2  }, // 1 Fahrstreifen pro Richtung
				{ 31.63  , 101.53 , 31.63  , 101.53 }, // 2 Fahrstreifen pro Richtung
				{ 37.84  , 82.62  , 34.735 , 101.53 }, // 3 Fahrstreifen pro Richtung
				{ 0      , 0      , 0      , 101.53 }  // 4 Fahrstreifen pro Richtung
			},
			//Tunnelstrecke: 1.Ziffer --> 2
			{
				{ 9.56   , 15.09    , 9.56  , 15.09 }, // 1 Fahrstreifen pro Richtung
				{ 11.735 , 14.67425 , 0     , 17.57 }, // 2 Fahrstreifen pro Richtung
				{ 11.735 , 11.735   , 0     , 17.57 }, // 3 Fahrstreifen pro Richtung
				{ 9.11   , 9.11     , 0     , 17.57 }  // 4 Fahrstreifen pro Richtung
			}		
		};
		
		//Parameter costRate
		double costRate = costRateTable[roadType.get(0)][roadType.get(2)-1][roadType.get(1)]; 
		if (costRate == 0) {
			Log.error("Accident cost rate not specified link " + link.getId().toString() + " , roadtype: "+ roadType.toString() );
		}
		
		double vehicleKm = demand * (link.getLength() / 1000.); // length is converted from METER to KILOMETER
		
		double accidentCosts = costRate * (vehicleKm / 1000.); // vehicleKM --> T.vehicleKM
		
		return accidentCosts; // in €
		//return costRate;
		
	}
	
}
