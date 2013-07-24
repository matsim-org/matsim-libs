/* *********************************************************************** *
 * project: org.matsim.*
 * CreateCounts.java
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
package playground.thibautd.initialdemandgeneration.old.counts;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;

import playground.thibautd.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class CreateCounts {
	private static final Logger log =
		Logger.getLogger(CreateCounts.class);

	public static void main(final String[] args) {
		//Config config = ConfigUtils.createConfig();
		Config config = new Config();
		CountsConfigGroup configGroup = new CountsConfigGroup();
		config.addModule( configGroup );
		ConfigUtils.loadConfig( config , args[0] );

		MoreIOUtils.initOut( configGroup.getOutputDir() );
		
		Gbl.startMeasurement();
		CountsCreator creator =
			new CountsCreator( configGroup );
		creator.run();
				
		Gbl.printElapsedTime();
		log.info("finished #########################################################################");
	}
	
}

