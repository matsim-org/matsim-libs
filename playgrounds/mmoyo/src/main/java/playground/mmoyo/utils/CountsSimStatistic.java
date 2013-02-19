/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import playground.mmoyo.analysis.counts.reader.CountsReader;
import java.util.Map;
import java.util.TreeMap;

public class CountsSimStatistic {

	public static void main(String[] args) {
		String cntCmpareOccupFilePath = "../../input/New Folder/OldR30hrSimulated_cadytsSimCountCompareOccupancy.txt";
		CountsReader countReader1 = new CountsReader(cntCmpareOccupFilePath);;

		String tab = "\t";
		Map <Double, Integer> map = new TreeMap <Double, Integer>();
		
		for (Id id: countReader1.getStopsIds()){
			Double simScaled = new Double(countReader1.getSimulatedScaled(id)[0]); //first and only time bin size for 24hrs
			if(!map.keySet().contains(simScaled)){
				map.put(simScaled, 1);
			}
			int newValue = map.get(simScaled)+1;
			map.put(simScaled, newValue);
		}

		for(Map.Entry <Double, Integer> entry: map.entrySet() ){
			System.out.println(entry.getKey() + tab + entry.getValue()) ;
		}	
	}
}