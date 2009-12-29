/* *********************************************************************** *
 * project: org.matsim.*
 * ReadEvents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.wisinee.IPF;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;

	public class RunIPF {
//		private final static String testPropertyFile = "./test/scenarios/ipf/TestParameter.xml";	
		
		
		public  String householdFile=null;
		public  String surveyFile=null;
		public  String distributionFile=null;
		public  String outputFile=null;
		public  File inFile1=null;
		public  File inFile2=null;
		public  File inFile3=null;
		public  File outFile=null;
		public  String spt=null;
		public  double sValue=0.1;
		public  int nx=1;
		public  int nz=1;
		public  String heading=null;
		public  int lastRowData=1;
		public  int nCol=1;
		public  int zoneCol=1;
		public  int yCol=1;	
		public  String ncxString=null;
		public  String xColString=null;		
		public  int maxItn =100;
		public  double convSet =0.5;
	
//			public static void main(String[] args){					
//				runIpfCal(testPropertyFile);
//			}
			
			public void runIpfCal(String PropFile){	
	
				Properties props = new Properties();
				try{
					props.loadFromXML( new FileInputStream(PropFile) );
					householdFile = props.getProperty("household File");
					surveyFile = props.getProperty("survey File");
					distributionFile = props.getProperty("distribution File");
					outputFile = props.getProperty("output File");
					spt = props.getProperty("spt");
					heading = props.getProperty("heading");
					sValue = Double.parseDouble(props.getProperty("sValue"));
					nz = Integer.parseInt(props.getProperty("nz"));
					nx = Integer.parseInt(props.getProperty("nx"));
					lastRowData = Integer.parseInt(props.getProperty("lastRowData"));
					nCol = Integer.parseInt(props.getProperty("nCol"));
					zoneCol = Integer.parseInt(props.getProperty("zoneCol"));
					yCol = Integer.parseInt(props.getProperty("yCol"));
					ncxString = props.getProperty("ncx");
					xColString = props.getProperty("xCol");
					maxItn = Integer.parseInt(props.getProperty("maxIteration"));
					convSet = Double.parseDouble(props.getProperty("convergence"));
				}
				catch( Exception xc ){
					xc.printStackTrace();
					System.exit(-1);
				}
				inFile1= new File(householdFile);
				inFile2= new File(surveyFile);
				inFile3= new File(distributionFile);
				outFile= new File(outputFile);	
				
				int[]ncx = new int[nx];
				int k=0;
				int found1 = -2;
				while (found1 != -1){
					found1 = ncxString.indexOf(",");
					if (found1 != -1) {
						ncx[k] = Integer.parseInt(ncxString.substring(0,found1));
						ncxString = ncxString.substring(found1+1);						
					}
					else{
						ncx[k] = Integer.parseInt(ncxString);
					}
				k++;
				}
				k=0;
				found1 = -2;
				int[]xCol = new int[nx];
				
				while (found1 != -1){
					found1 = xColString.indexOf(",");
					if (found1 != -1) {
						xCol[k] = Integer.parseInt(xColString.substring(0,found1));
						xColString = xColString.substring(found1+1);						
					}
					else{
						xCol[k]  = Integer.parseInt(xColString);
					}
				k++;
				}			

			int nRow=1; 						
			for (int i=0;i<nx;i++){
				nRow = nRow*ncx[i];
			}
			int[] bin = new int[nRow];			
			
			System.out.println("================================================");
			System.out.println("          +++++SETTING PARAMETERS+++++         ");
			System.out.println("================================================");			
			System.out.println("INPUT 'HOUSEHOLD/PERSON DATA' File from: \""+inFile1+"\"");
			System.out.println("INPUT 'SURVEY DATA' File from: \""+inFile2+"\"");		
			System.out.println("INPUT 'DISTRIBUTION DATA' File from: \""+inFile3+"\"");
			System.out.println("OUTPUT File to: \""+outFile+"\"");
			System.out.println();
			System.out.println("Using seperater:\""+spt+"\"");
			System.out.println();
			checkDataHeading(inFile1,inFile2,inFile3);	
			System.out.println("From 'HOUSEHOLD/PERSON DATA' and 'SURVEY DATA'");
			System.out.println("Using Column:"+zoneCol+" @to identify zone");
			System.out.println();
			System.out.println("Number of independent variables @(x):"+nx);
			for(int i=0;i<nx;i++){
				System.out.println("x"+(i+1)+" from Column:"+xCol[i]);
				System.out.println("Number of categories for x"+(i+1)+":"+ncx[i]);
			}
			System.out.println();
			System.out.println("Generated variable (y) name:"+heading);
			System.out.println("Using Column:"+yCol+" from 'SURVEY DATA' for y");	
			System.out.println("Number of categories of y:"+nCol);
			System.out.println();
			System.out.println("Number of zones:"+nz);
			System.out.println("Small value for zero cells:"+sValue);
			System.out.println("================================================");
			System.out.println("       +++++END SETTING PARAMETERS+++++      ");	
			System.out.println("================================================");
			
			
			System.out.println("Start reading information from 'SURVEY DATA'");
			InputSurveyData io1 = new InputSurveyData();
			io1.inputData(nx,ncx,nRow,nCol,spt,xCol,yCol,sValue,inFile2);	
			System.out.println("Finish.");
			System.out.println("------------------------------------------------");
			System.out.println("Start reading information from 'HOUSEHOLD/PERSON DATA'");
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
			
			s1 = in.readLine();			//readout the heading
			
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
						n++;
					}
				
					z = z+1;
					System.out
							.println("Finish. zone: (" + z +") " + zoneOld);
					//get fixed value for column
					System.out.println("Start reading 'DISTRIBUTION DATA' zone: (" + z +") " + zoneOld);
					
					InputPdf io3 = new InputPdf();
					io3.inputDistribution(z,nCol,spt,inFile3);
					System.out.println("Finish.");
					
					//call ipf for each zone
					System.out.println("Start IPF calculation....");			
					Ipf i1 = new Ipf();
					i1.setFixRow(GlobalVars.fixedR, nRow);
					i1.setFixColumn(GlobalVars.fixedC, nCol);
					i1.setInitialMatrix(GlobalVars.initialRij, nRow, nCol);
					GlobalVars.finalRij=i1.ipfcal(nRow, nCol,convSet,maxItn);					
					System.out.println("Finish.");
					
					System.out.println("Start linking IPF result to 'HOUSEHOLD/PERSON DATA'");				
					//link back to each HH
					LinktoHHlist link = new LinktoHHlist();
					link.link(GlobalVars.orgn.size(),nx,ncx,nRow,nCol,spt);
					System.out.println("Finish.");
					//print final data to file
					System.out.println("Start writing 'FINAL RESULT' to 'OUTPUT FILE'");					
					for (int i = 0; i<GlobalVars.orgn.size();i++){
						new PrintStream(out).println(GlobalVars.finalData[i]);
					}
					System.out.println("Finish. zone: ("+z+")"+zoneOld);
					System.out.println("------------------------------------------------");
					//clear temporary data;
					bin = new int[nRow];
					GlobalVars.orgn.clear();
					if (z==nz) {System.out.println("Finish all zones!!");break;} 
					System.out.println("Continue reading information from 'HOUSEHOLD/PERSON DATA'");
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
	private void checkDataHeading(File inFile1,File inFile2,File inFile3){
		try{
		BufferedReader in1 = new BufferedReader(new FileReader(inFile1));
		String s1;
		s1 = in1.readLine();	
		System.out.println("INPUT 'HOUSEHOLD/PERSON DATA'");
		System.out.println("Data Heading....");
		System.out.println(":::::::::::::::::::::::::::::::::");
		System.out.println(s1);
		System.out.println(":::::::::::::::::::::::::::::::::");
		System.out.println();
		in1.close();
		
		
		BufferedReader in2 = new BufferedReader(new FileReader(inFile2));
		s1 = in2.readLine();	
		System.out.println("INPUT 'SURVEY DATA'");
		System.out.println("Data Heading....");
		System.out.println(":::::::::::::::::::::::::::::::::");
		System.out.println(s1);
		System.out.println(":::::::::::::::::::::::::::::::::");
		System.out.println();
		in2.close();
		
		
		BufferedReader in3 = new BufferedReader(new FileReader(inFile3));
		s1 = in3.readLine();	
		System.out.println("INPUT 'DISTRIBUTION DATA'");
		System.out.println("Data Heading....");
		System.out.println(":::::::::::::::::::::::::::::::::");
		System.out.println(s1);
		System.out.println(":::::::::::::::::::::::::::::::::");
		System.out.println();
		in3.close();
		
		} catch(EOFException e){
			System.out.println("End of stream");
		} catch (IOException e){
			System.out.println(e.getMessage());				
		}
	}
		
}
