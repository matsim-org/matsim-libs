package freightKt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.freight.carrier.Carrier;

/**
 * @author kt
 * Sammelt die einzelnen anfallenden Betraege und gibt diese in einer Datei aus.
 */

/*TODO: 
 * 
 */

class WriteTextToFile {
	
	private File file;
		
	WriteTextToFile(File file, String string) {
		this.file = file;
		writeHeadLine(file);
		}

	
	private void writeHeadLine (File file) {
		FileWriter writer;
			
		try {
			writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

			writer.write("explizit ausgegebene Textzeilen beliebiger Art" +System.getProperty("line.separator"));
//			writer.write("Type \t LinkID \t FacilityID \t StartTime \t EndTime \t maxDuration");
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
	

	
	
}
