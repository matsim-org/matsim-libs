/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.counts;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

public class CSVToCountingStations {
	
	private class CsData {
		
		private Map<String, ArrayList<Integer>> volumePerTime = new TreeMap<String, ArrayList<Integer>>(); //Map<time, volumes[]>
				
		Map<String, ArrayList<Integer>> getVolumePerTime() {
			return volumePerTime;
		}

		void addTimeAndVolume(String time, Integer vol){
			volumePerTime.put(time, new ArrayList<Integer>());
			volumePerTime.get(time).add(vol);
		}
		
		void addVolume(String time,Integer vol){
			this.volumePerTime.get(time).add(vol);
		}
		
		Integer calcVolumePerTime(String time){
			Integer sum = 0;
			for (int i=0; i<this.volumePerTime.get(time).size(); i++){
				sum += this.volumePerTime.get(time).get(i);
			}
			return sum;
		}
		
		Boolean containsTime(String time){
			if (this.volumePerTime.containsKey(time)){
				return true;
			} else
			return false;
		}
	}
	
	private final String outputCSFile;
	private final String csDataFile;
	
	private String vehCat;
	private String csIdFile;
	private Map<String,Id<Link>> csIdString2LinkId = new HashMap<String,Id<Link>>();	
	private Map<String, Integer> catColumnInCSVFile = new TreeMap<String, Integer>();
	private Map<String, CsData> valuesOfCS = new TreeMap<String, CsData>();
	

//	public CSVToCountingStations(String outputFile){
//		this.ouputCSFile = outputFile;
//		this.csIdFile = null;	
//		this.csDataFile = null;
//	}
//	
//	public CSVToCountingStations(String outputFile, String csIdFile){
//		this.ouputCSFile = outputFile;
//		this.csIdFile = csIdFile;
//		this.csDataFile = null;
//	}
//	
	public CSVToCountingStations(String outputFile, String csIdFile, String csDataFile, String vehCat){
		this.outputCSFile = outputFile;
		this.csIdFile = csIdFile;
		this.csDataFile = csDataFile;
		this.vehCat = vehCat;
	}
	
	public CSVToCountingStations(String ouputCSFile, String csDataFile,
			String vehCat, Map<String, Id<Link>> idMap) {
		this.outputCSFile = ouputCSFile;
		this.csDataFile = csDataFile;
		this.vehCat = vehCat;
		this.csIdString2LinkId = idMap;
	}

	//Variante mit CSV Parser, der auch Leerfelder korrekt erfasst.
	//Zeilen, in denen keine LinkId angegeben ist, werden ignoriert.
	void createCsIdString2LinkId(){
		BufferedReader r = IOUtils.getBufferedReader(csIdFile);
		try{
			String line = r.readLine();

			while((line = r.readLine()) != null){	
				String[] splittedLine = line.split(";");
				String cs_id = splittedLine[0];		//Name of CS in Database
				String link_id = splittedLine[1];		//Link of CS

				if (cs_id != null){
					if (link_id.equalsIgnoreCase("") == false) {
						if(csIdString2LinkId.containsKey(cs_id) == false){
							csIdString2LinkId.put(cs_id, Id.create(link_id, Link.class));	
							System.out.println("added to map: " + cs_id );
						} else {
							System.out.println("key already exists!");
						}
					} else {
						System.out.println("CS with Id has no linkId: " + cs_id);
					}
				}
			}
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	void readCountsFromCSV(){	
		BufferedReader r = IOUtils.getBufferedReader(csDataFile);

		try{
			//headline
			String line = r.readLine();
			String[] splittedLine = line.split(";");
			for (int i=0; i < splittedLine.length; i++){
				catColumnInCSVFile.put(splittedLine[i], i);
			}
			
			//Prepare data map with the ids of all CSs which are assigned to a link 
			for (String csId :csIdString2LinkId.keySet()){
				valuesOfCS.put(csId, new CsData());
			}
			
			//data lines
			while((line = r.readLine()) != null){	
				splittedLine = line.split(";");
				String id = splittedLine[catColumnInCSVFile.get("PC")];
				String time = splittedLine[catColumnInCSVFile.get("HORA")];
				int vol = Integer.parseInt(splittedLine[catColumnInCSVFile.get(vehCat)]);
				if (valuesOfCS.containsKey(id)){
					if(!valuesOfCS.get(id).containsTime(time)){ //Create new CSData-Set for this time
						valuesOfCS.get(id).addTimeAndVolume(time, vol);
					} else {	//Only add Volume
						valuesOfCS.get(id).addVolume(time, vol);
					}	
				}
			}
		} catch(IOException e){
			e.printStackTrace();
		}

	}

	void writeCountsToFile() {
		Counts counts = new Counts();
		counts.setYear(2013);
		counts.setName("BD_FLUJOS_CONTINUAS-14Oct13");
		counts.setDescription("Santiago de Chile");
		for (String cs_id : valuesOfCS.keySet()){
			Map<Integer, ArrayList<String>> dataTimesPerHour =  writeDataTimesPerHour(cs_id);
			Count count = counts.createAndAddCount(Id.create(csIdString2LinkId.get(cs_id), Link.class), cs_id);
			for (int hour : dataTimesPerHour.keySet()){
				count.createVolume(hour, calcVolumePerHour(cs_id, dataTimesPerHour, hour));
			}
		}
		CountsWriter countsWriter = new CountsWriter(counts);
		countsWriter.write(outputCSFile);
		System.out.println("counts file written: " + outputCSFile);
	}
	
	//Note: In data the time describes the beginning of the time-window. In case of count it describes the end of the time-window.
	//e.g. The volume for the time from 6:00-7:00 are in data: 06:xx, but for the count.createVolume-method it is ("hour" 7).
	private Map<Integer, ArrayList<String>> writeDataTimesPerHour(String id){
		Map<Integer, ArrayList<String>> dataTimesPerHour = new TreeMap<Integer, ArrayList<String>>();
		for (Integer i = 1; i <= 24; i++){ //Hours 1-24
			dataTimesPerHour.put(i, new ArrayList<String>());
			 for (String t : valuesOfCS.get(id).getVolumePerTime().keySet()){
				 if (Integer.valueOf((t.substring(0,2)))+1 == i){ //correct the different time interpretations (see above)
					 dataTimesPerHour.get(i).add(t);
				 } 
			 }
		}
		return dataTimesPerHour;
	}
	
	private Integer calcVolumePerHour(String id, Map<Integer, ArrayList<String>> dataPerHour, int hour){
		Integer sum = 0;
		for (String time : dataPerHour.get(hour)){
				sum += valuesOfCS.get(id).calcVolumePerTime(time);
			}
		return sum;
	}
	
}
