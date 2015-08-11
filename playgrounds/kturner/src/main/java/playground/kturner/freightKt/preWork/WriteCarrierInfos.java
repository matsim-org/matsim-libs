package playground.kturner.freightKt.preWork;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author kt
 * Schreibt die wesentlichen Informationen der Carrier in eine Datei
 * Die einzelnen Werte sind Tabulator getrennt. Wird das ganze zum ersten Mal aufgerufen, so wird eine Kopfzeile erstellt.
 * Aktuell handelt es sich um folgende Informationen:
 * CarrierId: Anz. Services, Summe der Nachfrage, Anz der Depots, verwendete VehTypes
 */

/*TODO: 
 * Anpassen, damit obiges erfüllt ist
 */

class WriteCarrierInfos {
	
	private String carrierId;
	private int nOfServices;
	private int demand;
	private int nOfDepots;
	ArrayList<String> vehTypes = new ArrayList<String>();
	
	WriteCarrierInfos(File file, String carrierId, int nOfServices, int totalDemand,
			int nOfDepots, ArrayList<String> vehTypes) {

		this.carrierId = carrierId;
		this.nOfServices = nOfServices;
		this.demand = totalDemand;
		this.nOfDepots = nOfDepots;
		this.vehTypes = vehTypes;
		
		if (file.exists()){
			writeLinetoFile(file);
		} else {
			writeHeadLine(file);
		    writeLinetoFile(file);
		}
	}	
	
	private void writeHeadLine (File file) {
		FileWriter writer;
			
		try {
			writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

//			writer.write("Analyse der einzelnen Carrier." +System.getProperty("line.separator"));
			writer.write("CarrierID: \t Anz Services \t Nachfrage \t  Anz Depots \t VehTypes");

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
			writer.write(carrierId + "\t" + nOfServices +"\t"+ demand +"\t"+ nOfDepots +"\t" );
			
			for (int i=0 ; i < vehTypes.size(); i++)
				writer.write(vehTypes.get(i) + ", ");
			
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
