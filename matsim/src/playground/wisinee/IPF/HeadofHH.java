package playground.wisinee.IPF;

import java.io.*;
import java.util.ArrayList;

	public class HeadofHH {

		public static void main(String[] args){	
			System.out.println("Collecting information on survey data....");
			InputSurveyData1 io1 = new InputSurveyData1();
			io1.inputData();	
			System.out.println("Finish reading information on survey data!!");
			System.out.println("Start reading information from Insee....");
		try{
			File inFile = new File("./input/MenInsee_income_NP.txt");			
			BufferedReader in = new BufferedReader(new FileReader(inFile));
			File outFile = new File("./input/MenInsee_income_NP_Age_Sex1.txt");	
			FileOutputStream out = new FileOutputStream(outFile);			
			
			
			String iris = "", irisOld = "";
			String age = "";
			String np = "";
			String statusHead = "";
			int a = 0, p = 0,j=0;
			int[][][] binPP = new int [5][5][5];

			GlobalVars.fixedR = new double[125];
			int lastRowData = 662249;
			int z = 0;
			String orgData = "";
			
			GlobalVars.orgn = new ArrayList<OriginalData>();
			
			
			String s1, value;
			int col = 0;
			int row = 1;
			
			s1 = in.readLine();
			new PrintStream(out).println(s1+";"+"SexHead"+";"+"AgeGroupHead"+";"+"AgeHead");
			while ((s1= in.readLine()) != null){
				orgData = s1;
				int found = -2;
				while (found != -1){
					found = s1.indexOf(";");
					if (found != -1) {
						value = s1.substring(0,found);
						s1 = s1.substring(found+1);						
					}
					else{
						value = s1;
					}
					col = col+1;
					switch (col){
					case 4: iris = value;
					case 6: age = value;
					case 7:	np = value;
					case 10: statusHead = value;
					}					
				}				
				if (iris.compareTo(irisOld)!= 0 && irisOld.compareTo("")!= 0 || row == lastRowData){
					if (row == lastRowData){
						double np1 = Double.parseDouble(np);
						age = age.substring(1,6);
						a = ageGroup(age);
						p = noPersonGroup(np1);
						statusHead = statusHead.substring(1,7);
						j = status(statusHead);
						binPP[a][p][j] = binPP[a][p][j] + 1;
						OriginalData data1 = new OriginalData();
						data1.data = orgData;
						data1.ageGroup = a;
						data1.nopersonGroup = p;
						data1.statusofHead = j;
						GlobalVars.orgn.add(data1);
					}
									
					int n = 0;
					
					for (a = 0; a<5; a++){
						for (p = 0; p<5; p++){
							for (j=0;j<5;j++){
							GlobalVars.fixedR[n]=binPP[a][p][j];
							System.out.println(GlobalVars.fixedR[n]);
							n++;
							}
						}
					}
		
					z = z+1;
					System.out
							.println("Finish collecting data for zone : (" + z +") " + irisOld);
					//get fixed value for column
					System.out.println("Start reading pdf data....");
					InputPdf1 io3 = new InputPdf1();
					io3.inputAgeSexDistribution(z);
					System.out.println("Finish reading pdf data!!");
					System.out.println("Start IPF calculation....");
					//call ipf for each iris	
					Ipf i1 = new Ipf();
					i1.setFixRow(GlobalVars.fixedR, 125);
					i1.setFixColumn(GlobalVars.fixedC, 42);
					i1.setInitialMatrix(GlobalVars.intialRij, 125, 42);
					GlobalVars.finalRij=i1.ipfcal(125, 42);	
				
					System.out.println("Finish IPF calculation!!");
					System.out.println("Start link IPF result to Insee database....");									
					//link back to each HH
					LinktoInsee1 link = new LinktoInsee1();
					link.link(GlobalVars.orgn.size());
					System.out.println("Finish link to Insee for zone : (" + z
							+ ") " + irisOld);
					//print final data to file
					System.out.println("Start writing final result to file....");					
					for (int x = 0; x<GlobalVars.orgn.size();x++){
						new PrintStream(out).println(GlobalVars.finalData[x]);
					}
					System.out.println("Finish for zone : ("+z+")"+irisOld);
					//clear temporary data;
					binPP = new int[5][5][5];
					GlobalVars.orgn.clear();
//					if (z==2) break;
					if (z==776) System.out.println("Finish for all zones!!");
				}	
				if (row < lastRowData){
					double np1 = Double.parseDouble(np);
					age = age.substring(1,6);
					a = ageGroup(age);
					p = noPersonGroup(np1);
					statusHead = statusHead.substring(1,7);
					j = status(statusHead);
					binPP[a][p][j] = binPP[a][p][j] + 1;
					OriginalData data1 = new OriginalData();
					data1.data = orgData;
					data1.ageGroup = a;
					data1.nopersonGroup = p;
					data1.statusofHead = j;
					GlobalVars.orgn.add(data1);
					
					row = row +1;
					col = 0;
					irisOld = iris;
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
	private static int ageGroup(String a){
		int a1 = -1;
		if (a.compareTo("moins")==0) a1 = 0; 
		if (a.compareTo("30-45")==0) a1 = 1; 
		if (a.compareTo("45-60")==0) a1 = 2; 
		if (a.compareTo("60-75")==0) a1 = 3; 
		if (a.compareTo("75plu")==0) a1 = 4; 
		return a1;
	}
	private static int noPersonGroup(double np){
		int p = 0;
		if (np == 1) p = 0;
		if (np == 2) p = 1;
		if (np == 3) p = 2;
		if (np == 4) p = 3;
		if (np > 5) p = 4;
		return p;
	}
	private static int status (String statusHead){
		int j = 0;
		if (statusHead.compareTo("Tps-Pl")==0) j = 0;
		if (statusHead.compareTo("Tps-Pa")==0) j = 1;
		if (statusHead.compareTo("chomeu")==0) j = 2;
		if (statusHead.compareTo("Retrai")==0) j = 3;
		if (statusHead.compareTo("Inacti")==0) j = 4;
		return j;
	}

}
