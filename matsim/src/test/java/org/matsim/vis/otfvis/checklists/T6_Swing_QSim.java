/* *********************************************************************** *
 * project: org.matsim.*
 * 
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

package org.matsim.vis.otfvis.checklists;

import org.matsim.vis.otfvis.OTFClientSwing;

/**
 * @author florian ostermann
 */
public class T6_Swing_QSim {
	private static final String mviFile = "./Output/OTFVisTests/QSim/ITERS/it.1/1.otfvis.mvi";
	
	public static void main(String[] args) {
		new OTFClientSwing(mviFile).run();
	}

}
