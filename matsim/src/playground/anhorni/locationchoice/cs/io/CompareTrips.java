package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.IOUtils;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.MZTrip;


public class CompareTrips {
	
	private String outdir;
	private String mode;
	
	private TreeMap<Id, MZTrip> mzTrips = new TreeMap<Id, MZTrip>();;
	
	public CompareTrips(String outdir, String mode) {
		this.outdir = outdir;
		this.mode = mode;		
	}

	public void compare(String file, List<ChoiceSet> choiceSets) {
		
		this.read0("input/MZ2005_Wege.dat", this.mode);
		
		List<String> choiceSetIdList = new Vector<String>();	
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			choiceSetIdList.add(choiceSet_it.next().getId().toString());
		}		
		String outfile = outdir + this.mode +"_CompareTrips.txt";	
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
					
			try {		
				final String header="Id\tin choice set\twmittel\tausmittel\tfollowing trip wmittel\tfollowing trip ausmittel";						
				final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
				out.write(header);
				out.newLine();								
				while ((curr_line = bufferedReader.readLine()) != null) {								
					String[] entries = curr_line.split("\t", -1);					
					String tripId = entries[0].trim();					
					if (choiceSetIdList.contains(tripId)) {
						choiceSetIdList.remove(tripId);
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
						String wmittelOut = "undefined";
						String ausmittelOut = "undefined";
						if (wmittel.equals("9")) {
							wmittelOut = "car";
						}
						else if (wmittel.equals("15")) {
							wmittelOut = "walk";
						}
						if (ausmittel.equals("6")) {
							ausmittelOut = "car";
						}
						else if (ausmittel.equals("10")) {
							ausmittelOut = "walk";
						}

						String followingTrip = String.valueOf(Integer.parseInt(tripId) + 1);
						MZTrip followingTripMZ = this.mzTrips.get(new IdImpl(followingTrip)); 
						
						out.write(tripId + "\t" + "0" + "\t" + wmittelOut +"\t" + ausmittelOut +"\t" +
								followingTripMZ.getWmittel() + "\t" + followingTripMZ.getAusmittel());
						out.newLine();
					}
				}
				out.flush();
				
				choiceSet_it = choiceSets.iterator();
				while (choiceSet_it.hasNext()) {
					ChoiceSet choiceSet = choiceSet_it.next();					
					if (!choiceSetIdList.contains(choiceSet.getId().toString())) {
						out.write(choiceSet.getId().toString() + "\t" + "1" + "\t" + this.mode +"\t" + "?" +"\t" +
								this.mode +"\t" + "?");
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
	
	
	
	private void read0(String file, String mode) {
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
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
					
				MZTrip mzTrip = new MZTrip(id, coord, startTime, endTime);
				
				String wmittelIn = entries[53].trim();
				String wmittel = "undefined";
				if (wmittelIn.equals("15")) {
					wmittel = "walk";
				}
				else if (wmittelIn.equals("9")) {
					wmittel = "car";
				}
				mzTrip.setWmittel(wmittel);
				
				String ausmittelIn = entries[59].trim();
				String ausmittel = "undefined";
				if (ausmittelIn.equals("10")) {
					ausmittel = "walk";
				}
				else if (wmittelIn.equals("6")) {
					ausmittel = "car";
				}
				mzTrip.setAusmittel(ausmittel);
				
				
				this.mzTrips.put(id, mzTrip);
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
	}
	
}
