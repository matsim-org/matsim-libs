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

package playground.anhorni.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.core.gbl.Gbl;

public class CountsCompareReader {
	private String countsCompareFile;
	private String networkNameFile;
	private Stations stations;
	
	public CountsCompareReader(Stations stations, String countsCompareFile, String networkNameFile) {
		this.countsCompareFile = countsCompareFile;
		this.networkNameFile = networkNameFile;
		this.stations = stations;
	}
	
	private String readNetworkName() {
		String networkName = null;
		try {
			FileReader fileReader = new FileReader(this.networkNameFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line;
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1); 
				networkName = entries[0].trim();
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return networkName;
	}
	
	public void read() {
		
		String networkName = this.readNetworkName();
		
		try {
			FileReader fileReader = new FileReader(countsCompareFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line = bufferedReader.readLine(); // Skip header
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				
				String linkId = entries[0].trim();
				int hour = Integer.parseInt(entries[1].trim()) - 1;
				String matsimVolString = entries[2].trim().replaceAll(",", "");
				double matsimVol = Double.parseDouble(matsimVolString);
				
				if (!stations.addSimValforLinkId(networkName, linkId, hour, matsimVol)) {
//					log.error("Error with: " + linkId + "\thour:" + hour);
				}
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
