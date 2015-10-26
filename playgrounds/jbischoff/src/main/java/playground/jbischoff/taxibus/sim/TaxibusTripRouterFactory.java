/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.sim;

import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;

import javax.inject.Provider;

/**
 * @author jbischoff
 *
 */
public class TaxibusTripRouterFactory implements Provider<TripRouter> {

	private Controler controler; 
	
	public TaxibusTripRouterFactory(Controler controler) {
		this.controler = controler;
	}
	
	@Override
	public TripRouter get() {
		final Provider<TripRouter> delegate = TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario());

        TripRouter tr = delegate.get();
        tr.setRoutingModule("taxibus", new TaxibusServiceRoutingModule(controler));
		return tr;
	}

}
