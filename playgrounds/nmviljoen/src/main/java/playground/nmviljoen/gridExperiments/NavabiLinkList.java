package playground.nmviljoen.gridExperiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NavabiLinkList {
	public static int[][] linkList(int row, int col, String filename){
		//setting structure
		// In a Navabi network all nodes are connected to each other - there is no hub 
		int n = 12;
		//calculating size of the link list and boundary values
		int links =n*(n-1);
		int [][] linkList = new int[links][3];
		
		int count=0;

			for (int k = 1;k<=n;k++){
				for(int w =1;w<=n;w++){
					if(k != w){
//						System.out.println(k);
						linkList[count][0] = k;
						linkList[count][1] = w;
						linkList[count][2] = 1;
						count++;	
					}
				}
			}

		
			try {
				File file = new File(filename);
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (int r =0;r<links;r++) {
					bw.write(String.format("%d,%d,%d\n",linkList[r][0],linkList[r][1],linkList[r][2]));
				}
				bw.close();
				System.out.println("Navabi LinkList written to file "+filename);

			} catch (IOException e) {
				e.printStackTrace();
			}
			

			return linkList;
	}
}
