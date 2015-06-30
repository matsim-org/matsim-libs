/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigV1toV2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ConfigReader;

/**
 * @author thibautd
 */
public class ConfigV1toV2 {
	public static void main(final String[] args) {
		final String in = args[ 0 ];
		final String out = args[ 1 ];

		final Config config = ConfigUtils.createConfig();
		new ConfigReader( config ).readFile( in );
		new ConfigWriter( config ).write( out );
	}
}

