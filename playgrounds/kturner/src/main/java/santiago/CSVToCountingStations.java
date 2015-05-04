package santiago;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import au.com.bytecode.opencsv.CSVParser;

public class CSVToCountingStations {
	
	private final String ouputCSFile;
	
	private String CSIdFile;
	private Map<String,Id<Link>> IdMap = new HashMap<String,Id<Link>>();
	

	public CSVToCountingStations(String outputFile){
		this.ouputCSFile = outputFile;
		this.CSIdFile = null;	
	}
	
	public CSVToCountingStations(String outputFile, String CSIdFile){
		this.ouputCSFile = outputFile;
		this.CSIdFile = CSIdFile;
	}
	
	public void createCS(){
		createIds();
		readCountsFromXML();
	}
	
	//Varinte 2 mit CSV Parser, der auch Leerfelder korrekt erfasst.
	//Zeilen, in denen keine LinkId angegeben ist, werden ignoriert.
	private void createIds(){
		BufferedReader r = IOUtils.getBufferedReader(CSIdFile);
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
						if(IdMap.containsKey(cs_id) == false){
							IdMap.put(cs_id, Id.create(link_id, Link.class));	
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
	

	/*
	 * TODO aus XML einlesen -> XMLParser schreiben
	 * #Werte auf Stundenbasis schreiben, 
	 * #zunächst nur PKW (C01)
	 * #Zwei Varianten:
	 *  - Fahrzeugbsiert: Unabhängig von Besetzungsgrad nur die Anz Fahrzeuge aufsunnieren
	 * 	- Personenbasiert: Kategorie 2 auch mit 2 PErsonen (Faktor 2 besetzt ist), etc
	 * 
	 * Test mit den fixen Werten funktioniert ;-) KT, 4.5.15
	 */
	private void readCountsFromXML(){
		Counts counts = new Counts();
		counts.setYear(2013);
		counts.setName("Test");
		counts.setDescription("Test");
		
		for (String cs_id : IdMap.keySet()){
			Count count = counts.createAndAddCount(Id.create(IdMap.get(cs_id), Link.class), cs_id);
			count.createVolume(7, 1000);
			count.createVolume(8, 1000);
			count.createVolume(9, 1000);
		}
		CountsWriter countsWriter = new CountsWriter(counts);
		countsWriter.write(ouputCSFile);
	}
	

}
