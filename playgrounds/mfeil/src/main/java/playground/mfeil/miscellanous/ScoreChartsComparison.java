/* *********************************************************************** *
 * project: org.matsim.*
 * TravelChartsComparison.java
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

import java.io.*;
import org.matsim.core.utils.charts.*;
import java.util.StringTokenizer;




/**
 * Simple class to read an Excel file with several data sources and 
 * to draw a chart from this the data.
 *
 * @author mfeil
 */
public class ScoreChartsComparison {	
	
	double [] xaxis = new double [101];
	double [] best1 = new double [xaxis.length];
	double [] exec1 = new double [xaxis.length];
	double [] ave1 = new double [xaxis.length];
	double [] worst1 = new double [xaxis.length];
	double [] best2 = new double [xaxis.length];
	double [] exec2 = new double [xaxis.length];
	double [] ave2 = new double [xaxis.length];
	double [] worst2 = new double [xaxis.length];

	public void readData(String path) {

		try {

			FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
			line = br.readLine(); // contains column headers
			
			line = br.readLine();
			String token = null;
			int index=0;
			while (line != null) {		
				
				tokenizer = new StringTokenizer(line);
				
				token = tokenizer.nextToken();
				this.xaxis[index]= Double.parseDouble(token);
				
				token = tokenizer.nextToken();
				this.exec1[index]= Double.parseDouble(token);
				
				token = tokenizer.nextToken();
				this.worst1[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.ave1[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.best1[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.exec2[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.worst2[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.ave2[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.best2[index]= Double.parseDouble(token);

				line = br.readLine();
				index++;
			}

		
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}
	
	public static void main(final String [] args) {
		
		ScoreChartsComparison msc = new ScoreChartsComparison();
		
		msc.readData("./plans/scorestats.txt");
		for (int i=0; i<msc.xaxis.length;i++){
			//System.out.println(streetData.get(i));
			System.out.println(msc.xaxis[i]+"; "+msc.best1[i]);
		}
		
		XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
		
		chart.addSeries("Base test: avg. best score", msc.xaxis, msc.best2);
		//chart.addSeries(/*"PlanomatX: avg. worst score"*/"", msc.xaxis, msc.exec1);
		chart.addSeries("PlanomatX: avg. best score", msc.xaxis, msc.best1);
		//chart.addSeries(/*"PlanomatX: avg. of plans' average score"*/"", msc.xaxis, msc.exec1);
		chart.addSeries("Base test: avg. executed score", msc.xaxis, msc.exec2);
		chart.addSeries("PlanomatX: avg. executed score", msc.xaxis, msc.exec1);
		//chart.addSeries("Base test: avg. worst score", msc.xaxis, msc.worst2);
		//chart.addSeries("Base test: avg. best score", msc.xaxis, msc.best2);
		//chart.addSeries("Base test: avg. of plans' average score", msc.xaxis, msc.ave2);
		//chart.addSeries("Base test: avg. executed score", msc.xaxis, msc.exec2);
		chart.addMatsimLogo();
		chart.saveAsPng("./plans/scorestats.png", 800, 600);
		
	}

}

