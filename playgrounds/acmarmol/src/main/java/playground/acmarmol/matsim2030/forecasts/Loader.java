package playground.acmarmol.matsim2030.forecasts;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import playground.acmarmol.matsim2030.forecasts.p2030preparation.Municipalities;
import playground.acmarmol.matsim2030.forecasts.p2030preparation.Municipality;
import playground.acmarmol.matsim2030.forecasts.p2030preparation.OldP2030;

public  class Loader {
	
	public static Municipalities loadOldP2030Totals() throws IOException{
		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/P2030/";
		OldP2030 oldP2030 = new OldP2030();
		oldP2030.read(inputBase + "P2030Original.txt");
		
		System.out.println("Number of municipalities OLD P2030:" + oldP2030.getMunicipalitiesObject().getMunicipalities().size());
		return oldP2030.getMunicipalitiesObject();
		
	}
	
	public static TreeMap<String, Integer> loadGemeindetypologieARE(String fileName) throws NumberFormatException, IOException {
		TreeMap <String, Integer> gemeinde_typen = new TreeMap<>();
		
		System.out.println("reading "+ fileName);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
		
		String curr_line = br.readLine(); // Skip header
		int counter=0;	
		while ((curr_line = br.readLine()) != null) {
			counter++;
			String[] entries = curr_line.split("\t", -1);
			
			gemeinde_typen.put(entries[1], Integer.parseInt(entries[8]));
		}

		br.close();
		System.out.println("Number of municipalities Gemeindetypen file:" + counter );
		return gemeinde_typen;
		
	}
	
	
	public static TreeMap<String, Integer> loadMunicipalityTotals(String fileName) throws IOException {
		
		System.out.println("reading "+ fileName);
		TreeMap <String, Integer> mun_totals = new TreeMap <>();

		int counter=0;	
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
		
		String curr_line;
		//String curr_line = br.readLine(); // Skip header
			
		while ((curr_line = br.readLine()) != null) {
			counter++;
			String[] entries = curr_line.split("\t", -1);
			
		
			mun_totals.put(entries[0], Integer.parseInt(entries[2]));
			
			
			
		}
		System.out.println("Number of municipalities 2010 (population totals BFS website):" + counter );
		return  mun_totals;
		
	}
	
	
	public static HashMap<String, String> loadEliminatedMunicipalitiesDatabase(String fileName) throws IOException, FileNotFoundException {
		
		System.out.println("reading "+ fileName);
		HashMap<String, String> munic_changes = new HashMap<>();		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
		
		String curr_line = br.readLine();br.readLine(); // Skip headers
			
		while ((curr_line = br.readLine()) != null) {
			String[] entries = curr_line.split("\t", -1);
			
			//encoding: (old BFS nr, new BFS nummer)
			munic_changes.put(entries[3], entries[7]);
				
		}
		br.close();
		return  munic_changes;
		
	}
	
	public static void loadARE2030MunicipalityTotals(Municipalities municipalities) throws IOException {
		
		String inputfile = "C:/local/marmolea/input/Activity Chains Forecast/P2030/Raumdaten 2030 BFS Mittel Basis 2005A.txt";

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile), "ISO-8859-1"));
		String curr_line = br.readLine(); // Skip header
		
			while ((curr_line = br.readLine()) != null) {
				
				String[] entries = curr_line.split("\t", -1);
				
				Id<Municipality> id = Id.create(entries[1], Municipality.class);
				if(!municipalities.getMunicipalities().containsKey(id)){
					
					Municipality municipality = new Municipality(id);
					municipality.setName(entries[2]);
					double[] population =  {Double.parseDouble(entries[5]),Double.parseDouble(entries[6]),Double.parseDouble(entries[7])};
					municipality.setPopulation(population);
					municipality.setEmployment(Integer.parseInt(entries[19]));
					municipalities.addMunicipality(municipality);
					
				}
			}
			
	
	System.out.println("Number of municipalities ARE P2030: " + municipalities.getMunicipalities().size());
			
	}
	
	
	public static ArrayList<HashMap<Id<Municipality>, Id<Municipality>>> loadOZandMZDatabase(String fileName) throws IOException, FileNotFoundException {
		
		ArrayList<HashMap<Id<Municipality>, Id<Municipality>>> OZandMZ = new ArrayList<HashMap<Id<Municipality>, Id<Municipality>>>();
		HashMap<Id<Municipality>, Id<Municipality>> OZs = new HashMap<>();
		HashMap<Id<Municipality>, Id<Municipality>> MZs = new HashMap<>();
			
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
		
		String curr_line = br.readLine(); // Skip header
			
		String hh_gem;
		String OZ;
		String MZ;
		
		
		while ((curr_line = br.readLine()) != null) {
			
			String[] entries = curr_line.split("\t", -1);
			
			hh_gem = entries[2].trim();
			OZ = entries[8].trim();
			MZ = entries[4].trim();
			
			OZs.put(Id.create(hh_gem, Municipality.class), Id.create(OZ, Municipality.class));
			MZs.put(Id.create(hh_gem, Municipality.class), Id.create(MZ, Municipality.class));
				
		}
		
		OZandMZ.add(OZs);
		OZandMZ.add(MZs);
		
		System.out.println("Number of municipalities read in MZ and OZ file:  "+ OZs.size());
						
		return  OZandMZ;
		
	}
	
