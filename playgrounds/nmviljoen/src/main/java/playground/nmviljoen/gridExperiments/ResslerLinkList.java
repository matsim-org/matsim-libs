package playground.nmviljoen.gridExperiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ResslerLinkList {
	public static int[][] linkList(int row, int col, String filename){
		//setting structure
		// A Ressler network is a star-network with (n-1) nodes connected to a single hub (bi-directional connections)
		int n = 12;
		//calculating size of the link list and boundary values
		int links =(n-1)*2;
		int [][] linkList = new int[links][3];
		
		int count=0;

			for (int k = 2;k<=n;k++){
//				System.out.println(k);
				linkList[count][0] = 1;
				linkList[count][1] = k;
				linkList[count][2] = 1;
				count++;
				linkList[count][0] = k;
				linkList[count][1] = 1;
				linkList[count][2] = 1;
				count++;
			}

		
			try {
				File file = new File(filename);
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (int r =0;r<links;r++) {
					bw.write(String.format("%d,%d,%d\n",linkList[r][0],linkList[r][1],linkList[r][2]));
				}
				bw.close();
				System.out.println("Ressler LinkList written to file "+filename);

			} catch (IOException e) {
				e.printStackTrace();
			}
			

			return linkList;
	}
}
