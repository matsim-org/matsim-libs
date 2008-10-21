
package playground.wisinee.IPF;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InputSurveyData1 {
	
	public void inputData(int nx, int[]ncx, int nRow, int nCol,String spt){
		GlobalVars.initialRij = new double[nRow][nCol];	
		try{
			String s1, value; 
			int col = 0;
			int row = 0;			
			int pos = 0;
			int[] x = new int[nx];
			int y = 0;
			int sumncx=1;
			double ny=0;
			File inFile = new File("./input/Survey.txt");			
			BufferedReader in = new BufferedReader(new FileReader(inFile));
						
			s1= in.readLine();		//read out the data heading	
			while ((s1 = in.readLine()) != null){
				col = 0;
				row = row +1;
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
					switch (col){
					case 1: x[2] = Integer.parseInt(value)-1; break;
					case 2: x[1] = Integer.parseInt(value)-1; break;
					case 3: x[0] = Integer.parseInt(value)-1; break;
					case 4: y = Integer.parseInt(value)-1; break;
					case 5: ny = Double.parseDouble(value); break;
					}			
				}
				for (int i=0;i<nx;i++){
					if(i==0){
						pos = x[i];
					}
					else{
						for (int j=i;j>=1;j--){
							sumncx = sumncx*ncx[j-1];
						}
						pos = pos+x[i]*sumncx;
						sumncx=1;
					}									
				}					
				GlobalVars.initialRij[pos][y] =GlobalVars.initialRij[pos][y]+ny;					
			}
			in.close();			
		} catch(EOFException e){
			System.out.println("End of stream");
		} catch (IOException e){
			System.out.println(e.getMessage());
		}
	}	

}
