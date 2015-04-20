package freightKt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.Carrier;

/**
 * @author kt
 * Sammelt die einzelnen anfallenden Activities und gibt diese in einer Datei aus.
 * Analyse für das ActivityScoring.
 */

/*TODO: 
 * 
 */

class WriteActivities {
	
	private File file;
	private List<Activity> activities = new ArrayList<Activity>();
	private List<Activity> firstActivities = new ArrayList<Activity>();
	private List<Activity> lastActivities = new ArrayList<Activity>();
		
	WriteActivities(File file) {
		this.file = file;
		writeHeadLine(file);
		}

	
	private void writeHeadLine (File file) {
		FileWriter writer;
			
		try {
			writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

			writer.write("angefallene Activites" +System.getProperty("line.separator"));
			writer.write("Type \t LinkID \t FacilityID \t StartTime \t EndTime \t maxDuration");
			writer.write(System.getProperty("line.separator"));

			// Schreibt den Stream in die Datei
			// Sollte immer am Ende ausgeführt werden, sodass der Stream 
			// leer ist und alles in der Datei steht.
			writer.flush();

			// Schließt den Stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}
	
	void addActToWriter(Activity act) {
		activities.add(act);
	}
	
	void addFirstActToWriter(Activity act) {
		firstActivities.add(act);
	}
	
	void addLastActToWriter(Activity act) {
		lastActivities.add(act);
	}
	
	void writeTextLineToFile(String text){
		FileWriter writer;
		
		try {
			writer = new FileWriter(file, true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben
			writer.write(text);
			writer.write(System.getProperty("line.separator"));
			writer.flush();

			// Schließt den Stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}
	
	void writeCarrierLine (Carrier carrier) {
		FileWriter writer;

		try {
			writer = new FileWriter(file, true);  //wird an File angehangen

			writer.write("#Carrier: "+ carrier.getId().toString());
			writer.write(System.getProperty("line.separator"));

			writer.flush();

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void writeActsToFile() {
		FileWriter writer;
			
		try {
			// new FileWriter(file) - falls die Datei bereits existiert wird diese überschrieben
			writer = new FileWriter(file, true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben

			// Text wird in den Stream geschrieben
			writer.write("### First Activites: ###" + System.getProperty("line.separator"));
			for (Activity act : firstActivities){
			writer.write(act.getType() +"\t"+ act.getLinkId() +"\t"+ act.getFacilityId() +"\t"+ act.getStartTime() +"\t"+ act.getEndTime() +"\t"+ act.getMaximumDuration());
			writer.write(System.getProperty("line.separator"));
			}
			
			writer.write("### Normal Activites: ###" + System.getProperty("line.separator"));
			for (Activity act : activities){
			writer.write(act.getType() +"\t"+ act.getLinkId() +"\t"+ act.getFacilityId() +"\t"+ act.getStartTime() +"\t"+ act.getEndTime() +"\t"+ act.getMaximumDuration());
			writer.write(System.getProperty("line.separator"));
			}
			
			writer.write("### Last Activites: ###" + System.getProperty("line.separator"));
			for (Activity act : lastActivities){
			writer.write(act.getType() +"\t"+ act.getLinkId() +"\t"+ act.getFacilityId() +"\t"+ act.getStartTime() +"\t"+ act.getEndTime() +"\t"+ act.getMaximumDuration());
			writer.write(System.getProperty("line.separator"));
			}
		
			// Schreibt den Stream in die Datei
			// Sollte immer am Ende ausgeführt werden, sodass der Stream 
			// leer ist und alles in der Datei steht.
			writer.flush();

			// Schließt den Stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}
}
