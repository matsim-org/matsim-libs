package playground.kturner.freightKt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.Carrier;



/**
 * @author kt
 * Sammelt die einzelnen anfallenden Legs und gibt diese in einer Datei aus.
 * Analyse für das LegScoring.
 */

/*TODO: 
 * 
 */

class WriteLegs {

	private File file;
	private List<Leg> legs = new ArrayList<Leg>();

	//Constructor: Writes Headline
	WriteLegs(File file, Carrier carrier) {
		this.file = file;
		if (file.exists()){
			//do nothing
		} else {
			writeHeadLine(file);
		}
	}


	private void writeHeadLine (File file) {
		FileWriter writer;

		try {
			writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

			writer.write("angefallene Legs" +System.getProperty("line.separator"));
			writer.write("DepartureTime \t Traveltime \t Mode(vehType) \t Distance \t Route");
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

	void addLegToWriter(Leg leg) {
		legs.add(leg);
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

	void writeLegsToFile(Carrier carrier) {
		FileWriter writer;

		try {
			// new FileWriter(file) - falls die Datei bereits existiert wird diese überschrieben
			writer = new FileWriter(file, true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben

			// Text wird in den Stream geschrieben
			writer.write("### Legs: ###" + System.getProperty("line.separator"));
			for (Leg leg : legs){
				writer.write(leg.getDepartureTime() +"\t"+ leg.getTravelTime() +"\t"+  leg.getMode() +"\t"+  leg.getRoute().getDistance() +"\t"+ leg.getRoute().toString() );
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
	
	void writeLegToFile(Leg leg) {
		FileWriter writer;

		try {
			// new FileWriter(file) - falls die Datei bereits existiert wird diese überschrieben
			writer = new FileWriter(file, true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben

			// Text wird in den Stream geschrieben
			writer.write(leg.getDepartureTime() +"\t"+ leg.getTravelTime() +"\t"+ leg.getMode() +"\t"+ leg.getRoute().getDistance() + "\t" + leg.getRoute().toString());
			writer.write(System.getProperty("line.separator"));

			writer.flush();

			// Schließt den Stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
