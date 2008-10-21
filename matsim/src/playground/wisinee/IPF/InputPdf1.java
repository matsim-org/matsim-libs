
package playground.wisinee.IPF;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InputPdf1 {
	
	public void inputDistribution(int z, int nCol, String spt){
			GlobalVars.fixedC = new double[nCol];
			GlobalVars.fixedC = inputData(z,nCol,spt);	
			for (int i=0;i<nCol;i++){
				System.out.println(GlobalVars.fixedC[i]);
			}
	}
		
		private double[] inputData(int z, int nCol, String spt){
			double[] fixedPdf = new double[nCol];
			try{
				String s1, value;
				int col = 0;
							
				File inFile = new File("./input/Pdf.txt");			
				BufferedReader in = new BufferedReader(new FileReader(inFile));			
				s1= in.readLine();		//read out the data heading	
				for (int n = 1; n < z; n++){
					s1= in.readLine();
				}	
					s1 = in.readLine();
					int found = -2 ;
					while (found != -1){
						found = s1.indexOf(spt);
						if (found != -1) {
							value = s1.substring(0,found);
							s1 = s1.substring(found+1);						
						}
						else{
							value = s1;
						}
						col = col+1;
						if (col >= 3 && col <= nCol+2) fixedPdf[col-3] = Double.parseDouble(value);					
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


