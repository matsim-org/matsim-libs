package playground.southafrica.sandboxes.qvanheerden.freight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class MyOrderPolicyAdjustor {

	private static Map<String, Tuple<Coord, Tuple<Double, Double>>> customerMap = new HashMap<String, Tuple<Coord,Tuple<Double,Double>>>();
	private static List<String> productList = new ArrayList<String>();
	private static Logger log = Logger.getLogger(MyOrderPolicyAdjustor.class);
	private static List<Map<Tuple<String, String>, List<Double>>> listOfWeekMaps = new ArrayList<Map<Tuple<String,String>,List<Double>>>();
	private static Map<Tuple<String, String>, List<Double>> weekMap = new LinkedHashMap<Tuple<String,String>, List<Double>>();
	private static Map<Tuple<String, String>, Tuple<Double, Double>> aveMap = new LinkedHashMap<Tuple<String,String>, Tuple<Double,Double>>();
	private static List<String> chosenCustomerList = new ArrayList<String>(); 

	public static void main(String[] args) {
		Header.printHeader(MyOrderPolicyAdjustor.class.toString(), args);

		String customerFile = args[0];
		String productFile = args[1];
		String distributionFile = args[2];
		String aveOutputFile = args[3];
		String aveDemandOutputFile = args[4];
		String chosenCustomerFile = args[5];
		String outputFolder = args[6];

		MyDemandGenerator mdg = new MyDemandGenerator();
		customerMap = mdg.parseCustomerFile(customerFile);
		productList = mdg.parseProductFile(productFile);

		MyOrderPolicyAdjustor mope = new MyOrderPolicyAdjustor();
		mope.parseDemand(distributionFile);
		mope.parseChosenCustomers(chosenCustomerFile);
		mope.calcAverages();
		mope.writeToFile(aveOutputFile);
		mope.generateAverageDemand(aveDemandOutputFile);
		mope.generateAdjustedDemand(aveDemandOutputFile, outputFolder);

		Header.printFooter();
	}

	public void parseChosenCustomers(String chosenCustomerFile){
		BufferedReader br = IOUtils.getBufferedReader(chosenCustomerFile);
		try {
			String customer = null;
			while((customer=br.readLine())!=null){
				chosenCustomerList.add(customer);
			}
		} catch (IOException e) {
			log.error("Could not read");
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				log.error("Could not close reader");
			}
		}
	}
	
	public void generateAdjustedDemand(String aveDemandFile, String outputFolder){
		BufferedReader br = IOUtils.getBufferedReader(aveDemandFile);
		BufferedWriter bw1 = IOUtils.getBufferedWriter(outputFolder + "demand_min08_0.csv");
		BufferedWriter bw2 = IOUtils.getBufferedWriter(outputFolder + "demand_plus08_0.csv");
		BufferedWriter bw3 = IOUtils.getBufferedWriter(outputFolder + "demand_plus18_0.csv");
		BufferedWriter bw4 = IOUtils.getBufferedWriter(outputFolder + "demand_plus28_0.csv");
		BufferedWriter bw5 = IOUtils.getBufferedWriter(outputFolder + "demand_plus38_0.csv");
		List<BufferedWriter> listOfWriters = new ArrayList<BufferedWriter>();
		listOfWriters.add(bw1);
		listOfWriters.add(bw2);
		listOfWriters.add(bw3);
		listOfWriters.add(bw4);
		listOfWriters.add(bw5);

		try {
			for(BufferedWriter bw : listOfWriters){
				bw.write("customer, long, lat, product, mass, sale, duration, start, end");
				bw.newLine();
			}

			br.readLine(); //skip header
			String line;

			while((line=br.readLine())!=null){
				String[] array = line.split(",");

				String customer = array[0];
				double longi = Double.parseDouble(array[1]);
				double lati = Double.parseDouble(array[2]);
				String product = array[3];
				double mass = Double.parseDouble(array[4]);
				double sale = Double.parseDouble(array[5]);
				double duration = Double.parseDouble(array[6]);
				double start = Double.parseDouble(array[7]);
				double end = Double.parseDouble(array[8]);

				if(chosenCustomerList.contains(customer)){
					//generate new orderSize value for given product
					Tuple<String, String> tuple = new Tuple<String, String>(customer, product);
					double aveMass = aveMap.get(tuple).getFirst();
					double aveFreq = aveMap.get(tuple).getSecond();

					if(aveMass > 0){
						double[] adjustmentValues = {-0.8, 0.8, 1.8, 2.8, 3.8};

						for(int i = 0; i < adjustmentValues.length; i++){
							double orderSize = MyOrderPolicyAdjustor.generateOrderSize(aveMass, aveFreq+adjustmentValues[i]);
							listOfWriters.get(i).write(customer + "," + longi + "," + lati + "," + product + "," + orderSize + "," + 0 + ",300," + start + "," + end);
							listOfWriters.get(i).newLine();
						}
					}


				}else{
					//just write out current mass
					double[] adjustmentValues = {-0.8, 0.8, 1.8, 2.8, 3.8};
					for(int i = 0; i < adjustmentValues.length; i++){
						listOfWriters.get(i).write(customer + "," + longi + "," + lati + "," + product + "," + mass + "," + 0 + ",300," + start + "," + end);
						listOfWriters.get(i).newLine();
					}
				}


			}
		}catch(IOException e){
			log.error("Couldn't read from file");
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				log.error("Couldn't close reader");
			}

			try {
				for(BufferedWriter bw : listOfWriters){
					bw.close();
				}
			} catch (Exception e2) {
				log.error("Couldn't close one of the writers");
			}
		}

	}

	public static Double generateOrderSize(double aveMass, double freq){
		double orderSize = 0;

		double probOrder = (freq/5);

		double randomNumber = Math.random();

		if(randomNumber >= probOrder){
			//place an order of size = weekly demand/number of orders
			orderSize = aveMass/freq;
		}

		return orderSize;
	}

	public void generateAverageDemand(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("customer, long, lat, product, mass, sale, duration, start, end");
			bw.newLine();
			for(Tuple<String, String> key : aveMap.keySet()){
				double mass = aveMap.get(key).getFirst();
				double freq = aveMap.get(key).getSecond();

				if(freq>0){
					double orderSize = MyOrderPolicyAdjustor.generateOrderSize(mass, freq);

					if(orderSize > 50){

						double longi = customerMap.get(key.getFirst()).getFirst().getX();
						double lati = customerMap.get(key.getFirst()).getFirst().getY();
						double earliestStart = customerMap.get(key.getFirst()).getSecond().getFirst();
						double latestEnd = customerMap.get(key.getFirst()).getSecond().getSecond();

						bw.write(key.getFirst() + "," + longi + "," + lati + "," + key.getSecond() + "," + orderSize + "," + 0 + ",300," + earliestStart + "," + latestEnd);
						bw.newLine();
					}
				}
			}

		} catch (IOException e) {
			log.error("Couldn't write to file");
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				log.error("Couldn't close writer");
			}
		}
	}

	public void writeToFile(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("customer,product,aveMass,aveFreq");
			bw.newLine();

			for(Tuple<String, String> key : aveMap.keySet()){
				bw.write(key.getFirst() + "," + key.getSecond() + "," + String.format("%.2f", aveMap.get(key).getFirst()) + "," + String.format("%.2f", aveMap.get(key).getSecond()));
				bw.newLine();
			}

		} catch (IOException e) {
			log.error("Couldn't write to file");
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				log.error("Couldn't close buffered writer");
			}
		}
	}

	public void calcAverages(){

		for(String customer : customerMap.keySet()){
			for(String product : productList){
				List<Double> listAllMass = new ArrayList<Double>();
				List<Integer> listAllFreq = new ArrayList<Integer>();

				for(int i = 0; i < listOfWeekMaps.size(); i++){
					weekMap = listOfWeekMaps.get(i);

					Tuple<String, String> key = new Tuple<String, String>(customer, product);

					if(weekMap.containsKey(key)){
						double totalWeekMass = 0;
						List<Double> listOfMass = weekMap.get(key);

						for(double massValue : listOfMass){
							totalWeekMass += massValue;
						}

						listAllMass.add(totalWeekMass);
						listAllFreq.add(listOfMass.size());						
					}
				}

				double totalMass = 0;
				for(double value : listAllMass){
					totalMass += value;
				}

				double aveMass = 0;
				if(listAllMass.size()>0){
					aveMass = (totalMass/listAllMass.size());
				}

				double totalFreq = 0;
				for(double value : listAllFreq){
					totalFreq += value;
				}

				double aveFreq = 0;
				if(listAllFreq.size()>0){
					aveFreq = (totalFreq/listAllFreq.size());
				}

				aveMap.put(new Tuple<String, String>(customer, product), new Tuple<Double, Double>(aveMass, aveFreq));
			}
		}
	}

	public void parseDemand(String inputFile){
		BufferedReader br = IOUtils.getBufferedReader(inputFile);

		try {
			br.readLine(); //skip header
			String line;

			int startDay = 2;
			int weekNumber = 1;
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

				if(customerMap.containsKey(customer) && productList.contains(product)){
					if(seqDay >= startDay && seqDay <= startDay+4){
						//add to map
						Tuple<String, String> key = new Tuple<String, String>(customer, product);

						if(weekMap.containsKey(key)){
							List<Double> massList = weekMap.get(key);
							massList.add(mass);
							weekMap.put(key, massList);
						}else{
							List<Double> massList = new ArrayList<Double>();
							massList.add(mass);
							weekMap.put(key, massList);
						}

					}else{
						//increment week number
						weekNumber++;
						//increment startDay
						startDay = startDay + 5;

						//add map to list
						listOfWeekMaps.add(weekMap);

						//create new map
						weekMap = new LinkedHashMap<Tuple<String,String>, List<Double>>();

						//create new week map entry
						Tuple<String, String> key = new Tuple<String, String>(customer, product);

						List<Double> massList = new ArrayList<Double>();
						massList.add(mass);
						weekMap.put(key, massList);
					}

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
		}
	}
}
