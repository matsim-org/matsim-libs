/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.jhackney.controler;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

public class StandardControler extends Controler {

	private final Logger log = Logger.getLogger(StandardControler.class);

	public StandardControler(String args[]){
		super(args);
	}

	public static void main(final String[] args) {
		final Controler controler = new Controler(args);
		controler.addControlerListener(new StandardControlerListener());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
