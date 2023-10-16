/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.chessboard;

import org.matsim.core.controler.Controler;

@Deprecated
final class RunPassengerPlansFromConfig {
	// yyyy this does not work anymore.  Not secured by a testcase.  I think that it is only there to have an example to run
	// matsim _without_ freight.  --> imo, remove.  kai, jan'19

	public static void main(String[] args) {
		String configFile = "input/usecases/chessboard/passenger/config.xml" ;
		Controler controler = new Controler( configFile ) ;
		controler.run() ;
	}

}
