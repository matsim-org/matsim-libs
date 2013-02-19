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

package playground.mmoyo.analysis.tools;

import org.matsim.core.utils.misc.Time;

/**Finds valid time bin sizes for Cadyts.
** Cadyts accepts only time bin sizes that are can divide the day length (86400)
*/
public class TimeBinSize_Validator {

	public static void main(String[] args) {
		final String STR_BIN_SIZE = "binSize: ";
		final String STR_SPACE = " ";
		for (int binSize=86400; binSize>0; binSize--){
			if ( (86400 % binSize)== 0 ){
				System.out.println (STR_BIN_SIZE + binSize + STR_SPACE + Time.writeTime(binSize));
			}
		}
	}

}
