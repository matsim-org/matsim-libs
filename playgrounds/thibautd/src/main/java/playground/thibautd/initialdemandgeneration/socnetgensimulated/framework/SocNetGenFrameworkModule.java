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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;


import com.google.inject.AbstractModule;
import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
public class SocNetGenFrameworkModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(SocNetGenFrameworkModule.class);
	@Override
	protected void configure() {
		log.debug( "Configuring "+getClass().getSimpleName() );

		binder().requireExplicitBindings();
		binder().disableCircularProxies();

		bind( ModelRunner.class ).to( PreprocessedModelRunner.class );
		bind(TieUtility.class);
		bind( TiesWeightDistribution.class );
		bind( ModelIterator.class );
		log.debug("Configuring " + getClass().getSimpleName() + ": DONE");
	}
}
