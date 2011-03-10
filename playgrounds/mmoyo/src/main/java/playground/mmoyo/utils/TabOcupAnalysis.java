/* *********************************************************************** *
 * project: org.matsim.*
 * TabOcupAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.mmoyo.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mmoyo.analysis.counts.reader.CountsReader;

/**
 * Creates a tabular file with ocupp analysis of several runs stored in a folder
 */
public class TabOcupAnalysis {
	final String runsDirPath;
	
	public TabOcupAnalysis(final String runsDirPath){
		this.runsDirPath = runsDirPath;
	}
	
	protected void run(){
		File runsDir = new File(this.runsDirPath);
		if (runsDir.isDirectory()) {
			String[] children = runsDir.list();	
			final String occupTextLoc = "/ITERS/it.10/10.simCountCompareOccupancy.txt";
			final String strStop1 = "812020.3";
			final String strStop2 = "812550.1";
			final String TAB = "\t";
			final String CRL = "\n";
			
			Id stopId_1 = new IdImpl(strStop1);
			Id stopId_2 = new IdImpl(strStop2);
			
			StringBuffer sBuff1 = new StringBuffer(strStop1 + CRL + "Combination" + TAB);
			StringBuffer sBuff2 = new StringBuffer(strStop2 + CRL + "Combination" + TAB);
			
			for (byte j= 0; j<24 ; j++)   {
				sBuff1.append(j + TAB);
				sBuff2.append(j + TAB);
			}
			
			for (int i=0; i<children.length; i++) {
				String combName= children[i];
				CountsReader countReader = new CountsReader(this.runsDirPath + combName + occupTextLoc);

				double[] valuesStop1 =  countReader.getStopSimCounts(stopId_1);
				double[] valuesStop2 =  countReader.getStopSimCounts(stopId_2);
				
				sBuff1.append(CRL + combName + TAB);
				sBuff2.append(CRL + combName + TAB);
				
				for (byte j= 0; j<24 ; j++)   {  
					sBuff1.append(valuesStop1[j] + TAB);
					sBuff2.append(valuesStop2[j] + TAB);
				}
			}
			
			try {
			    BufferedWriter out = new BufferedWriter(new FileWriter(this.runsDirPath + strStop1 + ".txt"));
			    BufferedWriter out2 = new BufferedWriter(new FileWriter(this.runsDirPath + strStop2 + ".txt"));
			    out.write(sBuff1.toString());
			    out2.write(sBuff2.toString());
			    out.close();
			    out2.close();
			} catch (IOException e) {

			}
			
		}
	}
	
	protected void OccupFileExist(final String configDirPath){
		final String occupTextLoc = "/ITERS/it.10/10.simCountCompareOccupancy.txt";
		
		File configDir = new File(configDirPath);
		List <String> nonMissingRuns = new ArrayList<String>();
		List <String> missingRuns = new ArrayList<String>();
		
		//final String routedPlan = "routedPlan_";
		
		if (configDir.isDirectory()) {
			String[] children = configDir.list();	
			for (int i=0; i<children.length; i++) {
				String combination = children[i];
				combination = combination.substring(0, combination.length()-14);
				
				File occupFile = new File(this.runsDirPath + combination + occupTextLoc);
				//System.out.println(occupFile.getPath());
				if (occupFile.exists()){
					
					nonMissingRuns.add(combination);
				}else{
					missingRuns.add(combination);
				}
				//System.out.println(combination);
			}
		}
		
		System.out.println("These combinations are present:");
		for (String nonMissRun : nonMissingRuns){
			System.out.println(nonMissRun);
		}
		
		System.out.println("These combinations are misssing:");
		for (String missRun : missingRuns){
			System.out.println(missRun);
		}
		
	}
	
	private void test(){
		File runsDir = new File(this.runsDirPath);
		if (runsDir.isDirectory()) {
			String[] children = runsDir.list();	
			for (int i=0; i<=children.length; i++) {
				System.out.println(children[i]);
			}
		}
	}
		
	
	public static void main(String[] args) throws IOException {
		String runsDirPath;
		String configDirPath;
		if (args.length==2){
			configDirPath= args[0];
			runsDirPath=args[1];
		}else{
			configDirPath= "I:/configs/";
			runsDirPath="I:/z_Runs/";
			
		}
		
		TabOcupAnalysis tabOcupAnalysis = new TabOcupAnalysis(runsDirPath);
		//tabOcupAnalysis.OccupFileExist(configDirPath);
		tabOcupAnalysis.run();
	}

}
