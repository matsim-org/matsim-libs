package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes;

import java.util.ArrayList;
import java.util.TreeMap;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.etappenParsers.MZ2000EtappenParser;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.etappenParsers.MZ2005EtappenParser;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.etappenParsers.MZ2010EtappenParser;



public class EtappenLoader {

	/**
	 * @param args
	 * @return 
	 * @throws Exception 
	 */
	public static TreeMap<String, ArrayList<Etappe>> loadData(int year) throws Exception{
		
		if(year==2010){		
			//LOADING OF 2010 ETAPPEN DATA
			System.out.println("Loading etappen data from 2010...");
			String etappe2010File = "P:/Daten/Mikrozensen Verkehr Schweiz/2010/3_DB_SPSS/dat files/etappen.dat";
			TreeMap<String, ArrayList<Etappe>> etappes2010 = new TreeMap<String, ArrayList<Etappe>>();	
			new MZ2010EtappenParser(etappes2010).parse(etappe2010File);	
			System.out.println("    ...done");
			
			System.out.println(etappes2010.size());
		
		return etappes2010;
		
		
		}else if(year==2005){
			//LOADING OF 2005 ETAPPEN DATA
			System.out.println("Loading etappen data from 2005...");
			String etappe2005File = "P:/Daten/Mikrozensen Verkehr Schweiz/MZ 2005/MZ05_Datenbank_CH/2_DB_SPSS/dat files/etappen.dat";
			TreeMap<String, ArrayList<Etappe>> etappes2005 = new TreeMap<String, ArrayList<Etappe>>();	
			new MZ2005EtappenParser(etappes2005).parse(etappe2005File);		
			System.out.println("    ...done");
			
			System.out.println(etappes2005.size());
			
			return etappes2005;
			
		}else if(year==2000){
			//LOADING OF 2000 ETAPPEN DATA
			System.out.println("Loading etappen data from 2000...");
			String etappe2000File = "P:/Daten/Mikrozensen Verkehr Schweiz/Mz2000/spss/2000/dat_files/etappen.dat";
			TreeMap<String, ArrayList<Etappe>> etappes2000 = new TreeMap<String, ArrayList<Etappe>>();	
			new MZ2000EtappenParser(etappes2000).parse(etappe2000File);		
			System.out.println("    ...done");
			
			System.out.println(etappes2000.size());
			
			return etappes2000;
			
		}else{
			throw new RuntimeException("Etappen Loader cannot load data from year " + year);
		}

	}
	

	

}
