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

import java.io.*;
import org.matsim.core.utils.charts.*;
import java.util.StringTokenizer;




/**
 * Simple class to read an Excel file and 
 * to draw a MATSim chart from this the data.
 *
 * @author mfeil
 */
public class TravelCharts {	
	
	double [] xaxis = new double [101];
	double [] distanceserie = new double [xaxis.length];
	double [] timeserie = new double [xaxis.length];

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
			int index=0;
			while (line != null) {		
				
				tokenizer = new StringTokenizer(line);
				
				token = tokenizer.nextToken();
				this.xaxis[index]= Double.parseDouble(token);
				
				token = tokenizer.nextToken();
				this.distanceserie[index]= Double.parseDouble(token);
				
				token = tokenizer.nextToken();
				this.timeserie[index]= Double.parseDouble(token);

				line = br.readLine();
				index++;
			}

		
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}
	
	public static void main(final String [] args) {
		
		TravelCharts msc = new TravelCharts();
		
		msc.readData("./plans/tripdur.txt");
		for (int i=0; i<msc.xaxis.length;i++){
			//System.out.println(streetData.get(i));
			System.out.println(msc.xaxis[i]+"; "+msc.distanceserie[i]);
		}
		
		XYLineChart chart = new XYLineChart("Travel Statistics", "iteration", "distance in meters / time in seconds");
		chart.addSeries("average executed trip travel distance", msc.xaxis, msc.distanceserie);
		chart.addSeries("average executed trip travel time", msc.xaxis, msc.timeserie);
		chart.addMatsimLogo();
		chart.saveAsPng("./plans/travelstats.png", 800, 600);
		
	}

}

