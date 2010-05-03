/* *********************************************************************** *
 * project: org.matsim.*
 * LaneLayoutTestConvertTo20
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.lanes.laneLayoutTest;

import org.matsim.lanes.LaneDefinitonsV11ToV20Converter;


/**
 * @author dgrether
 *
 */
public class LaneLayoutTestConvertTo20 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		new LaneDefinitonsV11ToV20Converter().convert(LaneLayoutTestFileNames.LANEDEFINITIONS, 
				LaneLayoutTestFileNames.LANEDEFINITIONSV2, LaneLayoutTestFileNames.NETWORK);
	}

}
