package playground.wrashid.PSF.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.StringTokenizer;

/*
 * - The file is split into 15 minute bins, this means we have 96 entries in the file.
 * - numbering of hubs starts with zero
 * - first value is for 0 to 15.
 */
public class HubPriceInfo {

	// first index: , second index: price (max 96 entries, for the 15 min bins)
	double hubPrice[][];

	public HubPriceInfo(String fileName, int numberOfHubs) {
		//this.numberOfHubs = numberOfHubs;
		hubPrice=new double[numberOfHubs][96];
		
		try {
		
		FileReader fr = new FileReader(fileName);
		
		BufferedReader br = new BufferedReader(fr);
		String line;
		StringTokenizer tokenizer;
		String token;
		line = br.readLine();
		int rowId=0;
		while (line != null) {
			tokenizer = new StringTokenizer(line);
			
			for (int i=0;i<numberOfHubs;i++){
				token = tokenizer.nextToken();
				double parsedNumber=Double.parseDouble(token);
				hubPrice[i][rowId]=Double.parseDouble(token);
			}
			
			if (tokenizer.hasMoreTokens()){
				// if there are more columns than expected, throw an exception
				
				throw new RuntimeException("the number of hubs is wrong");
			}
			
			line = br.readLine();
			rowId++;
		}
		
		if (rowId!=96){
			throw new RuntimeException("the number of rows is wrong");
		}
		
		} catch (RuntimeException e){
			// just forward the runtime exception
			throw e;
		} catch (Exception e){
			throw new RuntimeException("Error reading the hub link mapping file");
		}
		
	}

	/**
	 * time: time of day in seconds
	 * hub
	 * @param time
	 * @param hubNumber
	 */
	public double getPrice(double time, int hubNumber) {
		return  hubPrice[hubNumber][(int)  Math.floor(time/(15*60))];
	}
	
	
	

}
