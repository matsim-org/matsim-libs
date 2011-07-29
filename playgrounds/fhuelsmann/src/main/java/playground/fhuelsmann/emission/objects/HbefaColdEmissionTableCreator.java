package playground.fhuelsmann.emission.objects;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;


public class HbefaColdEmissionTableCreator {

	private final  Map<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>> HbefaColdEmissionTable =
		new TreeMap<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>>();

	public Map<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>> getHbefaColdTable() {
		return this.HbefaColdEmissionTable;
	}

	public void makeHbefaColdTable(String filename){
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			// Read and forget header line
			br.readLine();
			// Read file line by line
			while ((strLine = br.readLine()) != null)   {
				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(";");
				HbefaColdEmissionFactor obj = new HbefaColdEmissionFactor(
						array[0], //vehCat
						array[1], //component
						array[2], //parkingTime
						array[3], //distance
						Double.parseDouble(array[4]));//coldEF
				
				ColdPollutant coldPollutant = ColdPollutant.valueOf(array[1]);
				int parkingTime = Integer.valueOf(array[2].split("-")[0]);
				int distance = Integer.valueOf(array[3].split("-")[0]);
				if (this.HbefaColdEmissionTable.get(coldPollutant) != null){
					if(this.HbefaColdEmissionTable.get(coldPollutant).get(distance) != null){
						this.HbefaColdEmissionTable.get(coldPollutant).get(distance).put(parkingTime, obj);
					}
					else{
						Map<Integer, HbefaColdEmissionFactor> tempParkingTime = new TreeMap<Integer, HbefaColdEmissionFactor>();
						tempParkingTime.put(parkingTime, obj);
						this.HbefaColdEmissionTable.get(coldPollutant).put(distance, tempParkingTime);	  
					}
				}
				else{
					Map<Integer,HbefaColdEmissionFactor> tempParkingTime =	new TreeMap<Integer, HbefaColdEmissionFactor>();
					tempParkingTime.put(parkingTime, obj);
					Map<Integer, Map<Integer, HbefaColdEmissionFactor>> tempDistance = new TreeMap<Integer, Map<Integer, HbefaColdEmissionFactor>>();
					tempDistance.put(parkingTime, tempParkingTime);
					this.HbefaColdEmissionTable.put(coldPollutant, tempDistance);				
				}
			}
			//Close the input stream
			in.close();
		}
		catch(Exception e){
			// Pass this exception upwards, because there is no way we can continue if we couldn't read this file!
			throw new RuntimeException(e);
		}
	}
}