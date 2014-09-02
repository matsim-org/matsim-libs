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

package playground.mmoyo.utils.counts;

import java.io.File;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.mmoyo.utils.DataLoader;

public class RemoveCountsWValueZero {

	public static void main(String[] args) {
		String contsFile ="../../";
		Counts counts = new DataLoader().readCounts(contsFile);
		
		//get number of counts with values zero in all time bins 
		int countValZeroNum=0;
		Set<Id> valuesZero = new HashSet<Id>();
		
		for(Entry<Id<Link>, Count> entry: counts.getCounts().entrySet() ){
				Id countId = entry.getKey();
				Count count = entry.getValue();
				
			boolean hasZero=true;
			for (int h=1;h<25;h++){
				hasZero =	(count.getVolume(h).getValue()==0.0) && hasZero;
			}
			if (hasZero)	{
				countValZeroNum++;
				valuesZero.add(countId);
			}
		}
		
		//remove counts vith values zero
		for (Id id : valuesZero){
			counts.getCounts().remove(id);
		}
		
		File file =new File(contsFile);
		String outFile = file.getParentFile().getPath() + File.separatorChar + file.getName() +"WOzeroValues.xml" ;
		new CountsWriter(counts).write(outFile);
		System.out.println();
	}

}
