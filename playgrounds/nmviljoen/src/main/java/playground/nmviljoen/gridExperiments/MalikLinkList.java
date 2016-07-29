package playground.nmviljoen.gridExperiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MalikLinkList {
	public static int[][] linkList(int row, int col, String filename){
		//setting structure
		// A Malik network has two bi-directionally connected hubs with n nodes connected bi-directedly to the hubs
		int core = 2;
		int n = 5;
		//calculating size of the link list and boundary values
		int links =core*n*2+core*(core-1);
		int [][] linkList = new int[links][3];
		
		int count=0;
		//populate hub network
		for (int i = 0;i<core;i++){
			for (int k = 0;k<core;k++)
				if(i!=k){
					linkList[count][0] = i+1;
					linkList[count][1] = k+1;
					linkList[count][2] = 1;
				}
			count++;
		}
		
		//populate spokes
		for (int i = 0;i<core;i++){
//			System.out.println();
			for (int k = core+n*i;k<=core+n*(i+1)-1;k++){
//				System.out.println(k);
				linkList[count][0] = i+1;
				linkList[count][1] = k+1;
				linkList[count][2] = 1;
				count++;
				linkList[count][0] = k+1;
				linkList[count][1] = i+1;
				linkList[count][2] = 1;
				count++;
			}
		}
//		System.out.println(links);
		
			try {
				File file = new File(filename);
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (int r =0;r<links;r++) {
					bw.write(String.format("%d,%d,%d\n",linkList[r][0],linkList[r][1],linkList[r][2]));
				}
				bw.close();
				System.out.println("Malik LinkList written to file "+filename);

			} catch (IOException e) {
				e.printStackTrace();
			}
			

			return linkList;
	}

}