public static TreeMap<String, Tuple<String, String>> loadTravelTimesToMZandOZ(String fileName, ArrayList<HashMap<Id<Municipality>, Id<Municipality>>> MZandOZ) throws IOException, FileNotFoundException {
		
		TreeMap<String, Tuple<String, String>> travel_times = new TreeMap<String, Tuple<String, String>>();
		
		HashMap<Id<Municipality>, Id<Municipality>> ozs = MZandOZ.get(0);
		HashMap<Id<Municipality>, Id<Municipality>> mzs = MZandOZ.get(1);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
		
		String curr_line = br.readLine(); //save header to index columns
		String[] columns = curr_line.split("\t", -1);
		
		String code;

		while ((curr_line = br.readLine()) != null) {
		
			String[] entries = curr_line.split("\t", -1);
			
			code = entries[0].trim();
			Id<Municipality> oz = ozs.get(Id.create(code, Municipality.class));
			Id<Municipality> mz = mzs.get(Id.create(code, Municipality.class));
			
			if(oz==null || mz==null){
				throw new RuntimeException("Could not found mz or oz for municipality nr: " + code);
			}
			
//			System.out.println(code);
//			System.out.println("OZ: " + oz);
//			System.out.println("MZ: " + mz);
			
			int oz_index = Arrays.asList(columns).indexOf(oz.toString());
			int mz_index = Arrays.asList(columns).indexOf(mz.toString());
			
			String oz_tt = entries[oz_index];
			String mz_tt = entries[mz_index];
			
			travel_times.put(code, new Tuple<String,String>(oz_tt,mz_tt));

		}
		br.close();
		return  travel_times;
		
	}

public static HashMap<String, Tuple<String, String>> loadCreatedMunicipalitiesDatabase(String fileName) throws IOException {
	
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
	String curr_line = br.readLine(); br.readLine(); // Skip headers
	
	String before_code;
	String after_code;
	String new_name;
	HashMap<String, Tuple<String, String>> code_pairs = new HashMap<String, Tuple<String,String>>();
	
	while ((curr_line = br.readLine()) != null) {
	
		
		String[] entries = curr_line.split("\t", -1);
		
		before_code = entries[3];
		after_code = entries[7];
		new_name = entries[8];
		
		code_pairs.put(before_code, new Tuple<String, String>(after_code, new_name));
	}
	br.close();
	return code_pairs;
}


public static HashMap<String, String> loadCreatedMunicipalitiesDatabase2(String fileName) throws IOException {
	
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
	String curr_line = br.readLine(); br.readLine(); // Skip headers
	
	String before_code;
	String after_code;

	HashMap<String, String> code_pairs = new HashMap<String, String>();
	
	while ((curr_line = br.readLine()) != null) {
	
		
		String[] entries = curr_line.split("\t", -1);
		
		before_code = entries[3];
		after_code = entries[7];

		if(!code_pairs.containsKey(after_code)){
		code_pairs.put(after_code, before_code);
		}
	}
	br.close();
	return code_pairs;
}

public static HashMap<Id<Municipality>, Id<Municipality>> loadOZtoGrossZtr(String fileName) throws IOException {
	
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
	String curr_line = br.readLine(); // Skip headers

	HashMap<Id<Municipality>, Id<Municipality>> ozToGrossZtr = new HashMap<Id<Municipality>, Id<Municipality>>();
	
	while ((curr_line = br.readLine()) != null) {
	
		
		String[] entries = curr_line.split("\t", -1);
		
		Id<Municipality> oz = Id.create(entries[0], Municipality.class);
		Id<Municipality> grossZtr = Id.create(entries[1], Municipality.class);

		
		ozToGrossZtr.put(oz, grossZtr);
		
	}
	br.close();
	
	return ozToGrossZtr;

	
}
	
}
