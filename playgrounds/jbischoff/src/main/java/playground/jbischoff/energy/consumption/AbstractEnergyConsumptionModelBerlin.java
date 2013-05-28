/* *********************************************************************** *
 * project: org.matsim.*

 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.jbischoff.energy.consumption;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.AbstractInterpolatedEnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumption;
import org.matsim.core.utils.io.IOUtils;


public abstract class AbstractEnergyConsumptionModelBerlin extends
		AbstractInterpolatedEnergyConsumptionModel {
	private static final Logger log = Logger.getLogger(AbstractEnergyConsumptionModelBerlin.class);
	protected String drivingType;
	protected abstract void SetDrivingType();

	public AbstractEnergyConsumptionModelBerlin(){
				initModell();
		}

	private void initModell() {
		SetDrivingType();
		String file = "\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\svn-checkouts\\volkswagen_internal\\Dokumente_MATSim_AP1und2\\DrivingLookupTable_2011-11-25.txt";
		
		Set<String[]> values = readFileContent(file, "\t", true);

		Double f = 500.;
		log.warn("currently a factor of " + f + " is used for the discharging-Profiles, because the given values are to low...");
		
		for(String[] s: values){
			if (s[0].equals(drivingType))	  //DrivingType
				
//				log.info("drivingType: "+drivingType);
				if(s[2].equals("0"))	 //no slopes implemented
				{
					queue.add(new EnergyConsumption(Double.parseDouble(s[1]),f*Double.parseDouble(s[3])/1000));
					//divide by 1000 - model requests [J/m]
				}
			
		}
				
	}


	
	
	private  Set<String[]> readFileContent(String inFile, String splitByExpr, boolean hasHeader){
		
		boolean first = hasHeader;
		Set<String[]> lines = new LinkedHashSet<String[]>();
		
		String line;
		try {
			log.info("start reading content of " + inFile);
			
			BufferedReader reader = IOUtils.getBufferedReader(inFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split(splitByExpr);
					if(first == true){
						first = false;
					}else{
						lines.add(columns);
					}
					
					line = reader.readLine();
				}
			}while(!(line == null));
			reader.close();
			log.info("finished...");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
}
