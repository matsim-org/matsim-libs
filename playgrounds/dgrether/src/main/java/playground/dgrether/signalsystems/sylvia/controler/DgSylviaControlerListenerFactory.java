/* *********************************************************************** *
 * project: org.matsim.*
 * DgSylviaControlerListenerFactory
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
package playground.dgrether.signalsystems.sylvia.controler;

import org.matsim.contrib.signals.controler.SignalsControllerListener;
import org.matsim.contrib.signals.controler.SignalsControllerListenerFactory;



/**
 * @author dgrether
 *
 */
public class DgSylviaControlerListenerFactory implements SignalsControllerListenerFactory {

	private DgSylviaConfig sylviaConfig;
	private boolean alwaysSameMobsimSeed = false ;
	
	public void setAlwaysSameMobsimSeed(boolean alwaysSameMobsimSeed) {
		this.alwaysSameMobsimSeed = alwaysSameMobsimSeed;
	}

	public DgSylviaControlerListenerFactory(DgSylviaConfig sylviaConfig) {
		this.sylviaConfig = sylviaConfig;
	}

	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		return new DgSylviaSignalControlerListener(sylviaConfig, this.alwaysSameMobsimSeed) ;
	}

}
