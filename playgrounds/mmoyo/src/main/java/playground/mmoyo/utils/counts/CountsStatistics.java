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

import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import playground.mmoyo.utils.DataLoader;
/**
 * shows the number of counts in a counts file and the number of them with values zero 
 */
public class CountsStatistics {
	
	public static void main(String[] args) {
		String contsFile ="../../input/newDemand/ptLinecountsScenario/counts/bvg.run189.10pct.100.ptLineCounts.txtaggregatedCountsFiltered4OldSchedule.xmlWOzeroValues.xml";
		Counts counts = new DataLoader().readCounts(contsFile);
		
		//get number of counts with values zero in all time bins 
		int countValZeroNum=0;
	
		for(Count count: counts.getCounts().values() ){
			boolean hasZero=true;
			for (int h=1;h<25;h++){
				hasZero =	(count.getVolume(h).getValue()==0.0) && hasZero;
			}
			if (hasZero)	{
				countValZeroNum++;
			}
		}
		
		System.out.println("size: " + counts.getCounts().size());
		System.out.println("counts with value zero: " +countValZeroNum);
	}

}
