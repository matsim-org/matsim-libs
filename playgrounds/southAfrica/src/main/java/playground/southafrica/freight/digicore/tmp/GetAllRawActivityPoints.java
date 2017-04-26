/* *********************************************************************** *
 * project: org.matsim.*
 * GetAllRawActivityPoints.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class GetAllRawActivityPoints {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GetAllRawActivityPoints.class.toString(), args);
		
		String input = args[0];
		String output = args[1];
		
		DigicoreVehicles dvs = new DigicoreVehicles();
		new DigicoreVehiclesReader(dvs).readFile(input);
		
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("x,y");
			bw.newLine();
			
			for(DigicoreVehicle dv : dvs.getVehicles().values()){
				for(DigicoreChain chain : dv.getChains()){
					for(DigicoreActivity act : chain.getAllActivities()){
						bw.write(String.format("%.0f,%.0f\n", act.getCoord().getX(), act.getCoord().getY()));
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		Header.printFooter();
	}

}
