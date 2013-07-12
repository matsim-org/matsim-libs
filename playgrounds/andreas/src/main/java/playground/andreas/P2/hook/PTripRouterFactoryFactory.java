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

package playground.andreas.P2.hook;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryInternal;

/**
 * Decide which {@link TripRouterFactoryInternal} to use.
 * 
 * @author aneumann, droeder
 *
 */
public class PTripRouterFactoryFactory {

	private final static Logger log = Logger.getLogger(PTripRouterFactoryFactory.class);

	public static TripRouterFactory getTripRouterFactoryInstance(Controler controler, Class<? extends TripRouterFactory> tripRouterFactory){

		if(tripRouterFactory == null){
			// standard case
			return new PTripRouterFactoryImpl(controler);
		} else {

			TripRouterFactory factory;
			try {
				Class<?>[] args = new Class[1];
				args[0] = Controler.class;
				Constructor<? extends TripRouterFactory> c = null;
				try{
					c = tripRouterFactory.getConstructor(args);
					factory = c.newInstance(controler);
				} catch(NoSuchMethodException e){
					throw new NoSuchMethodException("Cannot find Constructor in TripRouterFactory " + tripRouterFactory.getSimpleName() + " with single argument of type Controler. " +
							"ABORT!\n" );
				}
				log.info("Loaded TripRouterFactory " + tripRouterFactory.getSimpleName() + "...");
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			return factory;
		}
	}

}