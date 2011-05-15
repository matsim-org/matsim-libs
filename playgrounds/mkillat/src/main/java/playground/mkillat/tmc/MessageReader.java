package playground.mkillat.tmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageReader {

	public static List <Message>  read(String filename){

		List <Message> messages = new ArrayList <Message>();
		
		FileReader fr;
		BufferedReader br;
		try {
		fr = new FileReader(new File (filename));
		br = new BufferedReader(fr);
		String line = null;
		br.readLine(); //Erste Zeile (Kopfzeile) wird übersprungen.
		while ((line = br.readLine()) != null) {
		String[] result = line.split(";");
		
		
		Message current = new Message(	result[0], 
										result[1],
										Integer.parseInt(result[2]),
										result [3],
										result [4]);
		
			messages.add(current);
		
		}
		

		
	} catch (FileNotFoundException e) {
		System.err.println("File not found...");
			e.printStackTrace();
	} catch (NumberFormatException e) {
		System.err.println("Wrong No. format...");
		e.printStackTrace();
	} catch (IOException e) {
		System.err.println("I/O error...");
		e.printStackTrace();
		}
		return messages;
	}
	
}
