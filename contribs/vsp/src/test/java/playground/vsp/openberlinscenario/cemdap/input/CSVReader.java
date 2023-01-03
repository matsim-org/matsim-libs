package playground.vsp.openberlinscenario.cemdap.input;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author gthunig
 */
public class CSVReader {

    private String zeile;
    private ArrayList<String> list = new ArrayList<>();
    private String[] split = null;
    private BufferedReader data;
	private boolean isReady = false; 
	private String seperator;

    public CSVReader(String inputCSVFile, String seperator) {
        try {
                FileReader file = new FileReader(inputCSVFile);
                data = new BufferedReader(file);
                this.isReady = true;
        } catch (FileNotFoundException e) {
            System.out.println("Datei nicht gefunden");
        }
        this.seperator = seperator;
    }
   
    public boolean isReady() {
    	return isReady;
    }
    
    public void close() {
    	try {
    		isReady = false;
    		data.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public ArrayList<String> readFile() {
		try {
			while ((zeile = data.readLine()) != null) {
			    split = zeile.split(this.seperator);
				for (String aSplit : split) {
					//leere Zeilen ignorieren
					if (!(aSplit.equals("")))
						//eventuelle Leerzeichen zwischen zwei ',' entfernen
						//und Wert in Liste schreiben
						list.add(aSplit.trim());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return list;
    }
    
    public String[] readLine() {
		try {
			if ((zeile = data.readLine()) != null) {
			    split = zeile.split(this.seperator);
			    for(int i=0; i<split.length; i++) {
			        //eventuelle Leerzeichen zwischen zwei ',' entfernen
			        //und Wert in Liste schreiben
			        split[i] = split[i].trim();
			    }
			    return split;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }

	public static int getColumnNumber(String[] line, String column) {
		for (int i = 0; i < line.length; i++) {
			if (line[i].equals(column)) return i;
		}
		return -1;
	}
}