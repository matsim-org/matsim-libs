package playground.kturner.freightKt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.freight.carrier.Carrier;

/**
 * @author kt
 * Sammelt die einzelnen anfallenden Betraege und gibt diese in einer Datei aus.
 * Analyse für das MoneyScoring (v.a. Maut).
 */

/*TODO: 
 * addToWriter - Methode funktioniert nicht. ArrayList erhält keinen Eintrag :-(
 */

class WriteMoney {
	
	private File file;
	private List<Double> amounts = new ArrayList<Double>();
		
	WriteMoney(File file, Carrier carrier) {
		this.file = file;
		writeHeadLine(file);
		}

	
	private void writeHeadLine (File file) {
		FileWriter writer;
			
		try {
			writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

			writer.write("angefallene Geldzahlungen" +System.getProperty("line.separator"));

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
	
	void addAmountToWriter(double amount) {
		amounts.add(amount);
	}
	
	void writeAmountsToFile() {
		FileWriter writer;
			
		try {
			// new FileWriter(file) - falls die Datei bereits existiert wird diese überschrieben
			writer = new FileWriter(file, true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben

			// Text wird in den Stream geschrieben
			writer.write("### Money Paid:" + System.getProperty("line.separator"));
			writer.write("Anz Zahlungen: " + amounts.size() + System.getProperty("line.separator"));
			for (Double am : amounts){
				writer.write(am.toString());
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
	
	void writeMoneyToFile(double amount) {
		FileWriter writer;

		try {
			// new FileWriter(file) - falls die Datei bereits existiert wird diese überschrieben
			writer = new FileWriter(file, true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben

			// Text wird in den Stream geschrieben
			writer.write("Zahlung: "+ amount);
			writer.write(System.getProperty("line.separator"));

			writer.flush();

			// Schließt den Stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
