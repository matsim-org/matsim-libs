
package ageSexHeadHH;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import globalVariables.GlobalVars;
import globalVariables.OriginalData;
import ipf.Ipf;


public class InputSurveyData1 {
	
	public void inputData(){
		// 5 age group of chef of HH, 5 group of no people in HH, 2 sex, 21 age group of people in HH
		double[][][][][] dataBin = new double[5][5][5][2][21]; 
		try{
			String s1, value;
			int col = 0;
			int row = 0;			
			String ageChefGroup = "",npGroup = "";
			String  sexHead = "", ageHead = "";
			String occ = "", pcs = "";
			String  agePP = "", sexPP = "";
			int a = 0, s = 0, p = 0, ap = 0,h=0,j=0;
			

			
			
			File inFile = new File("Personne06_1.csv");			
			BufferedReader in = new BufferedReader(new FileReader(inFile));
						
			s1= in.readLine();		//read out the data heading	
			while ((s1 = in.readLine()) != null){
				col = 0;
				row = row +1;
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
					switch (col){
					case 2: sexHead = value;
					case 3: ageChefGroup = value;
					case 4: ageHead = value;
					case 5:	npGroup = value;
					case 17: sexPP = value;
					case 18: agePP = value;
					case 23: occ = value;
					case 25: pcs = value;
					}			
				}
				if (sexHead.compareTo(sexPP)==0&&ageHead.compareTo(agePP)==0)
					h = 1; // indicate the head of HH
				else h = 0;
				a = Integer.parseInt(ageChefGroup);
				p = Integer.parseInt(npGroup);
				s = Integer.parseInt(sexHead);
				ap = Integer.parseInt(ageHead);
				ap = ageGroup(ap);
				j = jobstatus(occ);
				if (h==1&& j<=5 ){
				dataBin[a-1][p-1][j-1][s-1][ap] = dataBin[a-1][p-1][j-1][s-1][ap] + 1;
				}
//				System.out.println(row);
			}
			in.close();
			GlobalVars.intialRij = new double[125][42];	
			for (a =0; a<5;a++){
				for (p=0;p<5;p++){
					for(j=0;j<5;j++){
					for (s=0;s<2;s++){
						for (ap=0;ap<21;ap++){
							if (dataBin[a][p][j][s][ap]==0) dataBin[a][p][j][s][ap]=0.2;
							if (a==0)  {
								if (ap<3||ap>=6) dataBin[a][p][j][s][ap]= 0;
							}
							if (a==1)  {
								if (ap<6||ap>=9) dataBin[a][p][j][s][ap]= 0;
							}
							if (a==2)  {
								if (ap<9||ap>=12) dataBin[a][p][j][s][ap]= 0;
							}
							if (a==3)  {
								if (ap<12||ap>=15) dataBin[a][p][j][s][ap]= 0;
							}
							if (a==4)  {
								if (ap<15) {
									dataBin[a][p][j][s][ap]= 0;
								}
								if (j<=2&&ap>=16){
									dataBin[a][p][j][s][ap]= 0;
								}
							}
							GlobalVars.intialRij[a*25+p*5+j][s*21+ap]=dataBin[a][p][j][s][ap];	
						}
					}
					}
				}
			}
			for (int i=0;i<125;i++){
				for (int k = 0;k<42;k++){
					System.out.print(GlobalVars.intialRij[i][k]);
					System.out.print('\t');
				}
				System.out.println();
			}

		} catch(EOFException e){
			System.out.println("End of stream");
		} catch (IOException e){
			System.out.println(e.getMessage());
		}
	}	

	private int ageGroup(int age){
		int group = 0;
		int[] ageClass = new int[21];
		ageClass[0] = 4;
		
		for (int g = 1; g<20;g++){
			ageClass[g] = ageClass[g-1]+5;
		}
		for (int g = 0; g<20;g++){
			if (age<=ageClass[g]){
				group = g; 
				break;
			}
		}
		if (age>ageClass[19]) group = 20;
		return group;
	}
	private int jobstatus(String occ){
		int j = 0, occ1=0;
		if (occ.compareTo("")!=0)
		occ1 = Integer.parseInt(occ);
		else occ1 = 0;
		if (occ1==0) j = 6;
		if (occ1==1) j = 1; //full time
		if (occ1==2) j = 2; //part time
		if (occ1==6) j = 3; //unemployed
		if (occ1==7) j = 4; //retire		
		if (occ1==3||occ1==4||occ1==5||occ1==8||occ1==9) j = 5; //inactif
		return j;
	}
}
