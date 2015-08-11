package playground.kturner.freightKt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

class WriteActivitiesInclScore {

	private File file;
	private Map<Activity, Double> activities = new HashMap<Activity, Double>();
	private Map<Activity, Double> firstActivities = new HashMap<Activity, Double>();
	private Map<Activity, Double> lastActivities = new HashMap<Activity, Double>();

	WriteActivitiesInclScore(File file) {
		this.file = file;
	}

	
	void writeHeadLine()  {
		FileWriter writer;

		try {
			writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

			writer.write("angefallene Activites" +System.getProperty("line.separator"));
			writer.write("Type \t LinkID \t FacilityID \t StartTime \t EndTime \t maxDuration \t score");
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

	void addActToWriter(Activity act, Double score) {
		if (score != null){
			activities.put(act, score);
		} else {
			activities.put(act, Double.POSITIVE_INFINITY);
		}
	}

	void addFirstActToWriter(Activity act, Double score) {
		if (score != null){
			firstActivities.put(act, score);
		} else {
			firstActivities.put(act, Double.POSITIVE_INFINITY);
		}
	}

	void addLastActToWriter(Activity act, Double score) {
		if (score != null){
			lastActivities.put(act, score);
		} else {
			lastActivities.put(act, Double.POSITIVE_INFINITY);
		}
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
			for (Activity act : firstActivities.keySet()){
				writer.write(act.getType() +"\t"+ act.getLinkId() +"\t"+ act.getFacilityId() +"\t"+ act.getStartTime() +"\t"+ act.getEndTime() +"\t"+ act.getMaximumDuration() +"\t"+ firstActivities.get(act));
				writer.write(System.getProperty("line.separator"));
			}

			writer.write("### Normal Activites: ###" + System.getProperty("line.separator"));
			for (Activity act : activities.keySet()){
				writer.write(act.getType() +"\t"+ act.getLinkId() +"\t"+ act.getFacilityId() +"\t"+ act.getStartTime() +"\t"+ act.getEndTime() +"\t"+ act.getMaximumDuration() +"\t"+ activities.get(act));
				writer.write(System.getProperty("line.separator"));
			}

			writer.write("### Last Activites: ###" + System.getProperty("line.separator"));
			for (Activity act : lastActivities.keySet()){
				writer.write(act.getType() +"\t"+ act.getLinkId() +"\t"+ act.getFacilityId() +"\t"+ act.getStartTime() +"\t"+ act.getEndTime() +"\t"+ act.getMaximumDuration() +"\t"+ lastActivities.get(act));
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
