/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.negotiation.locationnegotiation;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.ivt.utils.MonitoringUtils;
import playground.thibautd.negotiation.framework.NegotiatorConfigGroup;

/**
 * @author thibautd
 */
public class RunLocationNegotiation {
	public static void main( final String... args ) throws Exception {
		try ( AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose() ) {
			run( args );
		}
	}

	private static void run( final String... args ) {
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , new NegotiatorConfigGroup() , new LocationUtilityConfigGroup() );


	}
}
