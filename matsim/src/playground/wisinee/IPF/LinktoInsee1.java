package playground.wisinee.IPF;

import java.util.Random;



public class LinktoInsee1 {
	public double[] totalPP, usedPP;
	public double[][][][]countUsedPP;
	public String sex = "";
	public String age = "";
	public int agePPGroup =0;
	
	public void link(int n){
		countUsedPP = new double[5][5][5][42];
		totalPP = new double[43];
		usedPP = new double[43];
		GlobalVars.finalData = new String[n];
		
		int a = 0, p = 0,j=0;
		int selectI = 0;
		String data ="";
		
		for (int x = 0; x<n; x++){
			//get information on age, np group, np
			data = GlobalVars.orgn.get(x).data;
			a  = GlobalVars.orgn.get(x).ageGroup;
			p  = GlobalVars.orgn.get(x).nopersonGroup;
			j = GlobalVars.orgn.get(x).statusofHead;
			System.out.print(a);
			System.out.print('\t');
			System.out.print(p);
			System.out.print('\t');
			System.out.println(j);
			
			//calculation
			
				for (int i = 0; i <42; i++){
					totalPP[i+1] = GlobalVars.finalRij[a*25+p*5+j][i];
					usedPP[i+1] = countUsedPP[a][p][j][i];
				}
				
				selectI = randomPP(); 
				countUsedPP[a][p][j][selectI-1] = countUsedPP[a][p][j][selectI-1] + 1;
				GroupName(selectI);
				agePPGroup = selectI;
				if (selectI>21)agePPGroup = selectI-21;
				//prepare final data for write to file	
				GlobalVars.finalData[x] = data+";"+sex+";"+agePPGroup+";"+age;	
		}
		
	}
	
	
	private int randomPP(){
		double sumPP = 0, randomNum = 0;				
		int selectPP = 0;
		double[] cdfPP = new double[43];
		double[] pp = new double[43];		
		Random rand = new Random();
		
		for (int i = 1; i<=42; i++){
			pp[i] = totalPP[i] - usedPP[i];
			if (pp[i] < 1) pp[i] = 0;
				sumPP = sumPP + pp[i];							
		}
		
		if (sumPP < 1) {
			sumPP = 0;
			for (int i = 1; i<=42; i++){
				pp[i] = totalPP[i] - usedPP[i];
				if (pp[i]<0) pp[i]=0;
				sumPP = sumPP + pp[i];							
			}
		}
		
		cdfPP[0] = 0;
		for (int i = 1; i<=42; i++){
			cdfPP[i] = cdfPP[i - 1] + pp[i] / sumPP;				
		}	
		randomNum = rand.nextDouble();
		for (int i=1;i<=42;i++){
			if (randomNum > cdfPP[i-1] && randomNum <= cdfPP[i]){
				selectPP =i;
				break;
			}
		}
		return selectPP;
	}
	private void GroupName(int ig){
		int ig1 = 0;
		if (ig<=21) {sex = "M"; ig1 = ig;}
		else {sex = "F"; ig1 = ig-21;}		
		switch (ig1){
		case 1: age = "0-4"; break;
		case 2: age = "5-9"; break;
		case 3: age = "10-14"; break;
		case 4: age = "15-19"; break;
		case 5: age = "20-24"; break;
		case 6: age = "25-29"; break;
		case 7: age = "30-34"; break;
		case 8: age = "35-39"; break;
		case 9: age = "40-44"; break;
		case 10: age = "45-49"; break;
		case 11: age = "50-54"; break;
		case 12: age = "55-59"; break;
		case 13: age = "60-64"; break;
		case 14: age = "65-69"; break;
		case 15: age = "70-74"; break;
		case 16: age = "75-79"; break;
		case 17: age = "80-84"; break;
		case 18: age = "85-89"; break;
		case 19: age = "90-94"; break;
		case 20: age = "95-99"; break;
		case 21: age = "100+"; break;
		}
	}
	
	
}
