/* *********************************************************************** *
 * project: org.matsim.*
 * Main.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.sschieffer;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

public class Main {

	public static void main(String[] args) {
		String configPath="test/scenarios/equil/config.xml";
		
		
		Controler controler=new Controler(configPath);
		
		controler.addControlerListener(new AfterMobsimListener() {
			
			@Override
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				DecentralizedChargerV1 decentralizedChargerV1=new DecentralizedChargerV1();
				decentralizedChargerV1.performChargingAlgorithm(new DecentralizedChargerInfo());				
			}
		});
		
		controler.run();		
	}
	
}
