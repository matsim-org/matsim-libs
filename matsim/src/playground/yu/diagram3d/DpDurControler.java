/* *********************************************************************** *
 * project: org.matsim.*
 * DpDurControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.yu.diagram3d;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;

/**
 * controler for creating a matrix (departureTime, Duration, volume)
 * @author ychen
 *
 */
public class DpDurControler {

	/**
	 * @param args - config-file
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(args);
		DpDurWriter ddw = new DpDurWriter("DpDurMatrix.txt");
		
		Events events = new Events();
		events.addHandler(ddw);// TODO...

		System.out.println("@reading the eventsfile (TXTv1) ...");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());//One can also use readFile("..../...txt") hier
		System.out.println("@done.");

		ddw.writeMatrix();
		ddw.closeFile();
	}
}
