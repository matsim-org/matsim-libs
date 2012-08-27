package playground.mkillat.ba;

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
		
	}
	

	private void dateiEinlesen() {
		MitfahrzentraleAuslesen reader = new MitfahrzentraleAuslesen();
		
		for (int i = 1; i < 16; i++) {
			String seite = String.valueOf(i);
			String filename = reader.auslesen("Berlin", "Hamburg",seite );
			List <String> lines = ReadTheStringFromHTML.read(filename);
			AngeboteZusammenfassen aa = new AngeboteZusammenfassen();
			List <Angebot> angebote= aa.zusammenfassen(lines, filename, seite);
			
		}
		
		for (int i = 1; i < 16; i++) {
			String seite = String.valueOf(i);
			String filename = reader.auslesen("Hamburg", "Berlin",seite );
			List <String> lines = ReadTheStringFromHTML.read(filename);
			AngeboteZusammenfassen aa = new AngeboteZusammenfassen();
			List <Angebot> angebote= aa.zusammenfassen(lines, filename, seite);
			
		}
		
		
	}
	
	public void jedeStunde(){
		Timer timer = new Timer();
		TimerTask task = new TimerTask(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				System.out.println("Sheduled @" + System.currentTimeMillis());
	            dateiEinlesen();
			}
			
		};
		timer.scheduleAtFixedRate(task, 0, 1000*60*60);

	}
	
	
	
	
}
