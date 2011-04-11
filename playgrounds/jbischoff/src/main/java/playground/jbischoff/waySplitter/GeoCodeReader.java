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

/**
 * @author jbischoff
 *
 */

public class GeoCodeReader implements Runnable {

	@Override
	public void run() {
		JBGoogleGeocode gg = new JBGoogleGeocode();
		FileReader fr;
		String inf = "C:\\Users\\Joschka Bischoff\\Desktop\\Daten von Andreas\\bvg_befragung_persons.csv";
		String out = "C:\\Users\\Joschka Bischoff\\Desktop\\Daten von Andreas\\persons";
		try {
			fr = new FileReader(new File(inf));
			BufferedReader br = new BufferedReader(fr);
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(out+"_koord.csv")));
			
			
			String line = null;
			int l = 0;
			while ((line = br.readLine()) != null) {
				String[] r = line.split(";");
				
				if (l>0){
				Coord xy = gg.readGC(r[5]);	
				Double x = xy.getX();
				Double y = xy.getY();
				r[29] = x.toString();
				r[30] = y.toString();
				
				}
				l++;
				String newline = "";
				for (int i = 0 ; i<31 ; i++){
					newline = newline +";"+ r[i];
				}
				bw.append(newline+"\n");
			}
			bw.flush();
			bw.close();
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
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
		GeoCodeReader gc = new  GeoCodeReader();
		gc.run();

	}

}

