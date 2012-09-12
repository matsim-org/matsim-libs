package playground.mkillat.ba;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Analyse implements Runnable {


	public static void main(String[] args) {
		Analyse test = new Analyse();
		test.run();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
//		dateiEinlesen();
		jedeStunde();
//		auswertung();
//		test();
		
	}
	

	private void test() {
		String id = "39eb7c9449884e4868f0f75556ef0f30";
		AuslesenGetTelNumber.auslesen(id);
		String test = GetTelNumber.read("C:\\Dokumente und Einstellungen\\Marie\\ba\\datei_temp.html");
		System.out.println(test);
		
	}

	private void auswertung() {
		List <Angebot> test = AngebotFileReader.read("C:\\Dokumente und Einstellungen\\Marie\\ba\\datei_Berlin_Hamburg_28_8_2012_15.txt");
		System.out.println(test);
	}

	private void dateiEinlesen() {
		MitfahrzentraleAuslesen reader = new MitfahrzentraleAuslesen();
		
		lesenUndSchreiben("Berlin", "Hamburg", 15);
//		lesenUndSchreiben("Hamburg", "Berlin", 15);
		
		
	}
	
	public void lesenUndSchreiben(String vonStadt, String zuStadt, int anzahlSeiten){
		MitfahrzentraleAuslesen reader = new MitfahrzentraleAuslesen();
		List <String> filenames = new ArrayList<String>();
		for (int i = 1; i < anzahlSeiten+1; i++) {
			String seite = String.valueOf(i);
			String filename = reader.auslesen(vonStadt, zuStadt,seite );
			List <String> lines = ReadTheStringFromHTML.read(filename);
			AngeboteZusammenfassen aa = new AngeboteZusammenfassen();
			String temp = aa.zusammenfassen(lines, filename, seite);
			if(i==1){
				filenames.add(temp);
			}

			
			
		}
		
		if(filenames.size()==1){
			try{
				
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File("C:\\Dokumente und Einstellungen\\Marie\\ba\\filenamen_" + vonStadt + "_" + zuStadt + ".txt"),true));
				for (int i = 0; i < filenames.size(); i++) {
					writer.write(filenames.get(i));
					writer.newLine();
				}
				writer.flush();
				writer.close();	
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			

			}
		}
		
		
		
	}
	
	
	public void jedeStunde(){
		Timer timer = new Timer();
		TimerTask task = new TimerTask(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				System.out.println("Start @" + System.currentTimeMillis());
	            dateiEinlesen();
			}
			
		};
		timer.scheduleAtFixedRate(task, 0, 1000*60*60*3);

	}
	
	
	
	
}
