/* *********************************************************************** *
 * project: kai
 * KaiControler.java
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

package playground.kai.usecases.ownmobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;

public class Main {

	public static void main(String[] args) {
		
		Controler controler = new Controler( "examples/config/hybrid-config.xml" ) ;
		controler.setOverwriteFiles(true) ;

		final MobsimFactory mobsimFactory = new MobsimFactory() {
			@Override
			public RunnableMobsim createMobsim(Scenario sc, EventsManager events) {
				return new MyMobsim(sc,events) ;
			}
			
		} ;
		controler.setMobsimFactory(mobsimFactory) ;

		controler.run();
	
	}

}
