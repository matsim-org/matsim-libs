package santiago;

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

import au.com.bytecode.opencsv.CSVParser;

public class CSVToCountingStations {
	
	private final String ouputCSFile;
	private final String csIdFile;
	private final String csDataFile;
	
	private Map<String,Id<Link>> idMap = new HashMap<String,Id<Link>>();	
	private Map<String, Integer> catColumnInCSVFile = new TreeMap<String, Integer>();
	private Map<String, CsData> valuesOfCS = new TreeMap<String, CsData>();
	

	public CSVToCountingStations(String outputFile){
		this.ouputCSFile = outputFile;
		this.csIdFile = null;	
		this.csDataFile = null;
	}
	
	public CSVToCountingStations(String outputFile, String csIdFile){
		this.ouputCSFile = outputFile;
		this.csIdFile = csIdFile;
		this.csDataFile = null;
	}
	
	public CSVToCountingStations(String outputFile, String csIdFile, String csDataFile){
		this.ouputCSFile = outputFile;
		this.csIdFile = csIdFile;
		this.csDataFile = csDataFile;
	}
	
	//Varinte mit CSV Parser, der auch Leerfelder korrekt erfasst.
	//Zeilen, in denen keine LinkId angegeben ist, werden ignoriert.
	void createIds(){
		BufferedReader r = IOUtils.getBufferedReader(csIdFile);
		try{
			String line = r.readLine();

			while((line = r.readLine()) != null){	
				CSVParser csvParser = new CSVParser(';');
				String[] splittedLine = csvParser.parseLine(line);
				String cs_id = splittedLine[0];		//Name of CS in Database
				String link_id = splittedLine[1];		//Link of CS

//				System.out.println("Anteile: "+ splittedLine.length);
//				for (int i=0; i < splittedLine.length; i++){
//					System.out.println(splittedLine[i]);
//				}

				if (cs_id != null){
					if (link_id.equalsIgnoreCase("") == false) {
						if(idMap.containsKey(cs_id) == false){
							idMap.put(cs_id, Id.create(link_id, Link.class));	
							System.out.println("added to map: " + cs_id );
						} else {
							System.out.println("key already exists!");
						}
					}
				}
			}
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	void readCountsFromCSV(){	
		BufferedReader r = IOUtils.getBufferedReader(csDataFile);
		CSVParser csvParser = new CSVParser(';');
		String[] splittedLine = null;
		try{
			//headline
			String line = r.readLine();
			splittedLine = csvParser.parseLine(line);
			for (int i=0; i < splittedLine.length; i++){
				catColumnInCSVFile.put(splittedLine[i], i);
			}
			
			//Prepare data map with the ids of all CSs which are assigned to a link 
			for (String csId :idMap.keySet()){
				valuesOfCS.put(csId, new CsData());
			}
			
			//data lines
			while((line = r.readLine()) != null){	
				splittedLine = csvParser.parseLine(line);
				String id = splittedLine[catColumnInCSVFile.get("PC")];
				String time = splittedLine[catColumnInCSVFile.get("HORA")];
				int vol = Integer.parseInt(splittedLine[catColumnInCSVFile.get("C01")]);
//				System.out.println(id + " "+ time + " " + vol);
//				System.out.println(valuesOfCS.get(id).toString());
				if(!valuesOfCS.get(id).containsTime(time)){ //Create new CSData-Set for this time
					valuesOfCS.get(id).addTimeAndVolume(time, vol);
				} else {	//Only add Volume
					valuesOfCS.get(id).addVolume(time, vol);
				}	
//				System.out.println("Summe C01 zur Zeit: CS-ID: " + id + "Zeit: " + time + "Wert: "+  valuesOfCS.get(id).calcVolumePerTime(time));
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
			Count count = counts.createAndAddCount(Id.create(idMap.get(cs_id), Link.class), cs_id);
			for (int hour : dataTimesPerHour.keySet()){
				System.out.println("Before CreateVol: CS-ID: " + cs_id + "Zeit: " + hour + "Wert: "+  calcVolumePerHour(cs_id, dataTimesPerHour, hour));
				count.createVolume(hour, calcVolumePerHour(cs_id, dataTimesPerHour, hour));
			}
		}
		CountsWriter countsWriter = new CountsWriter(counts);
		countsWriter.write(ouputCSFile);
	}
	
	//Note: In data the time describe the beginning of the time-window. In case of the count it describes the end of the time-window.
	//e.g. The volume for the time from 6:00-7:00 are in data: 06:xx, but for the count.createVolume-method it is ("hour" 7).
	private Map<Integer, ArrayList<String>> writeDataTimesPerHour(String id){
		Map<Integer, ArrayList<String>> dataTimesPerHour = new TreeMap<Integer, ArrayList<String>>();
		for (Integer i = 1; i <= 24; i++){ //Hours 1-24
			dataTimesPerHour.put(i, new ArrayList<String>());
			 for (String t : valuesOfCS.get(id).getVolumePerTime().keySet()){
				 System.out.println("TimeString: " + t +" Substring: "+ t.substring(0,2) + " mod TimeString" + (Integer.valueOf(t.substring(0,2))+1) + " i: " + i );
				 
				 if (Integer.valueOf((t.substring(0,2)))+1 == i){ //correct the different time interpretations (s.above)
					 dataTimesPerHour.get(i).add(t);
					 System.out.println("If true; current timelist: " +  dataTimesPerHour.get(i).toString());
				 } else {
					 System.out.println("ELSE should do nothing");
//					 dataTimesPerHour.get(i).add("");
				 }
			 }
			 System.out.println("Times per Hour: Hour: " + i + " times: " + dataTimesPerHour.get(i).toString());
		}
		
		return dataTimesPerHour;
	}
	
	//Note: In data the time descripe the beginning of the time-window. In case of the count it descripes the end of the time-window.
	//e.g. The volume for the time from 6:00-7:00 are in data: 06:xx, but for the count.createVolume-method it is ("hour" 7).
	private Integer calcVolumePerHour(String id, Map<Integer, ArrayList<String>> dataPerHour, int hour){
		Integer sum = 0;
		for (String time : dataPerHour.get(hour)){
				sum += valuesOfCS.get(id).calcVolumePerTime(time);
			}
		return sum;
	}
	
}
