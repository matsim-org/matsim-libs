/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisActivityTimings.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.miscellanous;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * Simple class to read an events file and extract average activity timings. Works only when 
 * all agents have home-work-shopping-home or home-work-leisure-home activity chains. Calculates
 * averages and agent 1 in detail.
 *
 * @author mfeil
 */
public class AnalysisActivityTimings {	
	
	ArrayList<Double> times = new ArrayList<Double>();
	ArrayList<Integer> agent = new ArrayList<Integer>();
	ArrayList<String> event = new ArrayList<String>();
	ArrayList<String> event2 = new ArrayList<String>();


	public void readData(String path) {

		try {
	
			FileReader fr = new FileReader(path);		
			BufferedReader br = new BufferedReader(fr);
			String line = null;	
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String token = null;
			String token2 = null;
			int index = 1;
			while (line != null) {		
				System.out.println("Zeile "+index);
				tokenizer = new StringTokenizer(line);
		
				token = tokenizer.nextToken();
				this.times.add(Double.parseDouble(token));
			
				token = tokenizer.nextToken();
				this.agent.add(Integer.parseInt(token));
			
				token = tokenizer.nextToken();
				token = tokenizer.nextToken();
				token = tokenizer.nextToken();
				//token = tokenizer.nextToken();
				
				this.event.add(tokenizer.nextToken());
				token2="0";
				try{
				token2 = tokenizer.nextToken();
				}catch(Exception e){
				}
				if (token2!="0")this.event2.add(token2);
				else this.event2.add("shopping");
				line = br.readLine();
				index++;
			}

		
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}
	
	public static void main(final String [] args) {
		
		String outputDir = "./plans/";
		
		PrintStream stream1;
		try {
			stream1 = new PrintStream (new File(outputDir + "/analysisActivityTimings.xls"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		AnalysisActivityTimings msc = new AnalysisActivityTimings();

		msc.readData("./plans/100.events.txt");

		stream1.println("Agent 1");
		for (int i=0; i<msc.times.size();i++){
			if (msc.agent.get(i)==1 && msc.event.get(i).startsWith("actstart")) stream1.println("Act start\t"+msc.times.get(i)+
					"\t"+java.lang.Math.floor(msc.times.get(i)/3600)+
					"\t"+java.lang.Math.floor((msc.times.get(i)-(java.lang.Math.floor(msc.times.get(i)/3600))*3600)/60)+
					"\t"+(msc.times.get(i)-(java.lang.Math.floor(msc.times.get(i)/3600)*3600)-(java.lang.Math.floor((msc.times.get(i)-(java.lang.Math.floor(msc.times.get(i)/3600))*3600)/60)*60)));
			if (msc.agent.get(i)==1 && msc.event.get(i).startsWith("actend")) stream1.println("Act end\t"+msc.times.get(i)+
					"\t"+java.lang.Math.floor(msc.times.get(i)/3600)+
					"\t"+java.lang.Math.floor((msc.times.get(i)-(java.lang.Math.floor(msc.times.get(i)/3600))*3600)/60)+
					"\t"+(msc.times.get(i)-(java.lang.Math.floor(msc.times.get(i)/3600)*3600)-(java.lang.Math.floor((msc.times.get(i)-(java.lang.Math.floor(msc.times.get(i)/3600))*3600)/60)*60)));
		}
		double timeHome1 = 0;
		double timeHome1L = 0;
		double timeWork = 0;
		double timeWorkL = 0;
		double timeShopping = 0;
		double timeLeisure = 0;
		double timeHome2 = 0;
		double timeHome2L = 0;
		
		double timeftrip = 0;
		double timestrip = 0;
		double timettrip = 0;	
		double timeftripL = 0;
		double timestripL = 0;
		double timettripL = 0;
		
		double prtime = 0;
		boolean hasLeisure = false;
		int actpos = 0;
		
		for (int j=1;j<325;j++){
			actpos=0;
			for (int i=0; i<msc.times.size();i++){
				if (msc.event2.get(i).startsWith("leisure") && msc.agent.get(i)==j) {
					hasLeisure = true;
					break;
				}
				else hasLeisure = false;
			}
			System.out.println("hasLeisure "+hasLeisure);
			if (!hasLeisure){
				for (int i=0; i<msc.times.size();i++){
					if (msc.agent.get(i)==j && msc.event.get(i).startsWith("actend")) {
						if (actpos==0) timeHome1 += msc.times.get(i);
						else if (actpos==1) timeWork += msc.times.get(i)-prtime;
						else if (actpos==2) timeShopping += msc.times.get(i)-prtime;
						actpos++;
					}
					else if (msc.agent.get(i)==j && msc.event.get(i).startsWith("actstart")) {
						if (actpos!=3) prtime = msc.times.get(i);
						else timeHome2 += 86400 - msc.times.get(i);
					}
					else if (msc.agent.get(i)==j && msc.event.get(i).startsWith("departure")) {
						prtime = msc.times.get(i);
					}
					else if (msc.agent.get(i)==j && msc.event.get(i).startsWith("arrival")) {
						if (actpos==1) timeftrip += msc.times.get(i) - prtime;
						else if (actpos==2) timestrip += msc.times.get(i)- prtime;
						else if (actpos==3) timettrip += msc.times.get(i)- prtime;
					}
				}
			} else{
				for (int i=0; i<msc.times.size();i++){
					if (msc.agent.get(i)==j && msc.event.get(i).startsWith("actend")) {
						if (actpos==0) timeHome1L += msc.times.get(i);
						else if (actpos==1) timeWorkL += msc.times.get(i)-prtime;
						else if (actpos==2) timeLeisure += msc.times.get(i)-prtime;
						actpos++;
					}
					else if (msc.agent.get(i)==j && msc.event.get(i).startsWith("actstart")) {
						if (actpos!=3) prtime = msc.times.get(i);
						else timeHome2L += 86400 - msc.times.get(i);
					}
					else if (msc.agent.get(i)==j && msc.event.get(i).startsWith("departure")) {
						prtime = msc.times.get(i);
					}
					else if (msc.agent.get(i)==j && msc.event.get(i).startsWith("arrival")) {
						if (actpos==1) timeftripL += msc.times.get(i) - prtime;
						else if (actpos==2) timestripL += msc.times.get(i)- prtime;
						else if (actpos==3) timettripL += msc.times.get(i)- prtime;
					}
				}
			}
		}
		stream1.println();
		stream1.println("Average");
		stream1.println("Home1\tTrip\tWork\tTrip\tShopping\tTrip\tHome2");
		stream1.println(timeHome1/163/3600+"\t"+timeftrip/163/3600+"\t"+timeWork/163/3600+"\t"+timestrip/163/3600+"\t"+timeShopping/163/3600+"\t"+timettrip/163/3600+"\t"+timeHome2/163/3600);
		stream1.println("Home1\tTrip\tWork\tTrip\tLeisure\tTrip\tHome2");
		stream1.println(timeHome1L/161/3600+"\t"+timeftripL/161/3600+"\t"+timeWorkL/161/3600+"\t"+timestripL/161/3600+"\t"+timeLeisure/161/3600+"\t"+timettripL/161/3600+"\t"+timeHome2L/161/3600);
		
		System.out.println("Analysis finished.");
	}

}

