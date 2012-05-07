/* *********************************************************************** *
 * project: org.matsim.*
 * HybridSignalsControllerListenerFactory.java
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

package playground.gregor.sim2d_v3.controller;

import java.util.ArrayList;
import java.util.List;

import org.matsim.signalsystems.controler.SignalsControllerListener;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;

/**
 * 
 * @author cdobler
 */
public class HybridSignalsControllerListenerFactory implements SignalsControllerListenerFactory {

	private final List<SignalsControllerListenerFactory> factories;
	
	public HybridSignalsControllerListenerFactory() {
		factories = new ArrayList<SignalsControllerListenerFactory>();
	}
	
	public boolean addSignalsControllerListenerFactory(SignalsControllerListenerFactory factory) {
		return this.factories.add(factory);
	}

	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		HybridSignalsControllerListener listener = new HybridSignalsControllerListener();
		for (SignalsControllerListenerFactory factory : factories) {
			listener.addSignalsControllerListener(factory.createSignalsControllerListener());
		}
		return listener;
	}
}