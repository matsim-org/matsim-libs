package playground.anhorni.crossborder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.matsim.core.gbl.Gbl;

public class Zones2NodesParser {
	
	private Hashtable<Integer, Zone> zones = new Hashtable<Integer, Zone>();
	private String file;
		
	public Zones2NodesParser(String file) {
		this.file=file;
	}
	
	// Which nodes are situated in a given zone
	public void parse() {
		int line_cnt = 0;
		try {
			FileReader file_reader = new FileReader(file);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			String curr_line;						
			ArrayList<Integer> nodes=new ArrayList<Integer>();

			boolean firstZone=true;
			Integer zoneNrPrev=-1;
			while ((curr_line = buffered_reader.readLine()) != null) {
				
				// Only read every second line
				curr_line = buffered_reader.readLine();		
				String[] entries = curr_line.split("\t", -1);

				// ZONENR	NODENR	DIRECTION
				// 0       	1       2 
				Integer zoneNr = Integer.parseInt(entries[0].trim());			
								
				/*
				if (zoneNr==55) {
					System.out.println(zoneNr);
					System.out.println(Integer.parseInt(entries[1].trim()));
				}
				*/
				
				// firstZone: for the first zone we have nothing to write. Wait until second zone starts.
				if (zoneNr!=zoneNrPrev && !firstZone) {				
					if (!this.zones.containsKey(zoneNrPrev)) {
						Zone zone=new Zone();
						zone.setId(zoneNrPrev);	
						zone.setNodes(nodes);
						zones.put(zoneNrPrev, zone);
						nodes.clear();
					}
					zoneNrPrev=zoneNr;
				}
				firstZone=false;
				nodes.add(Integer.parseInt(entries[1].trim()));	
				line_cnt++;
			}
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println(" # lines = " + line_cnt);
	}
		
	public Hashtable<Integer, Zone> getZones() {
		return this.zones;
	}
}
