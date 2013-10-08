package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges;

import java.util.ArrayList;
import java.util.TreeMap;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges.wegeParsers.MZ2000WegeParser;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges.wegeParsers.MZ2005WegeParser;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges.wegeParsers.MZ2010WegeParser;



public class WegeLoader {

	/**
	 * Loads the wege INLAND information
	 * @param 
	 * @return 
	 * @throws Exception 
	 */
	public static TreeMap<String, ArrayList<Wege>> loadData(int year) throws Exception{
		
		if(year==2010){		
			//LOADING OF 2010 WEGE DATA
			String wege2010File = "P:/Daten/Mikrozensen Verkehr Schweiz/2010/3_DB_SPSS/dat files/wegeinland.dat";
			System.out.println("Loading wege data from 2010... ("+wege2010File+")");
			TreeMap<String, ArrayList<Wege>> weges2010 = new TreeMap<String, ArrayList<Wege>>();	
			new MZ2010WegeParser(weges2010).parse(wege2010File);	
			System.out.println("    ...done");
			
			System.out.println(weges2010.size());
		
		return weges2010;
		
		
		}else if(year==2005){
			//LOADING OF 2005 WEGE DATA
			String wege2005File = "P:/Daten/Mikrozensen Verkehr Schweiz/MZ 2005/MZ05_Datenbank_CH/2_DB_SPSS/dat files/wegeinland.dat";
			System.out.println("Loading wege data from 2005... ("+wege2005File+")");
			TreeMap<String, ArrayList<Wege>> weges2005 = new TreeMap<String, ArrayList<Wege>>();	
			new MZ2005WegeParser(weges2005).parse(wege2005File);		
			System.out.println("    ...done");
			
			System.out.println(weges2005.size());
			
			return weges2005;
			
		}else if(year==2000){
			//LOADING OF 2000 WEGE DATA
			String wege2000File = "P:/Daten/Mikrozensen Verkehr Schweiz/Mz2000/spss/2000/dat_files/wegeinland.dat";
			System.out.println("Loading wege data from 2000... ("+wege2000File+")");
			TreeMap<String, ArrayList<Wege>> weges2000 = new TreeMap<String, ArrayList<Wege>>();	
			new MZ2000WegeParser(weges2000).parse(wege2000File);		
			System.out.println("    ...done");
			
			System.out.println(weges2000.size());
			
			return weges2000;
			
		}else{
			throw new RuntimeException("Wege Loader cannot load data from year " + year);
		}

	}

}
