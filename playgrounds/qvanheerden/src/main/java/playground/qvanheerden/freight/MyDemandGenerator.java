package playground.qvanheerden.freight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.treetable.FileSystemModel;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import utilities.misc.Triple;

public class MyDemandGenerator {
	public static Logger log = Logger.getLogger(MyDemandGenerator.class);
	public Map<Tuple<String, String>, List<Double>> monMap,tueMap,wedMap,thuMap,friMap = new HashMap<Tuple<String,String>, List<Double>>();
	public Map<Tuple<String, String>, double[]> monDecileMap,tueDecileMap,wedDecileMap,thuDecileMap,friDecileMap = new HashMap<Tuple<String,String>, double[]>();
	public List<Map<Tuple<String, String>, List<Double>>> listOfDowMaps = new ArrayList<Map<Tuple<String,String>,List<Double>>>();
	public List<Map<Tuple<String, String>, double[]>> listOfDecileMaps = new ArrayList<Map<Tuple<String,String>,double[]>>();
	public List<Map<Tuple<String, String>, double[]>> listOfNewDecileMaps = new ArrayList<Map<Tuple<String,String>,double[]>>();
	
	/**
	 * This class will create a csv file containing shipments that can be 
	 * read in when running the {@link MyCarrierPlanGenerator}.
	 * 
	 * For now only generates two customers with a shipment each.  Will eventually 
	 * contain a demand generation model.
	 * 
	 * @param args
	 * 
	 * @author qvanheerden
	 */
	public static void main(String[] args) {
		Header.printHeader(MyDemandGenerator.class.toString(), args);
		
		String outputFile = args[0];
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		
		/* For now just write two shipments to work with
		 * - not all these fields are used in the model, but I may want to use 
		 * 	 some of the values eventually for my own analyses.
		 */
		try {
			bw.write("customer,long,lat,product,mass,sale,duration,start,end");
			bw.newLine();
			bw.write("customer_1,148340.842,-3708473.484,product_1,2500,392,500,0,86400");
			bw.newLine();
			bw.write("customer_2,140308.0297,-3704398.509,product_2,1000,577.5,500,0,86400");
			
		} catch (IOException e) {
			log.error("Could not write to file " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				log.error("Could not close writer.");
			}
		}

		Header.printFooter();
	}
	
	public void parseDistributionFile(String inputFile){
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		
		try {
			br.readLine(); //skip header
			
			String line;
			while((line=br.readLine())!=null){
				String[] array = line.split(",");
				
				int busMonth = Integer.parseInt(array[0]);
				int busDay = Integer.parseInt(array[1]);
				int calMonth = Integer.parseInt(array[2]);
				int calDay = Integer.parseInt(array[3]);
				int seqDay = Integer.parseInt(array[4]);
				int dow = Integer.parseInt(array[5]);
				String supplier = array[6];
				String customer = array[7];
				String group = array[8];
				String product = array[9];
				double mass = Integer.parseInt(array[10]);
				double sale = Integer.parseInt(array[11]);
				int daysSinceLast = Integer.parseInt(array[12]);
				double lastMass = Integer.parseInt(array[13]);
				double lastSale = Integer.parseInt(array[14]);
				double distance = Integer.parseInt(array[15]);
				
				//work with "order" object to enable extra work with values later...
				Order order = new Order(busMonth, busDay, calMonth, calDay, seqDay, dow, supplier, customer, group, product, mass, sale, daysSinceLast, lastMass, lastSale, distance);
				
				Tuple<String, String> key = new Tuple<String, String>(order.getCustomer(), order.getProduct());
				
				switch (order.getDayOfWeek()) {
				case 1:
					List<Double> monList = monMap.get(key);
					monList.add(order.getMass());
					monMap.put(key, monList);
					break;
				case 2:
					List<Double> tueList = tueMap.get(key);
					tueList.add(order.getMass());
					tueMap.put(key, tueList);
					break;
				case 3:
					List<Double> wedList = wedMap.get(key);
					wedList.add(order.getMass());
					wedMap.put(key, wedList);
					break;
				case 4:
					List<Double> thuList = thuMap.get(key);
					thuList.add(order.getMass());
					thuMap.put(key, thuList);
					break;
				case 5:
					List<Double> friList = friMap.get(key);
					friList.add(order.getMass());
					friMap.put(key, friList);
					break;

				default:
					break;
				}
			}
			
		} catch (IOException e) {
			log.error("Could not read distribution file...");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				log.error("Could not close reader...");
			}
		}
		
