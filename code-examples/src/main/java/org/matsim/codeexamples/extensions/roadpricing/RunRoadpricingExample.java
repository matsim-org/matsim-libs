/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.codeexamples.extensions.roadpricing;


import org.matsim.contrib.roadpricing.run.RunRoadPricingExample;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class RunRoadpricingExample {

	/**
	 * @see RunRoadPricingExample
	 */
	public static void main( String [] args ) {
		RunRoadPricingExample.main( new String []{
				"scenarios/equil-extended/config-with-roadpricing.xml"
				, "--config:controler.outputDirectory=" + "output"
				, "--config:controler.overwriteFiles=" + OverwriteFileSetting.deleteDirectoryIfExists.name()
				, "--config:controler.lastIteration=5"
		} );

	}

}
