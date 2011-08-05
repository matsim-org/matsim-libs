package city2000w.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import freight.ScheduledCommodityFlow;
import freight.ShipperImpl;
import freight.ShipperPlanReader;
import freight.Shippers;

public class TRBResultFormatter_Frequency {
	
static Logger logger = Logger.getLogger(TRBResultFormatter_TLCCosts.class);
	
	static class Result {
		String id;
		String cost;
		public Result(String id, String cost) {
			super();
			this.id = id;
			this.cost = cost;
		}
	}
	public static void main(String[] args) throws IOException {
		
		String fileFolder = "case_heavyProhibitions_toll/";
		
		File outfile = new File("/Users/stefan/Documents/Schroeder/Dissertation/paper/trb_2012/" + fileFolder);
		outfile.mkdirs();
		
		File newFile = new File(outfile.getAbsolutePath()+ "/avgFrequency.txt");
		
		File fileDirectory  = new File("output/" + fileFolder);
		String[] files = getFiles(fileDirectory);;
		  
		Map<String,List<Result>> results = new HashMap<String, List<Result>>();
		for(String file : files){
			Shippers shippers = new Shippers();
			new ShipperPlanReader(shippers.getShippers()).read(file);
			for(ShipperImpl shipper : shippers.getShippers()){
				double sumOfFreq = 0.0;
				for(ScheduledCommodityFlow sComFlow : shipper.getSelectedPlan().getScheduledFlows()){
					sumOfFreq += sComFlow.getShipments().size();
				}
				Double avgFreq = sumOfFreq/(double)shipper.getSelectedPlan().getScheduledFlows().size();
				String shipperId = shipper.getId().toString();
				Result r = new Result(shipperId,avgFreq.toString());
				if(results.containsKey(shipperId)){
					results.get(shipperId).add(r);
				}
				else{
					List<Result> list = new ArrayList<Result>();
					list.add(r);
					results.put(shipperId, list);
				}
			}
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(newFile.getAbsolutePath());
		writer.write("name;run;freq\n");
		for(String carrier : results.keySet()){
			List<Result> rList = results.get(carrier);
			for(int i=0;i<rList.size();i++){
				writer.write(getName(carrier) + ";" + (i+1) + ";" + Double.parseDouble(rList.get(i).cost) + "\n");
			}
		}
		Map<String,Result> avgResults = makeAvg(results);
		for(String carrier : avgResults.keySet()){
			Result r = avgResults.get(carrier);
			writer.write(getName(carrier) + ";Avg;" + Double.parseDouble(r.cost) + "\n");
		}
		writer.close();
	}
	private static Map<String, Result> makeAvg(Map<String, List<Result>> results) {
		Map<String,Result> avgResults = new HashMap<String, Result>();
		for(String carrier : results.keySet()){
			List<Result> rList = results.get(carrier);
			double sumDist = 0.0;
			for(int i=0;i<rList.size();i++){
				sumDist += Double.parseDouble(rList.get(i).cost);
			}
			Double avg = sumDist / rList.size();
			avgResults.put(carrier, new Result(carrier, avg.toString()));
		}
		return avgResults;
	}
	private static String getName(String carrier) {
		/*
		 * shipper_mixedValue_2;476.69699558967363
shipper_lowValue;326.6150393378608
shipper_mixedValue_1;529.3443203642894
shipper_highValue;650.8158556384359
total;1983.4722109302597
		 */
		if(carrier.equals("shipper_mixedValue_2")){
			return "Shipper 4";
		}
		if(carrier.equals("shipper_mixedValue_1")){
			return "Shipper 3";
		}
		if(carrier.equals("shipper_highValue")){
			return "Shipper 1";
		}
		if(carrier.equals("shipper_lowValue")){
			return "Shipper 2";
		}
		if(carrier.equals("total")){
			return "Total";
		}
		return carrier;
	}
	private static String[] getFiles(File outputDirectory) {
		String file = "trbShippersAfterIteration.xml";
		String[] files = new String[10];
		for(int i=0;i<10;i++){
			files[i] = outputDirectory + "/" + i + "/" + file;
		}
		return files;
	}


}