		log.info("Finished parsing file and populated 5 maps.");
		
	}

	public void calculateDeciles(){
		listOfDowMaps.add(monMap);
		listOfDowMaps.add(tueMap);
		listOfDowMaps.add(wedMap);
		listOfDowMaps.add(thuMap);
		listOfDowMaps.add(friMap);
		
		listOfDecileMaps.add(monDecileMap);
		listOfDecileMaps.add(tueDecileMap);
		listOfDecileMaps.add(wedDecileMap);
		listOfDecileMaps.add(thuDecileMap);
		listOfDecileMaps.add(friDecileMap);
		
		for(int i = 0; i < listOfDowMaps.size(); i++){
			Map<Tuple<String, String>, List<Double>> dowMap = listOfDowMaps.get(i);
			Map<Tuple<String, String>, double[]> decileMap = listOfDecileMaps.get(i);

			for(Tuple<String, String> key : dowMap.keySet()){
				List<Double> list = dowMap.get(key);
				double[] array = new double[list.size()];

				for(int j = 0; j < list.size(); j++){
					array[j] = list.get(j);
				}

				Percentile percentile = new Percentile();
				percentile.setData(array);

				double[] deciles = {10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 99.0};
				double[] values = new double[deciles.length];

				for(int j = 0; j < deciles.length; j++){
					values[j] = percentile.evaluate(deciles[j]);
				}

				decileMap.put(key, values);
			}
			
			listOfNewDecileMaps.add(decileMap);
		}
	
	}
	
	public void writeDecilesToFile(String outputFolder){

		for(int i = 0; i < listOfNewDecileMaps.size(); i++){
			BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "/massDeciles_" + i+2 + ".csv");

			try {
				bw.write("customer,product,10,20,30,40,50,60,70,80,90,99");
				bw.newLine();
				
				Map<Tuple<String, String>, double[]> decMap = listOfNewDecileMaps.get(i);
				
				for(Tuple<String, String> tuple : decMap.keySet()){
					bw.write(tuple.getFirst() + "," + tuple.getSecond() + ",");
					
					for(int j = 0; j < decMap.get(tuple).length - 1; j++){
						bw.write(String.format("%.0f,", decMap.get(tuple)[j]));
						bw.write(",");
					}
					//write last value
					bw.write(String.format("%.0f,", decMap.get(tuple)[decMap.get(tuple).length - 1])); 
					bw.newLine();
				}

			} catch (IOException e) {
				log.error("Could not write to file...");
			}finally{
				try {
					bw.close();
				} catch (IOException e) {
					log.error("Could not close writer");
				}
			}

		}
	}
	
	//TODO finish
	public void parseDecilesFromFile(String decileFolder){
		File folder = new File(decileFolder);
		if(!folder.canRead() || !folder.exists()){
			log.error("Decile file does not exist or is corrupt.");
		}
		
		List<File> fileList = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter(".csv"));
		
		/*check if there are entries in new decile map...
		 * if yes - clear because if we parse a file we want new values
		 * if no - do nothing
		 */
		if(!listOfNewDecileMaps.isEmpty()){
			listOfNewDecileMaps.clear();
		}
		
		//check if this from index is correct
		for(File file : fileList){
			String theDay = file.getName().substring(file.getName().indexOf("_")+1, file.getName().indexOf("."));
			int dow = Integer.parseInt(theDay);
			
			
			
			Map<Tuple<String, String>, double[]> decMap;
			
		}
		
	}
	
	//TODO finish
	public void generateDemand(){
		
	}
	
}