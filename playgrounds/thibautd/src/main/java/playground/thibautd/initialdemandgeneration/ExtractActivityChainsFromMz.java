/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractActivityChainsFromMz2000.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;

import playground.thibautd.initialdemandgeneration.activitychainsextractor.MzActivityChainsExtractor;
import playground.thibautd.utils.MoreIOUtils;

/**
 * executable class to extract activity chains from MZ2000
 * @author thibautd
 */
public class ExtractActivityChainsFromMz {
	/**
	 * usage: ExtractActivityChainsFromMz2000 zpFile=* wgFile=* etFile=* outDir=* startDay=* endDay=* year=*
	 */
	public static void main(final String[] args) {
		// default values, used if nothing given
		String startDay = "1";
		String endDay = "7";

		String year = null;
		String zpFile = null;
		String wgFile = null;
		String etFile = null;
		String outDir = null;

		for (String arg : args) {
			String[] keyValue = arg.split("=");

			if (keyValue[ 0 ].equals("zpFile")) {
				zpFile = keyValue[ 1 ];
			}
			if (keyValue[ 0 ].equals("wgFile")) {
				wgFile = keyValue[ 1 ];
			}
			if (keyValue[ 0 ].equals("etFile")) {
				etFile = keyValue[ 1 ];
			}
			if (keyValue[ 0 ].equals("outDir")) {
				outDir = keyValue[ 1 ];
			}
			if (keyValue[ 0 ].equals("startDay")) {
				startDay = keyValue[ 1 ];
			}
			if (keyValue[ 0 ].equals("endDay")) {
				endDay = keyValue[ 1 ];
			}
			if (keyValue[ 0 ].equals("year")) {
				year = keyValue[ 1 ];
			}
		}

		MoreIOUtils.initOut( outDir );
		MzActivityChainsExtractor extractor = new MzActivityChainsExtractor();

		Scenario scen; 
		if (year.equals( "2000" )) {
			scen = extractor.run2000(
					zpFile,
					wgFile,
					etFile,
					startDay,
					endDay);
		}
		else if (year.equals( "1994" )) {
			scen = extractor.run1994(
					zpFile,
					wgFile,
					startDay,
					endDay);
		}
		else {
			throw new IllegalArgumentException( "year "+year );
		}

		(new PopulationWriter(scen.getPopulation(),
							  scen.getNetwork())).write(
						  outDir + "/actchains-dow-"+startDay+"-"+endDay+"."+year+".xml.gz" );
	}

}

