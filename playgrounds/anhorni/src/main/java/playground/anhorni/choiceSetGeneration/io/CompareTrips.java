package playground.anhorni.choiceSetGeneration.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.locationchoice.analysis.mc.MZTrip;


public class CompareTrips {
	
	private String outdir;
	private String mode;
	
	private TreeMap<Id, MZTrip> mzTrips = new TreeMap<Id, MZTrip>();;
	
	public CompareTrips(String outdir, String mode) {
		this.outdir = outdir;
		this.mode = mode;		
	}

	public void compare(String file, List<ChoiceSet> choiceSets) {
		
		List<String> nelsonChoiceSetIdList = null;
		if (this.mode.equals("car")) {
			nelsonChoiceSetIdList= this.readNelsonChoiceSet("input/choice_set_auto.dat");
		}
		else if (this.mode.equals("walk")) {
			nelsonChoiceSetIdList= this.readNelsonChoiceSet("input/choice_set_walk.dat");
		}
		this.readMZTrips("input/MZ2005_Wege.dat");
		
		
		List<String> choiceSetIdList = new Vector<String>();	
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			choiceSetIdList.add(choiceSet_it.next().getId().toString());
		}
		
		List<String> inChoiceSetList = new Vector<String>();
		
		
		String outfile = outdir + this.mode +"_CompareTrips.txt";	
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			bufferedReader.readLine(); // Skip header
					
			try {		
				final String header="Id\tAction\tWmittel\tAusmittel\t" +
						"Following trip wmittel\tFollowing trip ausmittel\tIs in old "+ this.mode+ " choice set file [1]";						
				final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
				out.write(header);
				out.newLine();	
				String curr_line;
				while ((curr_line = bufferedReader.readLine()) != null) {								
					String[] entries = curr_line.split("\t", -1);					
					String tripId = entries[0].trim();
					
					boolean isInNelsonChoiceSet = nelsonChoiceSetIdList.contains(tripId);
					
					if (choiceSetIdList.contains(tripId)) {
						
						inChoiceSetList.add(tripId);
						/*
						 * This does NOT work:
						 
						choiceSetIdList.remove(tripId);
						*/
					}
					else {					
						String wmittel = null;
						String ausmittel = null;				
						if (this.mode.equals("car")) {
							wmittel = entries[28].trim();
							ausmittel = entries[15].trim();	
						}
						else {
							wmittel = entries[30].trim();
							ausmittel = entries[17].trim();
						}
						String wmittelOut = this.getWmittel(wmittel);
						String ausmittelOut = this.getAusmittel(ausmittel);
						MZTrip followingTripMZ = this.getNextTrip(tripId); 
						
						out.write(tripId + "\t" + "removed from old trip set" + "\t" + wmittelOut +"\t" + ausmittelOut +"\t" +
								followingTripMZ.getWmittel() + "\t" + followingTripMZ.getAusmittel() + 
								"\t" + Boolean.toString(isInNelsonChoiceSet));
						out.newLine();
					}
				}
				out.flush();
				
				choiceSet_it = choiceSets.iterator();
				while (choiceSet_it.hasNext()) {
					ChoiceSet choiceSet = choiceSet_it.next();					
					if (!inChoiceSetList.contains(choiceSet.getId().toString())) {
						
						boolean isInNelsonChoiceSet = nelsonChoiceSetIdList.contains(choiceSet.getId());
						
						String wmittelOut = this.mzTrips.get(choiceSet.getId()).getWmittel();
						String ausmittelOut = this.mzTrips.get(choiceSet.getId()).getAusmittel();
						MZTrip followingTripMZ = this.getNextTrip(choiceSet.getId().toString()); 
						
						out.write(choiceSet.getId().toString() + "\t" + "newly added to trip set" + "\t" + wmittelOut +"\t" + ausmittelOut +"\t" +
								followingTripMZ.getWmittel() +"\t" + followingTripMZ.getAusmittel() +
								"\t" + Boolean.toString(isInNelsonChoiceSet));
						out.newLine();
					}
				}	
				out.flush();
				out.close();
				
			} catch (final IOException e) {
				Gbl.errorMsg(e);
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
	}
	
	private void readMZTrips(String file) {	
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			bufferedReader.readLine(); // Skip header
			
			String curr_line;
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);
				
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				String tripNr = entries[3].trim();
				
				Id id = new IdImpl(HHNR + ZIELPNR + tripNr);
				
				Coord coord = new CoordImpl(
						Double.parseDouble(entries[30].trim()), Double.parseDouble(entries[31].trim()));
				
				double startTime = 60* Double.parseDouble(entries[5].trim());
				
				double endTime = startTime;
				if (entries[41].trim().length() > 0) {
					endTime = 60* Double.parseDouble(entries[41].trim());
				}
					
				MZTrip mzTrip = new MZTrip(id, null, coord, startTime, endTime);
				
				String wmittel = entries[53].trim();
				mzTrip.setWmittel(this.getWmittel(wmittel));
				
				String ausmittel = entries[59].trim();
				mzTrip.setAusmittel(this.getAusmittel(ausmittel));
							
				this.mzTrips.put(id, mzTrip);
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
	}
	
	
	private List<String> readNelsonChoiceSet(String file) {		
		List<String> nelsonChoiceSetIds = new Vector<String>();
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			bufferedReader.readLine(); // Skip header
			
			String curr_line;
			while ((curr_line = bufferedReader.readLine()) != null) {					
				String[] entries = curr_line.split("\t", -1);
				String id = entries[0].trim();
				nelsonChoiceSetIds.add(id);
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return nelsonChoiceSetIds;
	}
	
	private String getWmittel(String inString) {
		
		String outString = inString;
		if (inString.equals("9")) {
			outString = "car";
		}
		else if (inString.equals("15")) {
			outString = "walk";
		}
		else if (inString.equals("14")){
			outString = "bike";
		}
		return outString;
	}
	
	
	private String getAusmittel(String inString) {
		
		String outString = inString;
		if (inString.equals("6")) {
			outString = "car";
		}
		else if (inString.equals("10")) {
			outString = "walk";
		}
		else if (inString.equals("2")) {
			outString = "train";
		}
		else if (inString.equals("4")) {
			outString = "Tram";
		}
		else if (inString.equals("-97")) {
			outString = "unvollst�ndiger Ausgang";
		}
		else if (inString.equals("5")) {
			outString = "bus";
		}
		else if (inString.equals("7")) {
			outString = "motorbike";
		}
		else if (inString.equals("9")) {
			outString = "bike";
		}
		return outString;
	}
	
	private MZTrip getNextTrip(String tripId) {
		String followingTrip = String.valueOf(Integer.parseInt(tripId) + 1);
		return this.mzTrips.get(new IdImpl(followingTrip)); 
	}
	
}
