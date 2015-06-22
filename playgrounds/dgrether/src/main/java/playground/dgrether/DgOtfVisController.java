package playground.dgrether;
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vis.otfvis.OTFFileWriterFactory;


/* *********************************************************************** *
 * project: org.matsim.*
 * DgOtfVisController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * Runs the MATSim default controler with the OTFVis snapshot writing attached. 
 * @author dgrether
 *
 */
public class DgOtfVisController {

	public static void main(String[] args) {
		Controler c = new Controler(args);
		c.addOverridingModule(new OTFVisModule());
		c.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		c.run();
	}

}
