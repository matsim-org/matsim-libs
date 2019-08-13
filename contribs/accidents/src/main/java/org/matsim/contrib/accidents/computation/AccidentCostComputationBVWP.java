package org.matsim.contrib.accidents.computation;

import java.util.ArrayList;

/**
* @author mmayobre
*/

public class AccidentCostComputationBVWP {
//Computes the expected Accident Costs depending on the expected vehicle-km an the Cost Rate of each Road Type

	public static double computeAccidentCosts(double demand, double length, ArrayList<Integer> roadType){
		//costRateTable in €/T.vehicle-km
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
		
		double vehicleKm = demand * (length / 1000.); // length is converted from METER to KILOMETER
		
		double accidentCosts = costRate * (vehicleKm / 1000.); // vehicleKM --> T.vehicleKM
		
		return accidentCosts; // in €
		//return costRate;
		
	}
	
}
