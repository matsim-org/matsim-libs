/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerRun.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.mfeil;

import org.matsim.core.controler.Controler;

/**
 * @author Matthias Feil
 * To call ControlerMFeil
 */
public class ControlerRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Controler controler = new ControlerMFeil(args);
		controler.run();

	}

}
