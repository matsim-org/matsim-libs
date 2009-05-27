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

package playground.mfeil.analysis;

import java.io.*;
import org.matsim.core.utils.charts.*;
import java.util.StringTokenizer;




/**
 * Simple class to read an Excel file with several data sources and 
 * to draw a chart from this the data.
 *
 * @author mfeil
 */
public class TravelChartsComparison {	
	
	double [] xaxis = new double [101];
	double [] travdis1 = new double [xaxis.length];
	double [] travtim1 = new double [xaxis.length];
	double [] travdis2 = new double [xaxis.length];
	double [] travtim2 = new double [xaxis.length];


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
				this.travdis1[index]= Double.parseDouble(token);
				
				token = tokenizer.nextToken();
				this.travtim1[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.travdis2[index]= Double.parseDouble(token);

				token = tokenizer.nextToken();
				this.travtim2[index]= Double.parseDouble(token);

				line = br.readLine();
				index++;
			}

		
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}
	
	public static void main(final String [] args) {
		
		TravelChartsComparison msc = new TravelChartsComparison();
		
		msc.readData("./plans/traveldistancestats_comparison.txt");
		for (int i=0; i<msc.xaxis.length;i++){
			//System.out.println(streetData.get(i));
			System.out.println(msc.xaxis[i]+"; "+msc.travdis1[i]);
		}
		
		XYLineChart chart = new XYLineChart("Travel Statistics", "iteration", "distance in meters / time in seconds");
		
		chart.addSeries("PlanomatX: avg. exec. trip travel distance", msc.xaxis, msc.travdis1);
		chart.addSeries("PlanomatX: avg. exec. trip travel time", msc.xaxis, msc.travtim1);
		chart.addSeries("Base test: avg. exec. trip travel distance", msc.xaxis, msc.travdis2);
		chart.addSeries("Base test: avg. exec. trip travel time", msc.xaxis, msc.travtim2);

		chart.addMatsimLogo();
		chart.saveAsPng("./plans/traveldistancestats_comparison.png", 800, 600);
		
	}

}

