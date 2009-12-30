/* *********************************************************************** *
 * project: org.matsim.*
 * GeneratedDemandRunner
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package tutorial.example8DemandGeneration;

import org.matsim.run.Controler;
import org.matsim.run.OTFVis;


/**
 * Runs the generated demand and displays the 0th iteration in OTFVis
 * @author dgrether
 *
 */
public class GeneratedDemandRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler("../matsimExamples/tutorial/example8DemandGeneration/config.xml");
		controler.setOverwriteFiles(true) ;
		controler.run();

		String[] outputMvi = {"../matsimExamples/tutorial/example8DemandGeneration/output/ITERS/it.0/0.otfvis.mvi"};
		OTFVis.main(outputMvi);
		
	}

}
