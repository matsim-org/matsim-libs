/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.nmviljoen.network.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class Grid {	
	public static int[][] linkList(int row, int col){
		//calculating size of the link list and boundary values
		int start = 1;
		int end = row*col;
		int [] topBound = new int[col-2];
		int [] bottomBound = new int[col-2];
		int [] rightBound = new int[row-2];
		int [] leftBound = new int[row-2];
		int [] rest = new int[end-(4+topBound.length + bottomBound.length+ leftBound.length+rightBound.length)];
		int links = row*(col-1)*2+col*(row-1)*2;
		int [][] linkList = new int[links][3];
		System.out.println(links);
		
		//populating boundary arrays
		//topBound
		int next =0;
		for (int i = start+1; i<start+col-1;i++){
			topBound[next]=i;
			next++;
		}
		//bottomBound
		next =0;
		for (int i = end-col+2; i<end;i++){
			bottomBound[next]=i;
			next++;
		}
		//rightBound
		next =0;
		for (int i = start+2*col-1; i<end;i=i+col){
			rightBound[next]=i;
			next++;
		}	
		//leftBound
		next =0;
		for (int i = start+col; i<end-col+1;i=i+col){
			leftBound[next]=i;
			next++;
		}
		//rest
		next=0;
		for(int f =col+2;f<col*(row-1);f=f+col){
			for (int c=f;c<f+col-2;c++){
				rest[next]=c;
				next++;
			}
		}
		System.out.println("Top Bound");
		for (int p = 0;p<topBound.length;p++){
			System.out.print(topBound[p]+" ");
		}
		System.out.println();
		System.out.println("Bottom Bound");
		for (int p = 0;p<bottomBound.length;p++){
			System.out.print(bottomBound[p]+" ");
		}
		System.out.println();
		System.out.println("Right Bound");
		for (int p = 0;p<rightBound.length;p++){
			System.out.print(rightBound[p]+" ");	
		}
		System.out.println();
		System.out.println("Left Bound");
		for (int p = 0;p<leftBound.length;p++){
			System.out.print(leftBound[p]+" ");
		}
		System.out.println();
		System.out.println("Rest");
		for (int p = 0;p<rest.length;p++){
			System.out.print(rest[p]+" ");
		}
		System.out.println();
		
		//assigning links
			int count = -1;
			//FOUR CORNERS
			//top left - none
			//top right - left only
			int n=start+col-1;
			//link 1
			linkList[count+1][0]=n;
			linkList[count+1][1]=n-1;
			linkList[count+1][2]=1;
			//link 2
			linkList[count+2][0]=n-1;
			linkList[count+2][1]=n;
			linkList[count+2][2]=1;
			count=count+2;

			//bottom left - up only
			n=end+1-col;
			//link 1
			linkList[count+1][0]=n;
			linkList[count+1][1]=n-col;
			linkList[count+1][2]=1;
			//link 2
			linkList[count+2][0]=n-col;
			linkList[count+2][1]=n;
			linkList[count+2][2]=1;
			count = count + 2;

			//bottom right - up and left
			n=end;
			//link 1
			linkList[count+1][0]=n;
			linkList[count+1][1]=n-1;
			linkList[count+1][2]=1;
			//link 2
			linkList[count+2][0]=n-1;
			linkList[count+2][1]=n;
			linkList[count+2][2]=1;
			//link 3
			linkList[count+3][0]=n;
			linkList[count+3][1]=n-col;
			linkList[count+3][2]=1;
			//link 4
			linkList[count+4][0]=n-col;
			linkList[count+4][1]=n;
			linkList[count+4][2]=1;
			count = count + 4;

			//TOP BOUND - only left
			for (int r =0;r<topBound.length;r++){
				n=topBound[r];
				//link 1
				linkList[count+1][0]=n;
				linkList[count+1][1]=n-1;
				linkList[count+1][2]=1;
				//link 2
				linkList[count+2][0]=n-1;
				linkList[count+2][1]=n;
				linkList[count+2][2]=1;
				count = count + 2;
			}
			//BOTTOM BOUND - up and left
			for (int r =0;r<bottomBound.length;r++){
				n=bottomBound[r];
				//link 1
				linkList[count+1][0]=n;
				linkList[count+1][1]=n-1;
				linkList[count+1][2]=1;
				//link 2
				linkList[count+2][0]=n-1;
				linkList[count+2][1]=n;
				linkList[count+2][2]=1;
				//link 3
				linkList[count+3][0]=n;
				linkList[count+3][1]=n-col;
				linkList[count+3][2]=1;
				//link 4
				linkList[count+4][0]=n-col;
				linkList[count+4][1]=n;
				linkList[count+4][2]=1;
				count = count + 4;
			}
			//LEFT BOUND - up only
			for (int r =0;r<leftBound.length;r++){
				n=leftBound[r];
				//link 1
				linkList[count+1][0]=n;
				linkList[count+1][1]=n-col;
				linkList[count+1][2]=1;
				//link 2
				linkList[count+2][0]=n-col;
				linkList[count+2][1]=n;
				linkList[count+2][2]=1;
				count = count + 2;
			}
			//RIGHT BOUND - left and up
			for (int r =0;r<rightBound.length;r++){
				n=rightBound[r];
				//link 1
				linkList[count+1][0]=n;
				linkList[count+1][1]=n-1;
				linkList[count+1][2]=1;
				//link 2
				linkList[count+2][0]=n-1;
				linkList[count+2][1]=n;
				linkList[count+2][2]=1;
				//link 3
				linkList[count+3][0]=n;
				linkList[count+3][1]=n-col;
				linkList[count+3][2]=1;
				//link 4
				linkList[count+4][0]=n-col;
				linkList[count+4][1]=n;
				linkList[count+4][2]=1;
				count = count + 4;
			}
			//REST - left and up
			for (int r =0;r<rest.length;r++){
				n=rest[r];
				//link 1
				linkList[count+1][0]=n;
				linkList[count+1][1]=n-1;
				linkList[count+1][2]=1;
				//link 2
				linkList[count+2][0]=n-1;
				linkList[count+2][1]=n;
				linkList[count+2][2]=1;
				//link 3
				linkList[count+3][0]=n;
				linkList[count+3][1]=n-col;
				linkList[count+3][2]=1;
				//link 4
				linkList[count+4][0]=n-col;
				linkList[count+4][1]=n;
				linkList[count+4][2]=1;
				count = count + 4;
			}
			try {
				File file = new File("/Users/nadiaviljoen/Documents/workspace/ArticleRegister/GridNetworkFiles/baseline50x50/linkListGrid.csv");
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (int r =0;r<links;r++) {
					bw.write(String.format("%d,%d,%d\n",linkList[r][0],linkList[r][1],linkList[r][2]));
				}
				bw.close();
				System.out.println("LinkList written");

			} catch (IOException e) {
				e.printStackTrace();
			}
			

			return linkList;
	}

}
