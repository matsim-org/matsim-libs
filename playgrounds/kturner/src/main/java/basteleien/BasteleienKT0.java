package basteleien;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class BasteleienKT0 {

	/**
	 * @param args
	 */



	public static void main(String[] args) {
		String tempDir ="F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Basteleien/Temp";
		String finalDir ="F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Basteleien/Ziel";
		
		createDiretory(tempDir);
		fileSchreiben(new File(tempDir + System.getProperty("file.separator")+ "FileWriterTest.txt"));
		fileSchreiben(new File(tempDir + System.getProperty("file.separator")+ "FileWriterTest2.txt"));
		fileSchreiben(new File(tempDir + System.getProperty("file.separator")+ "FileWriterTest3.txt"));
		moveFiles(new File(tempDir), new File(finalDir));
		System.out.println("#### Fertig ####");

	}

	private static void fileSchreiben(File file){

		FileWriter writer;
		try {
			// new FileWriter(file ,true) - falls die Datei bereits existiert
			// werden die Bytes an das Ende der Datei geschrieben
			
			//writer = new FileWriter(file ,true);
			
			// new FileWriter(file) - falls die Datei bereits existiert
			// wird diese überschrieben
			writer = new FileWriter(file);

			// Text wird in den Stream geschrieben
			writer.write("Hallo Wie gehts?");
			writer.write("\t");		//Tabulator in Stream schreiben
			writer.write("Sieht doch nach nem Tab aus ;-)");
			writer.write(System.getProperty("line.separator")); // Platformunabhängiger Zeilenumbruch wird in den Stream geschrieben    
			writer.write("Danke mir gehts gut!");


			// Schreibt den Stream in die Datei
			// Sollte immer am Ende ausgeführt werden, sodass der Stream 
			// leer ist und alles in der Datei steht.
			writer.flush();

			// Schließt den Stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Test zum Erstellen eines Verzeichnisses  -> funktioniert
	private static void createDiretory(String testString_KT) {
		File file = new File(testString_KT);
		System.out.println("Der angebene Pfad " + testString_KT + " existiert ja/nein? " + file.exists());
		System.out.println("Der angebene Pfad " + testString_KT + " wurde erstellt ja/nein? " + file.mkdir());
		System.out.println("Der angebene Pfad " + testString_KT + " existiert ja/nein? " + file.exists());
	}
	
	private static void moveFiles (File sourceDir, File destDir) {	
		File[] files = sourceDir.listFiles();
		File destFile = null;
		destDir.mkdirs();

		try{
			for (int i = 0; i < files.length; i++) {
				destFile = new File(destDir.getAbsolutePath() + System.getProperty("file.separator") + files[i].getName());
				if (files[i].isDirectory()) {
					//copyDir(files[i], newFile);
				}
				else {
					files[i].renameTo(destFile);
					System.out.println("Dateiwurde verschoben: " + files[i].toString());
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
