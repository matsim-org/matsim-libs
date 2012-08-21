/* *********************************************************************** *
 * project: org.matsim.*
 * DgAirportCapacityReader
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
package air.scenario;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author dgrether
 *
 */
public class DgAirportCapacityReader {

	private static final Logger log = Logger.getLogger(DgAirportCapacityReader.class);
	private DgAirportsCapacityData capacityData;
	

	public DgAirportCapacityReader(DgAirportsCapacityData capacityData) {
		this.capacityData = capacityData;
	}

	public void readFile(String filename){
		log.info("reading from: " + filename);
		try {
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String header = br.readLine(); 
			String line = br.readLine();
			System.out.println();
			System.out.println(header);
			List<String> lines = new ArrayList<String>();
			while (line != null){
				String[] s = line.split(",");
				int number = Integer.parseInt(s[0]);
				String code = s[1];
				Double departureCapacityPerHour = null;
				if ("".compareTo(s[2]) != 0) {
					departureCapacityPerHour = Double.parseDouble(s[2]);
				}
				Double arrivalCapacityPerHour = null;
				if ("".compareTo(s[3]) != 0){
					arrivalCapacityPerHour = Double.parseDouble(s[3]);
				}
				
				Double totalCapacityPerHour = null;
				if ("".compareTo(s[4]) != 0){
					totalCapacityPerHour = Double.parseDouble(s[4]);
				}
				String source = s[5];
				
				if (departureCapacityPerHour != null && arrivalCapacityPerHour != null
						&& "LED".compareTo(code) != 0) { //LED has some wrong capacity information
					DgAirportCapacity ac = new DgAirportCapacity(code, this.capacityData.getCapacityPeriodSeconds());
					ac.setRunwayInboundFlowCapacityCarEquivPerHour(arrivalCapacityPerHour);
					ac.setRunwayOutboundFlowCapacityCarEquivPerHour(departureCapacityPerHour);
					this.capacityData.addAirportCapacity(ac);
					//some output
					String sep = "&";
					String lineEnd = "\\\\";
					StringBuilder builder = new StringBuilder();
					if (number < 10){
						builder.append("0");
						builder.append(number);
					}
					else {
						builder.append(number);
					}
					builder.append(sep);
					builder.append(code);
					builder.append(sep);
					builder.append(departureCapacityPerHour);
					builder.append(sep);
					builder.append(arrivalCapacityPerHour);
					builder.append(sep);
					if (totalCapacityPerHour == null) {
						builder.append("n/a");
					}
					else {
						builder.append(totalCapacityPerHour);
					}
					builder.append(sep);
					builder.append("\\url{");
					builder.append(source.replace("%", "\\%"));
					builder.append("} ");
					builder.append(lineEnd);
					System.out.println(builder.toString());
					lines.add(builder.toString());
				}
				line = br.readLine();
			}
			System.out.println();
			log.info("read airport capacity data successfully...");
			log.info("Capacity values used:" );
			for (String s : lines){
				log.info(s);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
		
	}
	
}
