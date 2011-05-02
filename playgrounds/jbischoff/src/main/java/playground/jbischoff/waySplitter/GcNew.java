/* *********************************************************************** *
 * project: org.matsim.*
 * GeoCodeReader.java
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

package playground.jbischoff.waySplitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author jbischoff
 *
 */

public class GcNew implements Runnable {

	@Override
	public void run() {
//		JBGoogleGeocode gg = new JBGoogleGeocode();
		FileReader fr;

//		String inf = "/Users/JB/Documents/Work/Daten von Andreas/bvg_befragung_ways.csv";
//		String out = "/Users/JB/Documents/Work/Daten von Andreas/bvg_befragung_ways";

		String inf = "C:\\Users\\Joschka Bischoff\\Desktop\\Daten von Andreas\\bvg_befragung_ways_koord_final.csv";
		String out = "C:\\Users\\Joschka Bischoff\\Desktop\\Daten von Andreas\\bvg_befragung_ways_koord_final";
		
		CoordinateTransformation coorTransform = TransformationFactory
		.getCoordinateTransformation(TransformationFactory.DHDN_GK4,TransformationFactory.WGS84);
		try {
			fr = new FileReader(new File(inf));
			BufferedReader br = new BufferedReader(fr);
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(out+"_wgs2.csv")));
			
			
			String line = null;
			int l = 0;
			int xx =1;
			while ((line = br.readLine()) != null) {
				String[] r = line.split(";");
				
				
//				Coord xy = gg.readGC(r[5]);	
				
				Double x = 0.0;
				Double y = 0.0;
			
				try{
//				Coord xy = coorTransform.transform(new CoordImpl(Double.parseDouble(r[29]),Double.parseDouble(r[30])));
					
					Coord xy = coorTransform.transform(new CoordImpl(Double.parseDouble(r[52]),Double.parseDouble(r[53])));
//					System.out.println(r[52]+" "+r[53]+" to "+xy);
					x = xy.getX();
				 y = xy.getY();
				}
				catch (Exception e) {xx++;}
				
				
//				r[29] = x.toString();
//				r[30] = y.toString();
//					persons
					

					r[52] = x.toString();
					r[53] = y.toString();
//						ways
											
				
				
				l++;
				String newline = "";
//				for (int i = 0 ; i<31 ; i++) 
//				persons
					for (int i = 0 ; i<54 ; i++) 
//				ways
				{
					newline = newline +";"+ r[i];
				}
				bw.append(newline+"\n");
			}
			bw.flush();
			bw.close();
			System.out.println("done, "+xx);
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GcNew gc = new  GcNew();
		gc.run();

	}

}

