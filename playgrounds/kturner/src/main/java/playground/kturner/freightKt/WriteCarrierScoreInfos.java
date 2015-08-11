package playground.kturner.freightKt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;

/**
 * @author kt
 * Schreibt die wesentlichen Informationen des entsprechenden JSPRIT-Laufs als neue Zeile in die angegebene Datei. 
 * Die einzelnen Werte sind Tabulator getrennt. Wird das ganze zum ersten Mal aufgerufen, so wird eine Kopfzeile erstellt.
 * Aktuell handelt es sich um folgende Informationen:
 * RunId, Anzahl der Carrier, Gesamtscore, Scores der einzelnen Carrier
 */

/*TODO: 
 * Das ganze nochmal für Anzahl der Vehicle je Carrier (sortiert nach VehTyp)
 * Vlt wird es ja überflüssig, sobald jsprit 1.4.3 läuft und somit der AlgorithmEventsRecorder zur Verfügung steht.
 */

class WriteCarrierScoreInfos {
	
	private Collection<Carrier> carriers;
	private int run;
	private double totalScore = 0.0;
	private Map <Id<Carrier>, Double> scoresOfCarrier = new TreeMap<Id<Carrier>, Double>();
	
	
	WriteCarrierScoreInfos(Carriers carriers, File file, int run) {
		this.carriers = carriers.getCarriers().values();
		this.run = run;
		
		werteBestimmen();
		
		if (file.exists()){
			writeLinetoFile(file);
		} else {
			writeHeadLine(file);
		    writeLinetoFile(file);
		}
	}

	
	void werteBestimmen() {
		for (Carrier carrier : carriers){
			if (carrier.getSelectedPlan() == null) {
				return;
			}
			scoresOfCarrier.put(carrier.getId(), carrier.getSelectedPlan().getScore());
			totalScore = totalScore + carrier.getSelectedPlan().getScore();
			}

		}
	
	
	private void writeHeadLine (File file) {
		FileWriter writer;
			
		try {
			writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

			writer.write("Carrierbasierte Auswertung der einzelnen Runs." +System.getProperty("line.separator"));
			writer.write("Hier der Scores für die einzelnen Carrier." +System.getProperty("line.separator"));
			writer.write("Run-Nr: \t Anz Carrier \t TotalScore \t");

			for(Map.Entry<Id<Carrier>, Double> e : scoresOfCarrier.entrySet()){
				writer.write(e.getKey() +"\t");
			}

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
	
	private void writeLinetoFile(File file) {
		FileWriter writer;
			
		try {
			// new FileWriter(file) - falls die Datei bereits existiert wird diese überschrieben
			writer = new FileWriter(file, true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben

			// Text wird in den Stream geschrieben
			writer.write(run + "\t");
			writer.write(carriers.size() + "\t");
			writer.write(totalScore + "\t");
			for (Double score : scoresOfCarrier.values()){
				writer.write(score+"\t");
			}
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
}
