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

package playground.mrieser.svi.controller;

import org.matsim.core.controler.Controler;

/**
 * @author mrieser
 */
public class SviController {

	public static void main(String[] args) {

//		args = new String[] {"/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/configDynusT.xml"};

		Controler controler = new Controler(args);

		// only for demo
//		controler.setOverwriteFiles(true);
//		controler.setCreateGraphs(false);
//		controler.setDumpDataAtEnd(false);
		// ------------

		DynusTUtils.integrate(controler);
		controler.run();
	}
}
