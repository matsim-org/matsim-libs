/* *********************************************************************** *
 * project: org.matsim.*
 * DgRoederGershensonStarter
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
package playground.dgrether.signalsystems.roedergershenson;

import org.matsim.core.controler.Controler;


/**
 * @author dgrether
 *
 */
public class DgRoederGershensonStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		DgRoederGershensonSignalsControllerListenerFactory signalsFactory = new DgRoederGershensonSignalsControllerListenerFactory(controler.getSignalsControllerListenerFactory());
		controler.setSignalsControllerListenerFactory(signalsFactory);
		controler.run();
	}

}
