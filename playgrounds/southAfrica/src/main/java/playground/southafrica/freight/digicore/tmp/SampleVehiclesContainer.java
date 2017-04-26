/* *********************************************************************** *
 * project: org.matsim.*
 * SampleVehiclesContainer.java
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

/**
 * 
 */
package playground.southafrica.freight.digicore.tmp;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesWriter;
import playground.southafrica.utilities.Header;

/**
 * Samples a given fraction of vehicles from a {@link DigicoreVehicles}
 * container, and writes the output to another container.
 * 
 * @author jwjoubert
 */
public class SampleVehiclesContainer {
	
	public static void main(String[] args) {
		Header.printHeader(SampleVehiclesContainer.class.toString(), args);
		
		String inputContainer = args[0];
		String outputContainer = args[1];
		double fraction = Double.parseDouble(args[2]);
		if(fraction < 0 || fraction > 1){
			throw new IllegalArgumentException("Fraction must be in the range [0,1]: " + fraction);
		}
		
		DigicoreVehicles dvIn = new DigicoreVehicles();
		new DigicoreVehiclesReader(dvIn).readFile(inputContainer);

		DigicoreVehicles dvOut = new DigicoreVehicles();
		dvOut.setCoordinateReferenceSystem(dvIn.getCoordinateReferenceSystem());
		dvOut.setDescription(dvIn.getDescription());
		dvOut.setSilentLog(dvIn.isSilent());
		
		Random rnd = MatsimRandom.getLocalInstance();
		rnd.setSeed(20170116);
		for(Id<Vehicle> vid : dvIn.getVehicles().keySet()){
			if(rnd.nextDouble() <= fraction){
				dvOut.addDigicoreVehicle(dvIn.getVehicles().get(vid));
			}
		}
		
		new DigicoreVehiclesWriter(dvOut).write(outputContainer);
		
		Header.printFooter();
	}
	
	
}
