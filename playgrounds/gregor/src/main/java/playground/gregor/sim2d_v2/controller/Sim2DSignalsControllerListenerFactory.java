/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DSignalsControllerListenerFactory
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
package playground.gregor.sim2d_v2.controller;

import org.matsim.signalsystems.controler.SignalsControllerListener;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;


/**
 * @author dgrether
 *
 */
public class Sim2DSignalsControllerListenerFactory implements SignalsControllerListenerFactory {

	/**
	 * @see org.matsim.signalsystems.controler.SignalsControllerListenerFactory#createSignalsControllerListener()
	 */
	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		return new Sim2DSignalsControllerListener();
	}

}
