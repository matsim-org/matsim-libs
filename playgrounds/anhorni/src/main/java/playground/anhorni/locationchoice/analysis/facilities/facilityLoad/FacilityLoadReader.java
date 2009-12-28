package playground.anhorni.locationchoice.analysis.facilities.facilityLoad;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;


public class FacilityLoadReader {
	
	TreeMap<Id, FacilityLoad> facilityLoads = new TreeMap<Id, FacilityLoad>();
	private final static Logger log = Logger.getLogger(FacilityLoadReader.class);
	
	public void readFiles() {
		this.readFacilityLoadFile("input/postprocessing/loads0.txt", 0);
		this.readFacilityLoadFile("input/postprocessing/loads1.txt", 1);
	}
		
	private void readFacilityLoadFile(String file, int state) {
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String curr_line = bufferedReader.readLine(); // Skip header				
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				
				IdImpl facilityId = new IdImpl(entries[0].trim());
				double x = Double.parseDouble(entries[1].trim());
				double y = Double.parseDouble(entries[2].trim());
				CoordImpl coord = new CoordImpl(x,y);
				
				double load = Double.parseDouble(entries[4].trim());
				String facilityType = entries[7].trim();
				
				if (!facilityType.equals("shop")) {
					continue;
				}
				
				FacilityLoad facilityLoad;
				if (state == 0) {
					facilityLoad = new FacilityLoad();
					facilityLoad.setLoad0(load);
					facilityLoads.put(facilityId, facilityLoad);	
				}
				else {
					facilityLoad = facilityLoads.get(facilityId);
					facilityLoad.setLoad1(load);
				}
				facilityLoad.setFacilityId(facilityId);	
				facilityLoad.setCoord(coord);
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		log.info("Number of facility loads: " + this.facilityLoads.size());
	}

	public TreeMap<Id, FacilityLoad> getFacilityLoads() {
		return facilityLoads;
	}

	public void setFacilityLoads(TreeMap<Id, FacilityLoad> facilityLoads) {
		this.facilityLoads = facilityLoads;
	}
}