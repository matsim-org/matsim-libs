package playground.southafrica.sandboxes.qvanheerden.freight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.filesampler.MyFileFilter;
import playground.southafrica.utilities.filesampler.MyFileSampler;

public class MyDemandAnalyser {

	public static Logger log = Logger.getLogger(MyDemandAnalyser.class); 
	public static Map<String, Id<Person>> customerMap = new HashMap<String, Id<Person>>();
	
	public static void main(String[] args) {
		Header.printHeader(MyDemandAnalyser.class.toString(), args);

		String demandFileDir = args[0];
		String analysisOutputDir = args[1];
		String customerFile = args[2];
		
		MyDemandAnalyser.parseCustomerDetails(customerFile);
		
		MyFileSampler mfs = new MyFileSampler(demandFileDir);
		List<File> fileList = mfs.sampleFiles(Integer.MAX_VALUE, new MyFileFilter(".csv"));
		
		for(File file : fileList){
			String name = file.getName().substring(0, file.getName().indexOf("."));
			String day = file.getName().substring(file.getName().indexOf(".")-1, file.getName().indexOf("."));
			Map<String, List<Double>> map = MyDemandAnalyser.parseDemand(file.getAbsolutePath());
			MyDemandAnalyser.writeDemandAnalysisToFile(analysisOutputDir + name + "_" + day + "_analysis.csv", map);
		}
		
		Header.printFooter();
	}
	
	public static void parseCustomerDetails(String customerFile){
		BufferedReader br = IOUtils.getBufferedReader(customerFile);
		
		try {
			br.readLine(); //skip header

			String line;
			while((line=br.readLine())!=null){
				String[] array = line.split(",");
				Id<Person> id = Id.create(array[0], Person.class);
				String customer = array[1];

				customerMap.put(customer, id);

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
	}

	/**
	 * Method that parses the CSV file containing demand data in the format:
	 * <br><br>
	 * customerName, long, lat, product, mass, sale, duration, start, end
	 * <br><br>
	 * Where each field is:
	 * <ul>
	 * <li> customer: Name of the customer
	 * <li> long: longitude (take care to use same CRS as network)
	 * <li> lat: latitude (take care to use same CRS as network)
	 * <li> product: Product to deliver
	 * <li> mass: mass (in kg) of product
	 * <li> sale: sales value (here in R, but could be any currency)
	 * <li> duration: duration of the off-loading activity (in seconds)
	 * <li> start: earliest start of the time window (in seconds)
	 * <li> end: latest end of the time window (in seconds)
	 * @param demandFile
	 */
	public static Map<String, List<Double>> parseDemand(String demandFile){
		BufferedReader br = IOUtils.getBufferedReader(demandFile);
		Map<String, List<Double>> orderMap = new HashMap<String, List<Double>>();

		try {
			br.readLine();//skip header

			String input;
			int i = 1;
			while((input = br.readLine()) != null){
				String[] array = input.split(",");

				String customer = array[0];
				double longi = Double.parseDouble(array[1]);
				double lati = Double.parseDouble(array[2]);
				String product = array[3];
				double mass = Double.parseDouble(array[4]);
				double sale = Double.parseDouble(array[5]);
				double duration = Double.parseDouble(array[6]);
				double start = Double.parseDouble(array[7]);
				double end = Double.parseDouble(array[8]);

				List<Double> list = new ArrayList<Double>();
				if(orderMap.containsKey(customer)){
					List<Double> tempList = orderMap.get(customer);
					orderMap.get(customer).set(2, tempList.get(2)+mass);
					orderMap.get(customer).set(3, tempList.get(3)+sale);
					orderMap.get(customer).set(4, tempList.get(4)+1.0);
				}else{
					list = new ArrayList<Double>();
					list.add(longi);
					list.add(lati);
					list.add(mass);
					list.add(sale);
					list.add(1.0);
					orderMap.put(customer, list);
				}

				i++;
			}


		} catch (IOException e) {
			log.error("Could not read demand file");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				log.error("Could not close demand file");
			}
		}
		return orderMap;
	}

	public static void writeDemandAnalysisToFile(String demandOutput, Map<String, List<Double>> orderMap){
		BufferedWriter bw = IOUtils.getBufferedWriter(demandOutput);

		try {
			bw.write("id, customer, long, lat, mass, sales, orders");
			bw.newLine();
			for(String cust : orderMap.keySet()){
				List<Double> list = orderMap.get(cust);
				Id<Person> custId = customerMap.get(cust);
				
				bw.write(
						custId.toString() + "," +
						cust + "," +
						list.get(0) +  "," +
						list.get(1) +  "," +
						String.format("%.2f", list.get(2)) + "," +
						String.format("%.2f", list.get(3)) + "," +
						String.format("%.0f", list.get(4))
						);
				bw.newLine();
			}
			
		} catch (IOException e) {
			log.error("Could not read from file");
		} finally{
			
			try {
				bw.close();
			} catch (IOException e) {
				log.info("Could not close writer");
			}
			
		}
	}
}
