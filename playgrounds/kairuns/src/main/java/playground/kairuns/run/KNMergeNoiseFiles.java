/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kairuns.run;

/**
 * @author nagel
 *
 */
public class KNMergeNoiseFiles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFilePath = "/Users/nagel/kairuns/a100/output/analysis_it.0/" ;
		BerlinUtils.mergeNoiseFiles(outputFilePath);
	}

}
