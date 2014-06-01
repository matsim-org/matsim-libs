package playground.vbmh.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class ReadParkhistory {
	
public LinkedList<HashMap<String,String>> events = new LinkedList<HashMap<String,String>>();
	public void readXML(String fileName){
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String zeile = "";
		
		try {
			zeile = reader.readLine();
			while ((zeile = reader.readLine()) != null) {
				events.add(new HashMap<String,String>());
				String[] felder = zeile.split(" ");
				for (int i=1; i<felder.length; i++){
					String[] wertePaar = felder[i].split("=");
					if(wertePaar[1].endsWith(">")){
						wertePaar[1]=wertePaar[1].split(">")[0];
					}
					events.getLast().put(wertePaar[0], wertePaar[1]);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //header
		
	}
	
	public  ReadParkhistory getSubHist(LinkedList<HashMap<String,String>> e){
		ReadParkhistory subHist = new ReadParkhistory();
		subHist.events=e;
		return subHist;
	}
	
	public HashMap<String,String> getEventByAttribute(String attribute, String value){
		for(HashMap<String, String> event : events){
			if(event.containsKey(attribute)){
				//System.out.println("Attribut vorhanden");
				if(event.get(attribute).equals(value)){
					return event;
				}
				
			}
		}
		return null;
	}
	
	public LinkedList<HashMap<String,String>> getAllEventByAttribute(String attribute, String value){
		LinkedList<HashMap<String,String>> liste = new LinkedList<HashMap<String,String>>();
		for(HashMap<String, String> event : events){
			if(event.containsKey(attribute)){
				//System.out.println("Attribut vorhanden");
				if(event.get(attribute).equals(value)){
					liste.add(event);
				}
				
			}
		}
		if(liste.isEmpty()){
			return null;
		}
		return liste;
	}
}
