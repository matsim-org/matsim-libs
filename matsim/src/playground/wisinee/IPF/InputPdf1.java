
package playground.wisinee.IPF;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InputPdf1 {
	
	public void inputAgeSexDistribution(int z){
			GlobalVars.fixedC = new double[42];
			GlobalVars.fixedC = inputData(z);	
			for (int i = 0;i<42;i++){
				System.out.println(GlobalVars.fixedC[i]);
			}
	}
		
		private double[] inputData(int z){
			double[] fixedPdf = new double[42];
			try{
				String s1, value;
				int col = 0;
							
				File inFile = new File("./input/sex_age_Distb_Head.csv");			
				BufferedReader in = new BufferedReader(new FileReader(inFile));			
				s1= in.readLine();		//read out the data heading	
				for (int n = 1; n < z; n++){
					s1= in.readLine();
				}	
					s1 = in.readLine();
					int found = -2 ;
					while (found != -1){
						found = s1.indexOf(",");
						if (found != -1) {
							value = s1.substring(0,found);
							s1 = s1.substring(found+1);						
						}
						else{
							value = s1;
						}
						col = col+1;
						if (col >= 3 && col <= 44)fixedPdf[col-3] = Double.parseDouble(value);					
					}		
				in.close();	
			} catch(EOFException e){
				System.out.println("End of stream");
			} catch (IOException e){
				System.out.println(e.getMessage());
			}
			return fixedPdf;
		}
		
	}


