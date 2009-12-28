package playground.wrashid.PHEV.estimationStreetParkingKantonZH;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.StringTokenizer;

import playground.wrashid.PHEV.parking.data.Facility;
import playground.wrashid.PHEV.parking.data.GarageParkingData;

// This class can read the file located at 
// C:\data\SandboxCVS\ivt\studies\switzerland\world\gg25_2001_infos.txt
public class GG25Data {

	

	int kantonId = 0;
	String kantonName = null;
	int communityId = 0; // primary key
	String communityName = null;
	int population = 0;
	int Eink_2000 = 0;
	int RG_verk = 0; // between 1 (=core city) and 5 (1=rural area)
	double priceBenzin95 = 0;

	public GG25Data() {}

	public static HashMap<Integer, GG25Data> readGG25Data(String path) {
		HashMap<Integer, GG25Data> hm = new HashMap<Integer, GG25Data>();
		try {

			GG25Data gg25data = null;

			// open file and parse it

			FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String token = null;
			while (line != null) {
				gg25data = new GG25Data();
				
				
				tokenizer = new StringTokenizer(line);

				token = tokenizer.nextToken("\t");
				gg25data.kantonId = Integer.parseInt(token);

				token = tokenizer.nextToken("\t");
				gg25data.kantonName = token;
				
				token = tokenizer.nextToken("\t");
				gg25data.communityId = Integer.parseInt(token);
				
				token = tokenizer.nextToken("\t");
				gg25data.communityName = token;
				
				token = tokenizer.nextToken("\t");
				gg25data.population = Integer.parseInt(token);
				
				token = tokenizer.nextToken("\t");
				gg25data.Eink_2000 = Integer.parseInt(token);
				
				token = tokenizer.nextToken("\t");
				gg25data.RG_verk = Integer.parseInt(token);
				
				token = tokenizer.nextToken("\t");
				gg25data.priceBenzin95 = Double.parseDouble(token);

				hm.put(gg25data.communityId, gg25data);
				line = br.readLine();
			}

			fr.close();

		} catch (Exception ex) {
			System.out.println(ex);
		}
		
		return hm;
	}
	
	public static void printFacilities(HashMap<Integer, GG25Data> hm){
		for (GG25Data gg25Data:hm.values()){
			System.out.println(gg25Data.communityId +  " - " + gg25Data.communityName);
		}
	}
}
