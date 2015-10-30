/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.counts;

import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

/**
 * @author johannes
 *
 */
public class CountsCompare {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Counts<Link> countsOld = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(countsOld);
		reader.parse("/home/johannes/gsv/counts/counts.2009.net20140909.5.24h.xml");
		
		Counts<Link> countsNew = new Counts();
		reader = new CountsReaderMatsimV1(countsNew);
		reader.parse("/home/johannes/gsv/counts/counts.2013.net20140909.5.24h.xml");

		double errsumAbs = 0;
		double errsum = 0;
		double cnt = 0;
		for(Count countOld : countsOld.getCounts().values()) {
			Count countNew = countsNew.getCount(countOld.getLocId());
			if(countNew != null) {
				double err = (countNew.getVolume(1).getValue() - countOld.getVolume(1).getValue()) / countOld.getVolume(1).getValue();
				errsumAbs += Math.abs(err);
				errsum += err;
				cnt++;
			}
		}
		
		System.out.println("Average error = " + errsum/cnt);
	}

}
