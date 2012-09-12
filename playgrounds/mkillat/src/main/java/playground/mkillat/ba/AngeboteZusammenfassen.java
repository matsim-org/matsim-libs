package playground.mkillat.ba;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AngeboteZusammenfassen {


	public static String zusammenfassen (List <String> input, String filename, String seite){
		List <Angebot> angebote = new ArrayList<Angebot>();
		List <String> datums = new ArrayList<String>();
		List <String> zeiten = new ArrayList<String>();
		List <String> preise = new ArrayList<String>();
		List <String> plaetze = new ArrayList<String>();
		List <String> ids = new ArrayList<String>();
		
		for (int i = 1; i < input.size(); i++) {
			String[] result = input.get(i).split("\"");
			List <String> line = new ArrayList <String>();
			for (int j = 0; j < result.length; j++) {
				line.add(result[j]);
			}
			
			if (line.get(1).equals("column-4")){
				String[] result2 = line.get(2).split(";");
				String temp = result2[1];
				String[] resul3 = temp.split("<");
				datums.add(resul3[0]);
			}
			
			if (line.get(1).equals("column-5")){
				String[] result2 = line.get(2).split(" ");
				if(result2.length<2){
					zeiten.add("0");
				}else{
					zeiten.add(result2[1]);	
				}
				
			}
			
			if (line.get(1).equals("column-6")){
				String[] result2 = line.get(2).split(">");
				String temp = result2[1];
				String[] resul3 = temp.split(",");
				if(resul3[0 ].equals("</td")){
					preise.add("0");
				}
				else{
					preise.add(resul3[0]);
				}
			}
			
			if (line.get(1).equals("column-7")){
				String[] result2 = line.get(2).split(">");
				String temp = result2[1];
				String[] resul3 = temp.split("<");
				plaetze.add(resul3[0]);
			}
			
			if (line.get(1).equals("column-8")){
				ids.add(result[7]);
			}
		}
		
		for (int i = 0; i < datums.size(); i++) {
			if(seite.equals("1") ||  seite.equals("2") ){
				AuslesenGetTelNumber.auslesen(ids.get(i));
				String number = GetTelNumber.read("C:\\Dokumente und Einstellungen\\Marie\\ba\\datei_temp.html");
				Angebot temp = new Angebot(datums.get(i), zeiten.get(i), preise.get(i), plaetze.get(i), ids.get(i), number);
				angebote.add(temp);
			}else{
				Angebot temp = new Angebot(datums.get(i), zeiten.get(i), preise.get(i), plaetze.get(i), ids.get(i), "000");
				angebote.add(temp);
			}
		
			
		}
		filename = filename.replace("_Seite" + seite + ".html", ".txt");
		
		try {
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename),true));
			writer.write("# datum; zeit; preis; plaetze; id; nummer"); 
			writer.newLine();
			

			
			for (int i = 0; i < angebote.size(); i++) {
				writer.write(angebote.get(i).datum + ";" + angebote.get(i).zeit + ";" + angebote.get(i).preis + ";" + angebote.get(i).plaetze + ";" + angebote.get(i).id+  ";" + angebote.get(i).nummer);
				writer.newLine();
			}
			
			
			
			writer.flush();
			writer.close();	
			System.out.println("Die Datei wurde nach " + filename + " geschrieben.");
		} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	

	}

		return filename;
	}

	
}
