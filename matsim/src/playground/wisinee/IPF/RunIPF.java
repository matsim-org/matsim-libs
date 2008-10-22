package playground.wisinee.IPF;

import java.io.*;
import java.util.ArrayList;

	public class RunIPF {

		public static void main(String[] args){	
			
//==========SETTING=============================================================			
//----------File name-----------------------------------------------------------
//			initial Household data
			File inFile1 = new File("./input/HHData.txt");	
////			initial pattern data (initial matrix) 
//			File inFile2 = new File("./input/SurveyData.csv");		
////			given distribution (fixed column)
//			File inFile3 = new File("./input/IncomeDistb.csv");	
//			final output file 
			File outFile = new File("./output/HHData_income.txt");	
//-------------------------------------------------------------------------------			
//----------Parameter------------------------------------------------------------	
//			Setting column separator
			String spt = ",";
//			number of independent variables and number of categories for each of the independent variables
			int nx = 3;    						//number of independent variables
//			----------------------------------------------------------------------
			int[]ncx = new int[nx];	
			ncx[0] = 5;							//number of categories for each of 
			ncx[1] = 5;							//independent variables respectively
			ncx[2] = 3;							//
//			----------------------------------------------------------------------
			int[]xCol = new int[nx];			
			xCol[0]= 5;						//identify which column in the household and survey data
			xCol[1]= 4;						//for each independent variable
			xCol[2]= 3;						
//			----------------------------------------------------------------------
			int zoneCol=2;						//identify which column in the household and survey data
												//for indicating zone where the household is located
//			----------------------------------------------------------------------
			int nRow = 75; 						//total rows = 5*5*3 = 75
//			----------------------------------------------------------------------
//			identify which column in the survey data for the variable to be generated
			int yCol=6;							
//			----------------------------------------------------------------------
			int[] bin = new int[nRow];	
//			number of categories of the generated variable
			int nCol = 10;
//			identify how many rows in the Household list (not include heading)
			int lastRowData = 1272;
//			heading for the generated column
			String heading = "Income";
//			number of zones
			int nz = 3;
//			small value of the zero cells of the initial matrix
			double sValue=0.1;
//--------------------------------------------------------------------------------	
//================================================================================		
			System.out.println("===============================================");
			System.out.println("Collecting information from 'Survey data'");
			InputSurveyData1 io1 = new InputSurveyData1();
			io1.inputData(nx,ncx,nRow,nCol,spt,xCol,yCol,sValue);	
			System.out.println("Finish reading information from 'Survey data'");
			System.out.println("===============================================");
			System.out.println("Start reading information from 'Household data'");
		try{
	
			BufferedReader in = new BufferedReader(new FileReader(inFile1));
			FileOutputStream out = new FileOutputStream(outFile);	
			
			String zone = "", zoneOld = "";
			int[] x = new int[nx];
			GlobalVars.fixedR = new double[nRow];

			int z = 0;
			String orgData = "";			
			GlobalVars.orgn = new ArrayList<OriginalData>();
			
			
			String s1, value;
			int col = 0;
			int row = 1;
			int pos = 0;
			int sumncx=1;
			
			s1 = in.readLine();
			new PrintStream(out).println(s1+spt+heading);
			while ((s1= in.readLine()) != null){
				orgData = s1;
				int found = -2;
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
					
					if (col==zoneCol) zone=value;
					for(int c=0;c<nx;c++){
						if (col==xCol[c]) x[c]=Integer.parseInt(value)-1;
					}				
				}				
				if (zone.compareTo(zoneOld)!= 0 && zoneOld.compareTo("")!= 0 || row == lastRowData){
					if (row == lastRowData){
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
						bin[pos] = bin[pos] + 1;
						OriginalData data= new OriginalData();
						data.data = orgData;
						for(int i=0;i<nx;i++){
							data.dataCol[i]= x[i];
						}
						GlobalVars.orgn.add(data);
					}
									
					int n = 0;		
					for (int i=0;i<nRow;i++){
						GlobalVars.fixedR[n]= bin[i];
						System.out.println(GlobalVars.fixedR[n]);                          
						n++;
					}
				
					z = z+1;
					System.out
							.println("Finish collecting data for zone : (" + z +") " + zoneOld);
					System.out.println("===============================================");
					//get fixed value for column
					System.out.println("Start reading 'Pdf data'");
					InputPdf1 io3 = new InputPdf1();
					io3.inputDistribution(z,nCol,spt);
					System.out.println("Finish reading 'Pdf data'");
					System.out.println("===============================================");
					
					//call ipf for each zone
					System.out.println("Start IPF calculation....");			
					Ipf i1 = new Ipf();
					i1.setFixRow(GlobalVars.fixedR, nRow);
					i1.setFixColumn(GlobalVars.fixedC, nCol);
					i1.setInitialMatrix(GlobalVars.initialRij, nRow, nCol);
					GlobalVars.finalRij=i1.ipfcal(nRow, nCol);					
					System.out.println("Finish IPF calculation!!");
					System.out.println("===============================================");
					
					System.out.println("Start link IPF result to 'Household data'....");				
					//link back to each HH
					LinktoInsee1 link = new LinktoInsee1();
					link.link(GlobalVars.orgn.size(),nx,ncx,nRow,nCol,spt);
					System.out.println("Finish link to Insee for zone : (" + z
							+ ") " + zoneOld);
					//print final data to file
					System.out.println("Start writing final result to file....");					
					for (int i = 0; i<GlobalVars.orgn.size();i++){
						new PrintStream(out).println(GlobalVars.finalData[i]);
					}
					System.out.println("Finish for zone : ("+z+")"+zoneOld);
					System.out.println("===============================================");
					System.out.println("===============================================");
					//clear temporary data;
					bin = new int[nRow];
					GlobalVars.orgn.clear();
					if (z==nz) System.out.println("Finish for all zones!!");
				}	
				if (row < lastRowData){
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
					bin[pos] = bin[pos] + 1;
					OriginalData data= new OriginalData();
					data.data = orgData;
					for(int i=0;i<nx;i++){
						data.dataCol[i]= x[i];
					}
					GlobalVars.orgn.add(data);
					
					row = row +1;
					col = 0;
					zoneOld = zone;
				}				
			}
			in.close();
			out.close();
		} catch(EOFException e){
			System.out.println("End of stream");
		} catch (IOException e){
			System.out.println(e.getMessage());				
		}
	}

}
