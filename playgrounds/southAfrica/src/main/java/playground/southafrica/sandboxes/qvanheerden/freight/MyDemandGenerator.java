package playground.southafrica.sandboxes.qvanheerden.freight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class MyDemandGenerator {
	public static Logger log = Logger.getLogger(MyDemandGenerator.class);
	public Map<Tuple<String, String>, List<Double>> monMap,tueMap,wedMap,thuMap,friMap = new HashMap<Tuple<String,String>, List<Double>>();
	public Map<Tuple<String, String>, double[]> monDecileMap,tueDecileMap,wedDecileMap,thuDecileMap,friDecileMap = new HashMap<Tuple<String,String>, double[]>();
	public List<Map<Tuple<String, String>, List<Double>>> listOfDowMaps = new ArrayList<Map<Tuple<String,String>,List<Double>>>();
	public List<Map<Tuple<String, String>, double[]>> listOfDecileMaps = new ArrayList<Map<Tuple<String,String>,double[]>>();
	public List<Map<Tuple<String, String>, double[]>> listOfNewDecileMaps = new ArrayList<Map<Tuple<String,String>,double[]>>();
//	public Map<String, Coord> customerMap = new HashMap<String, Coord>();
//	public List<String> productList = new ArrayList<String>();

	/**
	 * This class will create a csv file containing shipments that can be 
	 * read in when running the {@link MyCarrierPlanGenerator}.
	 * 
	 * For now only generates demand for all customers for one day of the year.
	 * Will eventually contain a demand generation model.
	 * 
	 * @param args
	 * 
	 * @author qvanheerden
	 */
	public static void main(String[] args) {
		Header.printHeader(MyDemandGenerator.class.toString(), args);

		String distributionFile = args[0];
		String customerFile = args[1];
		String productFile = args[2];
		String simpleDemandOutputDirectory = args[3];
		
		MyDemandGenerator mdg = new MyDemandGenerator();
		List<String> productList = mdg.parseProductFile(productFile);
		Map<String, Tuple<Coord, Tuple<Double, Double>>> customerMap = mdg.parseCustomerFile(customerFile);
		mdg.generateSimpleDemand(distributionFile, simpleDemandOutputDirectory + "/demand_day2.csv", 2, customerMap, productList);
		mdg.generateSimpleDemand(distributionFile, simpleDemandOutputDirectory + "/demand_day3.csv", 3, customerMap, productList);
		mdg.generateSimpleDemand(distributionFile, simpleDemandOutputDirectory + "/demand_day4.csv", 4, customerMap, productList);
		mdg.generateSimpleDemand(distributionFile, simpleDemandOutputDirectory + "/demand_day5.csv", 5, customerMap, productList);
		mdg.generateSimpleDemand(distributionFile, simpleDemandOutputDirectory + "/demand_day6.csv", 6, customerMap, productList);

		Header.printFooter();
	}

	public List<String> parseProductFile(String productFile){
		BufferedReader br = IOUtils.getBufferedReader(productFile);
		List<String> productList = new ArrayList<String>();
		
		try {
			br.readLine(); //skip header

			String line;
			while((line=br.readLine())!=null){
				String[] array = line.split(",");
				String product = array[1];
				productList.add(product);
			}
		}catch(IOException e){
			log.error("Could not read product file");
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				log.error("Could not close product file");
			}
		}
		return productList;
	}
	
	/**
	 * Read in file containing only the customers to consider
	 * @param inputFile
	 * @return
	 */
	public Map<String, Tuple<Coord, Tuple<Double, Double>>> parseCustomerFile(String inputFile){
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		Map<String, Tuple<Coord, Tuple<Double, Double>>> customerMap = new HashMap<String, Tuple<Coord,Tuple<Double,Double>>>();
		
		try {
			br.readLine(); //skip header

			String line;
			while((line=br.readLine())!=null){
				String[] array = line.split(",");
				String customer = array[1];
				double longi = Double.parseDouble(array[2]);
				double lati = Double.parseDouble(array[3]);
				double distance = Double.parseDouble(array[4]);
				double earliestStart = Double.parseDouble(array[5]);
				double latestEnd = Double.parseDouble(array[6]);

				Coord coord = new Coord(longi, lati);
				Tuple<Double, Double> timeWindow = new Tuple<Double, Double>(earliestStart, latestEnd);
				Tuple<Coord, Tuple<Double, Double>> entry = new Tuple<Coord, Tuple<Double,Double>>(coord, timeWindow);
				
				customerMap.put(customer, entry);

			}
		}catch(IOException e){
			log.error("Could not read customer file");
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				log.info("Could not close customer file");
			}
		}
		return customerMap;
	}


	public void generateSimpleDemand(String inputFile, String outputFile, int chosenSeqDay, Map<String, Tuple<Coord, Tuple<Double, Double>>> customerMap, List<String> productList){
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);

		try {
			bw.write("customer, long, lat, product, mass, sale, duration, start, end");
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
				double mass = Double.parseDouble(array[10]);
				double sale = Double.parseDouble(array[11]);
				int daysSinceLast = Integer.parseInt(array[12]);
				double lastMass = Double.parseDouble(array[13]);
				double lastSale = Double.parseDouble(array[14]);
				//double distance = Double.parseDouble(array[15]);

				//ignore everything up to sequential day=3 (that is 3 july 2013)
				if(seqDay==chosenSeqDay && customerMap.containsKey(customer) && productList.contains(product)){
					bw.newLine(); // writing new line before next entry to not have
					//a new line after last entry
					//customer, long, lat, product, mass, sale, duration, start, end
					double longi = customerMap.get(customer).getFirst().getX();
					double lati = customerMap.get(customer).getFirst().getY();
					double earliestStart = customerMap.get(customer).getSecond().getFirst();
					double latestEnd = customerMap.get(customer).getSecond().getSecond();
					
					bw.write(customer + "," + longi + "," + lati + "," + product + "," + mass + "," + sale + ",300," + earliestStart + "," + latestEnd);

				}else if(seqDay > chosenSeqDay){ //break from while loop once seqDay > chosenSeqDay
					break;
				}
			}
		}catch(IOException e){
			log.error("Could not read distribution file...");
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				log.error("Could not close reader.");
			}

			try {
				bw.close();
			} catch (IOException e) {
				log.error("Could not close writer.");
			}
		}
	}

}
//	public void parseDistributionFile(String inputFile){
//		BufferedReader br = IOUtils.getBufferedReader(inputFile);
//
//		try {
//			br.readLine(); //skip header
//
//			String line;
//			while((line=br.readLine())!=null){
//				String[] array = line.split(",");
//
//				int busMonth = Integer.parseInt(array[0]);
//				int busDay = Integer.parseInt(array[1]);
//				int calMonth = Integer.parseInt(array[2]);
//				int calDay = Integer.parseInt(array[3]);
//				int seqDay = Integer.parseInt(array[4]);
//				int dow = Integer.parseInt(array[5]);
//				String supplier = array[6];
//				String customer = array[7];
//				String group = array[8];
//				String product = array[9];
//				double mass = Integer.parseInt(array[10]);
//				double sale = Integer.parseInt(array[11]);
//				int daysSinceLast = Integer.parseInt(array[12]);
//				double lastMass = Integer.parseInt(array[13]);
//				double lastSale = Integer.parseInt(array[14]);
//				double distance = Integer.parseInt(array[15]);
//
//				//work with "order" object to enable extra work with values later...
//				Order order = new Order(busMonth, busDay, calMonth, calDay, seqDay, dow, supplier, customer, group, product, mass, sale, daysSinceLast, lastMass, lastSale, distance);
//
//				Tuple<String, String> key = new Tuple<String, String>(order.getCustomer(), order.getProduct());
//
//				//TODO check switch case statement - probably need to check if maps contain mapping yet.
//				switch (order.getDayOfWeek()) {
//				case 1:
//					List<Double> monList = monMap.get(key);
//					monList.add(order.getMass());
//					monMap.put(key, monList);
//					break;
//				case 2:
//					List<Double> tueList = tueMap.get(key);
//					tueList.add(order.getMass());
//					tueMap.put(key, tueList);
//					break;
//				case 3:
//					List<Double> wedList = wedMap.get(key);
//					wedList.add(order.getMass());
//					wedMap.put(key, wedList);
//					break;
//				case 4:
//					List<Double> thuList = thuMap.get(key);
//					thuList.add(order.getMass());
//					thuMap.put(key, thuList);
//					break;
//				case 5:
//					List<Double> friList = friMap.get(key);
//					friList.add(order.getMass());
//					friMap.put(key, friList);
//					break;
//
//				default:
//					break;
//				}
//			}
//
//		} catch (IOException e) {
//			log.error("Could not read distribution file...");
//		} finally{
//			try {
//				br.close();
//			} catch (IOException e) {
//				log.error("Could not close reader...");
//			}
//		}
//
//		log.info("Finished parsing file and populated 5 maps.");
//
//	}
//
//	public void calculateDeciles(){
//		listOfDowMaps.add(monMap);
//		listOfDowMaps.add(tueMap);
//		listOfDowMaps.add(wedMap);
//		listOfDowMaps.add(thuMap);
//		listOfDowMaps.add(friMap);
//
//		listOfDecileMaps.add(monDecileMap);
//		listOfDecileMaps.add(tueDecileMap);
//		listOfDecileMaps.add(wedDecileMap);
//		listOfDecileMaps.add(thuDecileMap);
//		listOfDecileMaps.add(friDecileMap);
//
//		for(int i = 0; i < listOfDowMaps.size(); i++){
//			Map<Tuple<String, String>, List<Double>> dowMap = listOfDowMaps.get(i);
//			Map<Tuple<String, String>, double[]> decileMap = listOfDecileMaps.get(i);
//
//			for(Tuple<String, String> key : dowMap.keySet()){
//				List<Double> list = dowMap.get(key);
//				double[] array = new double[list.size()];
//
//				for(int j = 0; j < list.size(); j++){
//					array[j] = list.get(j);
//				}
//
//				Percentile percentile = new Percentile();
//				percentile.setData(array);
//
//				double[] deciles = {10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 99.0};
//				double[] values = new double[deciles.length];
//
//				for(int j = 0; j < deciles.length; j++){
//					values[j] = percentile.evaluate(deciles[j]);
//				}
//
//				decileMap.put(key, values);
//			}
//
//			listOfNewDecileMaps.add(decileMap);
//		}
//
//	}
//
//	public void writeDecilesToFile(String outputFolder){
//
//		for(int i = 0; i < listOfNewDecileMaps.size(); i++){
//			BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "/massDeciles_" + i+2 + ".csv");
//
//			try {
//				bw.write("customer,product,10,20,30,40,50,60,70,80,90,99");
//				bw.newLine();
//
//				Map<Tuple<String, String>, double[]> decMap = listOfNewDecileMaps.get(i);
//
//				for(Tuple<String, String> tuple : decMap.keySet()){
//					bw.write(tuple.getFirst() + "," + tuple.getSecond() + ",");
//
//					for(int j = 0; j < decMap.get(tuple).length - 1; j++){
//						bw.write(String.format("%.0f,", decMap.get(tuple)[j]));
//						bw.write(",");
//					}
//					//write last value
//					bw.write(String.format("%.0f,", decMap.get(tuple)[decMap.get(tuple).length - 1])); 
//					bw.newLine();
//				}
//
//			} catch (IOException e) {
//				log.error("Could not write to file...");
//			}finally{
//				try {
//					bw.close();
//				} catch (IOException e) {
//					log.error("Could not close writer");
//				}
//			}
//
//		}
//	}
//
//	//TODO finish
//	public void parseDecilesFromFile(String decileFolder){
//		File folder = new File(decileFolder);
//		if(!folder.canRead() || !folder.exists()){
//			log.error("Decile file does not exist or is corrupt.");
//		}
//
//		List<File> fileList = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter(".csv"));
//
//		/*check if there are entries in new decile map...
//		 * if yes - clear because if we parse a file we want new values
//		 * if no - do nothing
//		 */
//		if(!listOfNewDecileMaps.isEmpty()){
//			listOfNewDecileMaps.clear();
//		}
//
//		//check if this from index is correct
//		for(File file : fileList){
//			String theDay = file.getName().substring(file.getName().indexOf("_")+1, file.getName().indexOf("."));
//			int dow = Integer.parseInt(theDay);
//
//
//
//			Map<Tuple<String, String>, double[]> decMap;
//
//		}
//
//	}