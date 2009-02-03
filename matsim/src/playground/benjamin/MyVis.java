/* *********************************************************************** *
 * project: org.matsim.*
 * 
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
package playground.benjamin;

import org.matsim.run.OTFVis;

/**
 *
 */
public class MyVis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String otffile = "../studies/bkickhoefer/output/oneRouteTwoModeTest_1000/ITERS/it.1000/1000.otfvis.mvi";
//		String otffile = "../studies/bkickhoefer/output/oneRouteTwoModeTest_2000/ITERS/it.2000/2000.otfvis.mvi";
		String otffile = "C:/4_Meins/Studium/WiIng/Hauptstudium/Diplomarbeit/Eclipse_WS/run703/it.500/500.events.mvi";
		OTFVis.main(new String[] {otffile});
	}

}
