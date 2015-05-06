package santiago;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
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
		Counts counts = new Counts();
		counts.setYear(2013);
		counts.setName("Test");
		counts.setDescription("Test");
		
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
			
			//Write all Ids
			for (String csId :idMap.keySet()){
				valuesOfCS.put(csId, new CsData());
			}
			
			//data lines
			while((line = r.readLine()) != null){	
				splittedLine = csvParser.parseLine(line);
				String id = splittedLine[catColumnInCSVFile.get("PC")];
				String time = splittedLine[catColumnInCSVFile.get("HORA")];
				int vol = Integer.parseInt(splittedLine[catColumnInCSVFile.get("C01")]);
				System.out.println(id + " "+ time + " " + vol);
				System.out.println(valuesOfCS.get(id).toString());
				if(!valuesOfCS.get(id).containsTime(time)){ //Create new CSData-Set for this time
					valuesOfCS.get(id).addTimeAndVolume(time, vol);
				} else {	//Only add Volume
					valuesOfCS.get(id).addVolume(time, vol);
				}	
				System.out.println("Summe C01 zur Zeit: CS-ID: " + id + "Zeit:" + time + "Wert: "+  valuesOfCS.get(id).calcVolumePerTime(time));
			}
		} catch(IOException e){
			e.printStackTrace();
		}
		
		
//		for (String cs_id : IdMap.keySet()){
//			Count count = counts.createAndAddCount(Id.create(IdMap.get(cs_id), Link.class), cs_id);
//			count.createVolume(7, 1000);
//			count.createVolume(8, 1000);
//			count.createVolume(9, 1000);
//		}
		CountsWriter countsWriter = new CountsWriter(counts);
		countsWriter.write(ouputCSFile);
	}
	
}
