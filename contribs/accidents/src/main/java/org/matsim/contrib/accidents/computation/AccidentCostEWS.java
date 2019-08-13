package org.matsim.contrib.accidents.computation;

import java.util.ArrayList;

/**
* @author mmayobre
*/

public class AccidentCostEWS {
//Gives the expected Costs Rates depending on Road Type back --> to combine with DenmarkModel --> NOT IN USE
	
	public static double computeAccidentCostEWS (ArrayList<Integer> roadTypeEWS){
		// accidentCostRate in 1000€ pro Unfall
		double costRateTableEWS [][][] = {
			//Vorfahrtberechtigte Innerortsstraße ohne Behinderung, plangleich: 1. Ziffer --> 1
			{
				{ 10.15 , 12.47 }, // 1 Fahrstreifen pro Richtung, a) mit Mittelstreifen oder Mittelinseln in kurzen Abständen, b) ohne Mittelstreifen (anbaufrei/angebaut)
				{ 10.53 , 10.46 }, // 2 Fahrstreifen pro Richtung; a) Mit Mittelstreifen, b) ohne Mittelstreifen
				{ 10.53 , 10.46 }, // 3 Fahrstreifen pro Richtung; a) Mit Mittelstreifen, b) ohne Mittelstreifen
				{ 10.53 , 10.46 }  // 4 Fahrstreifen pro Richtung; a) Mit Mittelstreifen, b) ohne Mittelstreifen
			},
			//Vorfahrtberechtigte Innerortsstraße mit Behinderung (durch Knotenpuntkeinflüsse, ruhenden Verkehr, ÖV), plangleich: 1. Ziffer --> 2
			{
				{ 10.36 , 12.22 , 11.32 , 10.85 }, // 1 Fahrstreifen pro Richtung; a) mit Mittelstreifen oder Mittelinseln in kurzen Abständen, b) offene mehrgeschossige Bebauung, c) gesclossene Bebauung, d) Geschäftsstraße
				{ 12.47 , 10.46 }, // 2 Fahrstreifen pro Richtung; a) Mit Mittelstreifen, b) ohne Mittelstreifen
				{ 12.47 , 10.46 }, // 3 Fahrstreifen pro Richtung; a) Mit Mittelstreifen, b) ohne Mittelstreifen
				{ 12.47 , 10.46 }  // 4 Fahrstreifen pro Richtung; a) Mit Mittelstreifen, b) ohne Mittelstreifen
			},
			//Innerortsstraße mit Behinderung durch fehlende Vorfahrt, ruhenden Verkehr (Erschließungstraßen): 1. Ziffer --> 3
			{
				{ 6.11 , 7.70 }, // offene Bebauung; mit oder ohne bauliche Geschwindigkeitsbegrenzung
				{ 5.64 , 6.80 } // geschlossene Bebauung; mit oder ohne bauliche Geschwindigkeitsbegrenzung
			}
		};
			
		//Parameter accidentCostRate 
			
		double accidentCostEWS = costRateTableEWS[roadTypeEWS.get(0)-1][roadTypeEWS.get(2)-1][roadTypeEWS.get(1)-1]; 
		return accidentCostEWS;
		
	}
}
