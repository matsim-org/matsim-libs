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

package org.matsim.contrib.minibus.hook;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Decide which {@link org.matsim.core.router.TripRouterFactory} to use.
 * 
 * @author aneumann, droeder
 *
 */
class PTripRouterFactoryFactory {

	private final static Logger log = Logger.getLogger(PTripRouterFactoryFactory.class);

	public static Provider<TripRouter> getTripRouterFactoryInstance(MatsimServices controler, 
			Class<? extends Provider<TripRouter>> tripRouterFactory, PTransitRouterFactory pTransitRouterFactory){

		if(tripRouterFactory == null){
			// standard case
			return new PTripRouterFactoryImpl(controler, pTransitRouterFactory);
		} else {

			Provider<TripRouter> factory;
			try {
				try{
					Class<?>[] args = new Class[1];
					args[0] = Controler.class;
					Constructor<? extends Provider<TripRouter>> c = null;
					c = tripRouterFactory.getConstructor(args);
					factory = c.newInstance(controler);
				}catch(NoSuchMethodException e){
					try {
						Class<?>[] args = new Class[2];
						args[0] = Controler.class;
						args[1] = Provider.class;
						Constructor<? extends Provider<TripRouter>> c = null;
						c = tripRouterFactory.getConstructor(args);
						factory = c.newInstance(controler, pTransitRouterFactory);
					} catch (NoSuchMethodException ee) {
						throw new NoSuchMethodException("Cannot find Constructor in TripRouterFactory " + tripRouterFactory.getSimpleName() + 
								" with single argument of type Controler or arguments of type Controler and TransitRouterFactory. " +
								"ABORT!\n" );
					}
				}
				log.info("Loaded TripRouterFactory " + tripRouterFactory.getSimpleName() + "...");
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}

            return factory;
		}
	}

}