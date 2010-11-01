/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicWithindayStarter
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
package playground.dgrether.satellic;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;


/**
 * @author dgrether
 *
 */
public class DgSatellicWithindayStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		controler.addControlerListener(new BeforeMobsimListener() {
			
			@Override
			public void notifyBeforeMobsim(BeforeMobsimEvent event) {
				event.getControler().setMobsimFactory(new DgWithindayMobsimFactory());
			}
		});
		controler.run();
	}

}
