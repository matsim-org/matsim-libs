/* *********************************************************************** *
 * project: org.matsim.*
 * DgPrognose2025Files2WGS84Converter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.prognose2025;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.DgPaths;
import playground.gregor.gis.coordinatetransform.ApproximatelyCoordianteTransformation;


/**
 * @author dgrether
 *
 */
public class DgPrognose2025Files2WGS84Converter {

	private static final Logger log = Logger.getLogger(DgPrognose2025Files2WGS84Converter.class);
	
	private String knoten2004strasse = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten.csv";
	private String knoten2004strasseOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten_wgs84.csv";
	private String knoten2004wasser = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/wasser/knoten.csv";
	private String knoten2004wasserOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/wasser/knoten_wgs84.csv";
	private String knoten2004schiene = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/schiene/knoten.csv";
	private String knoten2004schieneOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/schiene/knoten_wgs84.csv";
	private String knoten2004luft = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/luft/knoten.csv";
	private String knoten2004luftOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/luft/knoten_wgs84.csv";
	
	private String knoten2025strasse = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/strasse/knoten.csv";
	private String knoten2025strasseOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/strasse/knoten_wgs84.csv";
	private String knoten2025wasser = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/wasser/knoten.csv";
	private String knoten2025wasserOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/wasser/knoten_wgs84.csv";
	private String knoten2025schiene = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/schiene/knoten.csv";
	private String knoten2025schieneOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/schiene/knoten_wgs84.csv";
	private String knoten2025luft = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/luft/knoten.csv";
	private String knoten2025luftOut = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2025/luft/knoten_wgs84.csv";
	
	
	private String f = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/coordinateTransformationLookupTable.csv";
	private ApproximatelyCoordianteTransformation transform = new ApproximatelyCoordianteTransformation(f);
	
	private void startConversion(String file, String outFile, int indexX, int indexY) {
		log.info("Conversion of " + file + " to " + outFile + " ...");
		try {
			BufferedReader reader = IOUtils.getBufferedReader(file);
			BufferedWriter writer = IOUtils.getBufferedWriter(outFile);

			//don't convert the header
			String line = reader.readLine();
			if (line != null) {
				writer.write(line);
				writer.newLine();
			}
			String[] columns = null;
			String xCoordString, yCoordString;
			double xCoord, yCoord;
			Coord coord;
			line = reader.readLine();
			while (line != null) {
				columns = line.split(";");
				
				//get and transform the coordinate
				xCoordString = columns[indexX];
				yCoordString = columns[indexY];
				xCoord = Double.parseDouble(xCoordString);
				yCoord = Double.parseDouble(yCoordString);
				coord = new CoordImpl(xCoord, yCoord);
				coord = transform.getTransformed(coord);
				
				//create the new String with the transformed Coordinate
				StringBuilder b = new StringBuilder();
				for (int i = 0; i < columns.length; i++){
					if (i == indexX){
						b.append(coord.getX());
					}
					else if (i == indexY){
						b.append(coord.getY());
					}
					else {
						b.append(columns[i]);
					}
					b.append(";");
				}
				writer.write(b.toString());
				writer.newLine();
				
				line = reader.readLine();
			} // end while
			
			reader.close();
			writer.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Conversion done.");
	}

	
	private void startConversion() {
		this.startConversion(knoten2004strasse, knoten2004strasseOut, 2, 3);
		this.startConversion(knoten2004schiene, knoten2004schieneOut, 3, 4);
		this.startConversion(knoten2004wasser, knoten2004wasserOut, 2, 3);
		this.startConversion(knoten2004luft, knoten2004luftOut, 3, 4);

		this.startConversion(knoten2025strasse, knoten2025strasseOut, 2, 3);
		this.startConversion(knoten2025schiene, knoten2025schieneOut, 3, 4);
		this.startConversion(knoten2025wasser, knoten2025wasserOut, 2, 3);
		this.startConversion(knoten2025luft, knoten2025luftOut, 3, 4);

	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DgPrognose2025Files2WGS84Converter().startConversion();
	}





}
