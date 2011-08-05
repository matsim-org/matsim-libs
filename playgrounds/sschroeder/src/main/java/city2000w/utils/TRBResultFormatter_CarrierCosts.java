package city2000w.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;



public class TRBResultFormatter_CarrierCosts {
	static Logger logger = Logger.getLogger(TRBResultFormatter_CarrierCosts.class);
	
	static class Result {
		String carrier;
		String distance;
		public Result(String carrier, String distance) {
			super();
			this.carrier = carrier;
			this.distance = distance;
		}
	}
	public static void main(String[] args) throws IOException {
		
		String fileFolder = "case_heavyProhibitions_toll/";
		
		File outfile = new File("/Users/stefan/Documents/Schroeder/Dissertation/paper/trb_2012/" + fileFolder);
		outfile.mkdirs();
		
		File newFile = new File(outfile.getAbsolutePath()+ "/carrierCosts.txt");
		
		File fileDirectory  = new File("output/" + fileFolder);
		String[] files = getFiles(fileDirectory);;
		  
		
		Map<String,List<Result>> results = new HashMap<String, List<Result>>();
		for(String file : files){
			BufferedReader reader = IOUtils.getBufferedReader(file);
			String line = null;
			while((line = reader.readLine()) != null){
				String[] tokens = line.split(";");
				String carrierName = null;
				String distance = null;
				Result result = null;
				if(tokens.length > 0){
					carrierName = tokens[0];
					distance = tokens[1];
					result = new Result(carrierName,distance);
				}
				if(result != null){
					if(results.containsKey(carrierName)){
						results.get(carrierName).add(new Result(carrierName,distance));
					}
					else{
						List<Result> list = new ArrayList<TRBResultFormatter_CarrierCosts.Result>();
						list.add(result);
						results.put(carrierName, list);
					}
				}
			}
			reader.close();
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(newFile.getAbsolutePath());
		writer.write("name;run;distance\n");
		for(String carrier : results.keySet()){
			List<Result> rList = results.get(carrier);
			for(int i=0;i<rList.size();i++){
				writer.write(getName(carrier) + ";" + (i+1) + ";" + (int)Math.round(Double.parseDouble(rList.get(i).distance)) + "\n");
			}
		}
		Map<String,Result> avgResults = makeAvg(results);
		for(String carrier : avgResults.keySet()){
			Result r = avgResults.get(carrier);
			writer.write(getName(carrier) + ";Avg;" + (int)Math.round(Double.parseDouble(r.distance)) + "\n");
		}
		writer.close();
	}
	private static Map<String, Result> makeAvg(Map<String, List<Result>> results) {
		Map<String,Result> avgResults = new HashMap<String, TRBResultFormatter_CarrierCosts.Result>();
		for(String carrier : results.keySet()){
			List<Result> rList = results.get(carrier);
			double sumDist = 0.0;
			for(int i=0;i<rList.size();i++){
				sumDist += Double.parseDouble(rList.get(i).distance);
			}
			Double avg = sumDist / rList.size();
			avgResults.put(carrier, new Result(carrier, avg.toString()));
		}
		return avgResults;
	}
	private static String getName(String carrier) {
		if(carrier.equals("total")){
			return "Total";
		}
		if(carrier.equals("carrier_heavy")){
			return "Carrier 4";
		}
		if(carrier.equals("carrier_medium")){
			return "Carrier 3";
		}
		if(carrier.equals("carrier_small_north")){
			return "Carrier 1";
		}
		if(carrier.equals("carrier_small_south")){
			return "Carrier 2";
		}
		return carrier;
	}
	private static String[] getFiles(File outputDirectory) {
		String file = "trbCarrierCosts.txt";
		String[] files = new String[10];
		for(int i=0;i<10;i++){
			files[i] = outputDirectory + "/" + i + "/" + file;
		}
		return files;
	}

}
