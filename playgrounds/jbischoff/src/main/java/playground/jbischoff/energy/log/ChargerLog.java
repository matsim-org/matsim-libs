/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

	package playground.jbischoff.energy.log;

	import java.io.BufferedWriter;
	import java.io.File;
	import java.io.FileWriter;
	import java.io.IOException;
	import java.util.Collections;
	import java.util.LinkedList;

	import org.matsim.api.core.v01.Id;


/**
 *@author jbischoff
 *
 */

public class ChargerLog {



		private LinkedList<ChargeLogRow> log;

		public ChargerLog() {
			reset();
		}

		public void reset() {
			log = new LinkedList<ChargeLogRow>();
		}

		public void add(ChargeLogRow row) {
			log.add(row);
		}

		public ChargeLogRow get(int i) {
			return log.get(i);
		}

		public int getNumberOfEntries() {
			return log.size();
		}

		public String getTitleRowFileOutput() {
			return "chargerId\ttime\tOccupation\trelative Occupation";
		}

		public void printToConsole() {
			System.out.println(getTitleRowFileOutput());
			Collections.sort(log);
			for (ChargeLogRow row:log){
				System.out.println(rowToString(row));
			}
		}
		
		private String rowToString(ChargeLogRow row){
			return (row.getChargerId() + "\t"+ row.getTime() + "\t" +row.getAbsoluteLOC()+ "\t" +row.getRelativeLOC());
		}
		
		public void writeToFile(String outputFile)  {
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
				Collections.sort(log);
				bw.write(this.getTitleRowFileOutput());
				for (ChargeLogRow row:log){
					bw.newLine();
					bw.write(rowToString(row));
					
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				System.err.println("Could not create File" + outputFile);
				e.printStackTrace();
			}
		}
		
		public int size(){
			return log.size();
		}



	public void writeToFiles(String outputFileDir)  {
		
		try {
			if (log.isEmpty()) return;
			
			Collections.sort(log);
			Id last = log.get(0).getChargerId();
			int l = last.toString().length()-6;
			String fn ;
			try{
			fn = last.toString().substring(l);
			}
			catch (StringIndexOutOfBoundsException e){
			fn = "charger "+Math.random();	
			}
			String filename = (outputFileDir+"/charger_"+fn+".txt");
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
			
//			bw.write(this.getTitleRowFileOutput());
			
			
			for (ChargeLogRow row:log){
				if (row.getChargerId().equals(last)){
				bw.newLine();
				bw.write(rowToString(row));
				last  = row.getChargerId();
				}
				else {
					bw.flush();
					bw.close();
					last  = row.getChargerId();
					l = last.toString().length()-6;
					 fn = last.toString().substring(l);

					filename = (outputFileDir+"/charger_"+fn+".txt");
					bw = new BufferedWriter(new FileWriter(new File(filename)));
					bw.write(rowToString(row));
				}
				
			}
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			System.err.println("Could not create File" + outputFileDir);
			e.printStackTrace();
		}
	}


	

}
