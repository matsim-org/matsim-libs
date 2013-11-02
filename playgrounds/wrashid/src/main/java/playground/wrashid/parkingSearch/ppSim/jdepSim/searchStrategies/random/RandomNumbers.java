/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random;

import java.util.HashMap;
import java.util.Random;

import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class RandomNumbers {

	static TwoHashMapsConcatenated<Id, Integer, Random> randomNumbers;
	static double startTime=-1;

	public static Random getRandomNumber(Id personId,Integer legIndex,String strategyName){
		if (randomNumbers.get(personId, legIndex)==null){
			int seed=strategyName.toString().hashCode();
			if (startTime!=-1){
				seed+=startTime;
			}
			
			randomNumbers.put(personId, legIndex, new Random(seed));
		}
		return randomNumbers.get(personId, legIndex);
	}
	
	public static void reset(){
		if (ZHScenarioGlobal.loadBooleanParam("RandomNumbers.useStartTimeForRandomSeedInitialization")){
			startTime=System.currentTimeMillis();
		}
		randomNumbers=new TwoHashMapsConcatenated<Id, Integer, Random>();
	}
	
}

