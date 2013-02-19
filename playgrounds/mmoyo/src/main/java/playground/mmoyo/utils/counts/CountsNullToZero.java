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

import org.matsim.api.core.v01.Id;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import playground.mmoyo.utils.DataLoader;

/**
 * creates 24 volumes per count, if they do not exist, value zero is asigned
 */
public class CountsNullToZero {

	public static void main(String[] args) {
		String contsFile = "../../";
		Counts counts = new DataLoader().readCounts(contsFile);

		for (Id id : counts.getCounts().keySet()){
			Count count = counts.getCount(id);
			for (int h=1; h<=24; h++){
				if (count.getVolume(h)==null){
					count.createVolume(h, 0.0);
				}	
			}
		}
		
	
		new CountsWriter(counts).write(contsFile + "COMPLETED.xml.gz");
		
	}

}
