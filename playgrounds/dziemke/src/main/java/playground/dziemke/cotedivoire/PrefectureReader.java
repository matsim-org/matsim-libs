package playground.dziemke.cotedivoire;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrefectureReader {

	public static List <Prefecture> read(String filename){

		List <Prefecture> subprefectures = new ArrayList <Prefecture>();
		
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(filename));
			String line = null;
			int i=0;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				Prefecture prefecture = new Prefecture(Integer.parseInt(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
				// System.out.println("Koordinaten der " + i + ".ten Sub-Präfektur: Latitude (Breitengrad): "
				//		+ prefecture.getLatitude() + "; Longitude (Längengrad): " + prefecture.getLongitude());
				i++;
				subprefectures.add(prefecture);
			}
			System.out.println("Es wurden insgesamt " + i + " Subpräfekturen eingelsen.");
		
	} catch (FileNotFoundException e) {
		System.err.println("File not found...");
			e.printStackTrace();
	} catch (NumberFormatException e) {
		System.err.println("Wrong No. format...");
		e.printStackTrace();
	} catch (IOException e) {
		System.err.println("I/O error...");
		e.printStackTrace();
	} finally {
		try {
            reader.close();
		} catch (IOException ex) {
            ex.printStackTrace();
        }
	}
	return subprefectures;
	}
}