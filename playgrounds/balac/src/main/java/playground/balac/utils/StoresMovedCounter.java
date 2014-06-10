package playground.balac.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.core.utils.io.IOUtils;

public class StoresMovedCounter {

	/**
	 * @param args
	 */
	
	
	BufferedReader readLink_1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_noret_new");
	BufferedReader readLink_2 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_ret_new");
	
	private int numberOfFirstRetailer = 28;
	private int numberOfSecondRetailer = 18;
	
	ArrayList<String> c = new ArrayList<String>();
	
	public void countMovedRetailers() throws IOException {
		
		
		//readLink_1.readLine();
		//readLink_2.readLine();
		int numberOfMovedRetailer1 = 0;
		int numberOfMovedRetailer2 = 0;
			for(int i = 0; i < numberOfFirstRetailer; i++) {
			
				String s1 = readLink_1.readLine();
				String[] arr1 = s1.split("\t");
				String s2 = readLink_2.readLine();
				String[] arr2 = s2.split("\t");
				c.add(arr1[4]);
				
			}
			
			for(int i = 0; i < numberOfSecondRetailer; i++) {
				
				String s1 = readLink_1.readLine();
				String[] arr1 = s1.split("\t");
				String s2 = readLink_2.readLine();
				String[] arr2 = s2.split("\t");
				c.add(arr1[4]);
				
			}
			
			readLink_1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_noret_new");
			readLink_2 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_ret_new");
			
			
			for(int i = 0; i < numberOfFirstRetailer; i++) {
				
				String s1 = readLink_1.readLine();
				String[] arr1 = s1.split("\t");
				String s2 = readLink_2.readLine();
				String[] arr2 = s2.split("\t");
				
				if (!arr1[2].equals(arr2[2]) || !arr1[3].equals(arr2[3])) 
					if ( !c.contains(arr2[4]))
					numberOfMovedRetailer1 += 1;
			}
			
			for(int i = 0; i < numberOfSecondRetailer; i++) {
				
				String s1 = readLink_1.readLine();
				String[] arr1 = s1.split("\t");
				String s2 = readLink_2.readLine();
				String[] arr2 = s2.split("\t");
				
				if (!arr1[2].equals(arr2[2]) || !arr1[3].equals(arr2[3])) 
					if ( !c.contains(arr2[4]))
					numberOfMovedRetailer2 += 1;
			}
			
			readLink_1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_noret_new");
			readLink_2 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_ret_new");
			
			int decreased = 0;
			int increased = 0;
			for(int i = 0; i < numberOfFirstRetailer; i++) {
				
				String s1 = readLink_1.readLine();
				String[] arr1 = s1.split("\t");
				String s2 = readLink_2.readLine();
				String[] arr2 = s2.split("\t");
				
				if (Integer.parseInt(arr1[5]) > Integer.parseInt(arr2[5])) 
					decreased ++;
				else
					increased ++;
			}
			
			for(int i = 0; i < numberOfSecondRetailer; i++) {
				
				String s1 = readLink_1.readLine();
				String[] arr1 = s1.split("\t");
				String s2 = readLink_2.readLine();
				String[] arr2 = s2.split("\t");
				
				if (Integer.parseInt(arr1[5]) > Integer.parseInt(arr2[5])) 
					decreased ++;
				else
					increased ++;
			}
		System.out.println(numberOfMovedRetailer1 + " " + numberOfMovedRetailer2);	
		System.out.println(decreased + " " + increased);	
		
		
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		StoresMovedCounter smc = new StoresMovedCounter();
		smc.countMovedRetailers();

	}

}
