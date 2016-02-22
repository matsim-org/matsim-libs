/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.noise;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * Merges files to the format 'Receiver Point Id ; xCoord ; yCoord ; Time ; Value' which can be used in QGis to create a video (Using the Plug-In 'TimeManager').
 * 
 * @author ikaddoura
 *
 */
public class CreateVideoCSVFile {

	private double startTime = 3 * 3600.;
	private double timeBinSize = 3600.;
	private double endTime = 10. * 3600.; // < 24
	private String pathToFilesToMerge = "/Users/ihab/Documents/workspace/shared-svn/projects/neukoellnNoise/baseCase_rpGap25meters/immissions/";
	private String receiverPointsFile = "/Users/ihab/Documents/workspace/shared-svn/projects/neukoellnNoise/baseCase_rpGap25meters/receiverPoints/receiverPoints.csv";
	private String separator = ";";
	private int iteration = 100;
	private String label = "immission";
	
	private String outputPath = pathToFilesToMerge;
	
	private BufferedWriter bw;
	private Map<Double, Map<Id<ReceiverPoint>, Double>> time2rp2value = new HashMap<Double, Map<Id<ReceiverPoint>, Double>>();
	private Map<Id<ReceiverPoint>, Tuple<Double, Double>> receiverPoints;
	
	public static void main(String[] args) {
		CreateVideoCSVFile readNoiseFile = new CreateVideoCSVFile();
		readNoiseFile.run();
	}
	
	private void run() {
		
		String outputFile = outputPath + Integer.toString(iteration) + "." + label + "_video.csv";
		
		try {
			
//			receiverPoints = readInReceiverPoints();
			receiverPoints = readInReceiverPoints(4592188., 5813743., 4604284., 5820670.);
			
			for (double time = startTime; time <= endTime; time = time + timeBinSize) {
				
				System.out.println("Reading time bin: " + time);
				
				String fileName = pathToFilesToMerge + Integer.toString(iteration) + "." + label + "_" + Double.toString(time) + ".csv";
				BufferedReader br = IOUtils.getBufferedReader(fileName);
				
				String line = null;
				line = br.readLine();

				Map<Id<ReceiverPoint>, Double> rp2value = new HashMap<Id<ReceiverPoint>, Double>();
				int lineCounter = 0;
				System.out.println("Reading lines ");
				while ((line = br.readLine()) != null) {
					
					if (lineCounter % 10000 == 0.) {
						System.out.println("# " + lineCounter);
					}
					
					String[] columns = line.split(separator);
					Id<ReceiverPoint> rp = null;
					Double value = null;
					for (int column = 0; column < columns.length; column++) {
						if (column == 0) {
							rp = Id.create(columns[column], ReceiverPoint.class);
						} else if (column == 1) {
							value = Double.valueOf(columns[column]); 
						} else {
							throw new RuntimeException("More than two columns. Aborting...");
						}						
					}
					rp2value.put(rp, value);

					lineCounter++;
					time2rp2value.put(time, rp2value);
				}
			}
			
			bw = new BufferedWriter(new FileWriter(outputFile));
			
			// write headers
			bw.write("Id;xCoord;yCoord;Time;" + label);
			bw.newLine();

			// fill table
			for (double time = startTime; time <= endTime; time = time + timeBinSize) {
									
				String dateString = "2000-01-01 ";
				String timeString = Time.writeTime(time, Time.TIMEFORMAT_HHMMSS);
				String dateTimeString = dateString + timeString;
				
				for (Id<ReceiverPoint> rp : receiverPoints.keySet()) {
					bw.write(rp.toString() + ";" + receiverPoints.get(rp).getFirst() + ";" + receiverPoints.get(rp).getSecond() + ";" + dateTimeString + ";" + time2rp2value.get(time).get(rp));
					bw.newLine();
				}
			}
			
			bw.close();
			System.out.println("Output written to " + outputFile);
			
			File file2 = new File(outputFile + "t");
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
				bw.write("\"String\",\"Real\",\"Real\",\"String\",\"Real\"");
							
				bw.newLine();
				
				bw.close();
				System.out.println("Output written to " + outputFile + "t");
				
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	private Map<Id<ReceiverPoint>, Tuple<Double, Double>> readInReceiverPoints() {
		Map<Id<ReceiverPoint>, Tuple<Double, Double>> rp2coords = new HashMap<Id<ReceiverPoint>, Tuple<Double,Double>>();
		BufferedReader br = IOUtils.getBufferedReader(this.receiverPointsFile);
		
		try {
			String line = null;
			line = br.readLine();
			
			int lineCounter = 0;
			System.out.println("Reading lines ");
			while ((line = br.readLine()) != null) {
				
				if (lineCounter % 10000 == 0.) {
					System.out.println("# " + lineCounter);
				}
				
				String[] columns = line.split(separator);
				Id<ReceiverPoint> rp = null;
				Double xCoord = null;
				Double yCoord = null;

				for (int column = 0; column < columns.length; column++) {
					if (column == 0) {
						rp = Id.create(columns[column], ReceiverPoint.class);
					} else if (column == 1) {
						xCoord = Double.valueOf(columns[column]);
					} else if (column == 2) {
						yCoord = Double.valueOf(columns[column]);
					} else {
						throw new RuntimeException("More than three columns. Aborting...");
					}
				}
				
				Tuple<Double, Double> coords = new Tuple<Double,Double>(xCoord, yCoord);
				rp2coords.put(rp, coords);
				
				lineCounter++;
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return rp2coords;
	}
	
	private Map<Id<ReceiverPoint>, Tuple<Double, Double>> readInReceiverPoints(Double xMin, Double yMin, Double xMax, Double yMax) {
		Map<Id<ReceiverPoint>, Tuple<Double, Double>> rp2coords = new HashMap<Id<ReceiverPoint>, Tuple<Double,Double>>();
		BufferedReader br = IOUtils.getBufferedReader(this.receiverPointsFile);
		
		try {
			String line = null;
			line = br.readLine();
			
			int lineCounter = 0;
			System.out.println("Reading lines ");
			while ((line = br.readLine()) != null) {
				
				if (lineCounter % 10000 == 0.) {
					System.out.println("# " + lineCounter);
				}
				
				String[] columns = line.split(separator);
				Id<ReceiverPoint> rp = null;
				Double xCoord = null;
				Double yCoord = null;

				for (int column = 0; column < columns.length; column++) {
					if (column == 0) {
						rp = Id.create(columns[column], ReceiverPoint.class);
					} else if (column == 1) {
						xCoord = Double.valueOf(columns[column]);
					} else if (column == 2) {
						yCoord = Double.valueOf(columns[column]);
					} else {
						throw new RuntimeException("More than three columns. Aborting...");
					}
				}
				
				if ( (xCoord > xMin) && (xCoord < xMax) && (yCoord > yMin) && (yCoord < yMax) ) {
					Tuple<Double, Double> coords = new Tuple<Double,Double>(xCoord, yCoord);
					rp2coords.put(rp, coords);
				}
				
				lineCounter++;
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return rp2coords;
	}

}
