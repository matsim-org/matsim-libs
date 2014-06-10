package playground.balac.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;



public class CustomersPerIteration {

	
	private int numberOfRetailers = 2;
	
	private int numberOfFirstRetailer = 28;
	private int numberOfSecondRetailer = 18;
	private int[] totalCustomers;
	private int numberOfIterations = 60;
	/**
	 * @param args
	 * @throws IOException 
	 */
	
	public void convertTable() throws IOException {
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/" + "CustomersPerIterration.txt");
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary");
		
		readLink.readLine();
		for(int j = 1; j <= numberOfIterations; j++) {
			totalCustomers = new int[numberOfRetailers];
			for(int i = 0; i < numberOfFirstRetailer; i++) {
			
				String s = readLink.readLine();
				String[] arr = s.split("\t");
			
				totalCustomers[0] += Integer.parseInt(arr[5]);
			}
			for(int i = 0; i < numberOfSecondRetailer; i++) {
			
				String s = readLink.readLine();
				String[] arr = s.split("\t");
			
				totalCustomers[1] += Integer.parseInt(arr[5]);
			}
		/*	outLink.write(j * 2 + ". interation" + "\t");
			outLink.write(Integer.toString(totalCustomers[0]));
			outLink.newLine();
			outLink.write(j * 2 + ". interation" + "\t");
			outLink.write(Integer.toString(totalCustomers[1]));
			outLink.newLine();*/
			//outLink.write( j * 2 + "\t");
			
			outLink.write(Integer.toString(totalCustomers[1]+totalCustomers[0]));
			outLink.newLine();
			
			
		}
		outLink.flush();
		outLink.close();
		
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		CustomersPerIteration c = new CustomersPerIteration();
		c.convertTable();
		

	}

}
