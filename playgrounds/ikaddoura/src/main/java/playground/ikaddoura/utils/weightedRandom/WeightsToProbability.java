/* *********************************************************************** *
 * project: org.matsim.*
 * Test.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.ikaddoura.utils.weightedRandom;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

/**
 * @author Ihab
 *
 */
public class WeightsToProbability {

	public static void main(String[] args) {
        
       Map<Id<StrategySettings>, Double> unsortedMap = new HashMap<>();
       ValueComparator vc = new ValueComparator(unsortedMap);
       TreeMap<Id<StrategySettings>, Double> sortedMap = new TreeMap<>(vc); // id, weight
       
       unsortedMap.put(Id.create("1", StrategySettings.class), 0.2);
       unsortedMap.put(Id.create("2", StrategySettings.class), 0.1);
       unsortedMap.put(Id.create("3", StrategySettings.class), 0.7);
              
       sortedMap.putAll(unsortedMap);
       
       //****************************************

       System.out.println("unsortedMap:");
       for (Id id : unsortedMap.keySet()) {
       		System.out.println("id / value: " + id + " / " + unsortedMap.get(id));
       }
       System.out.println();
       System.out.println("sortedMap:");
       for (Id id : sortedMap.keySet()) {
           System.out.println("id / value: " + id + " / " + sortedMap.get(id));
       }
       
       //****************************************
   
   double z1 = 0.;
   double z2 = 0.;
   double z3 = 0.;
   for (int n = 0; n <= 1000; n++){
       double weightsSum = 0.0;
       for (Id id : sortedMap.keySet()){
    	   weightsSum = weightsSum + sortedMap.get(id);
       }
       System.out.println();
       System.out.println("weightsSum: " + weightsSum);
       
       Random random = new Random();
       double rnd = random.nextDouble() * weightsSum;
       System.out.println("rnd: " + rnd);
       
       Id weightedRndId = null;
       double cumulatedWeight = 0.0;
       for (Id id : sortedMap.keySet()){
    	   cumulatedWeight = cumulatedWeight + sortedMap.get(id);
    	   if (cumulatedWeight >= rnd){
    		   weightedRndId = id;
    	       System.out.println("weightedRndId: " + weightedRndId.toString());
    		   break;
    	   }
       }
       System.out.println("weightedRndId: " + weightedRndId.toString());
      
       if (weightedRndId.toString().equals("1")) {
    	   z1++;
       } else if (weightedRndId.toString().equals("2")) {
    	   z2++;
       } else if (weightedRndId.toString().equals("3")) {
    	   z3++;
       }
   }
   
   System.out.println("1: "+ (z1/(z1+z2+z3)));
   System.out.println("2: "+ (z2/(z1+z2+z3)));
   System.out.println("3: "+ (z3/(z1+z2+z3)));

  }
}
