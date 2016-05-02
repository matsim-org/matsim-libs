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
package org.matsim.contrib.noise.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import org.matsim.contrib.noise.data.ReceiverPoint;

/**
 * @author ikaddoura
 *
 */
public final class MergeNoiseCSVFile {

	private static final Logger log = Logger.getLogger(MergeNoiseCSVFile.class);

	// default values
	private double startTime = 4. * 3600.;
	private double timeBinSize = 3600.;
	private double endTime = 24. * 3600.;
	private String separator = ";";
	private double threshold = -1. ;
	private OutputFormat outputFormat = OutputFormat.xyt ;
	
	private String outputDirectory = null;
	private String[] workingDirectories = null;
	private String[] labels = null;
	private String receiverPointsFile = null;

	public static enum OutputFormat { xyt1t2t3etc, xyt } ;

	private Map<String, Map<Double, Map<Id<ReceiverPoint>, Double>>> label2time2rp2value = new HashMap<>();
	private Map<Id<ReceiverPoint>, Coord> rp2Coord = new HashMap<Id<ReceiverPoint>, Coord>();

	public static void main(String[] args) {
		MergeNoiseCSVFile readNoiseFile = new MergeNoiseCSVFile();
		readNoiseFile.run();
	}
	
	public final void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public final void setLabel(String label) {
		this.labels = null;
		this.labels[0] = label;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectories = null;
		this.workingDirectories[0] = workingDirectory;
		this.outputDirectory = workingDirectory;
	}

	public void setOutputDirectory(String outputFilePath) {
		this.outputDirectory = outputFilePath;
	}

	public void setReceiverPointsFile(String receiverPointsFile) {
		this.receiverPointsFile = receiverPointsFile;
	}

	public final void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setWorkingDirectory(String[] workingDirectories) {
		this.workingDirectories = workingDirectories;
	}

	public void setLabel(String[] labels) {
		this.labels = labels;
	}

	public void setTimeBinSize(double timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public final void run() {
		// lv final. kai

		readValues();
		readReceiverPoints();
		writeFile();
	}

	private void writeFile() {
		int lineCounter = 0 ;

		String outputFile = this.outputDirectory;

		for (int i = 0; i < this.labels.length; i++) {
			outputFile = outputFile + this.labels[i] + "_"; 			
		}
		outputFile = outputFile + "merged_" + this.outputFormat.toString() + ".csv.gz";

		try ( BufferedWriter bw = IOUtils.getBufferedWriter(outputFile) ) {
			// so-called "try-with-resources". Kai

			log.info(" Writing merged file to " + outputFile + "...") ;

			// write headers
			switch( this.outputFormat ) {
			// yy should probably become different classes. kai
			case xyt1t2t3etc:
				bw.write("Receiver Point Id;x;y");
				for (String label : this.label2time2rp2value.keySet()) {
					for (double time = startTime; time <= endTime; time = time + timeBinSize) {
						bw.write(";" + label + "_" + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS));
					}
				}
				break;
			case xyt:
				bw.write("Receiver Point Id;x;y;time");
				for (String label : this.label2time2rp2value.keySet()) {
					bw.write(";" + label);
				}
				break;
			default:
				throw new RuntimeException("not implemented") ;
			}

			bw.newLine();

			// fill table
			switch( this.outputFormat ) {
			case xyt1t2t3etc:	

				for (Id<ReceiverPoint> rp : this.rp2Coord.keySet()) {
					bw.write(rp.toString() + ";" + rp2Coord.get(rp).getX() + ";" + rp2Coord.get(rp).getY());

					for (String label : this.label2time2rp2value.keySet()) {
						for (double time = startTime; time <= endTime; time = time + timeBinSize) {
							bw.write(";" + this.label2time2rp2value.get(label).get(time).get(rp));
						}
					}
					bw.newLine();
				}				
				break;
			case xyt:

				for (Id<ReceiverPoint> rp : this.rp2Coord.keySet()) {

					for (double time = startTime; time <= endTime; time = time + timeBinSize) {

						boolean writeThisLine = false;
						String lineToWrite = rp.toString() + ";" + rp2Coord.get(rp).getX() + ";" + rp2Coord.get(rp).getY() + ";" + String.valueOf(time);

						for (String label : this.label2time2rp2value.keySet()) {
							double value = this.label2time2rp2value.get(label).get(time).get(rp);
							if (value > this.threshold) {
								writeThisLine = true;
							}
							lineToWrite = lineToWrite + ";" + value;
						}

						// only write the line if at least one value is larger than threshold
						if (writeThisLine) {
							bw.write(lineToWrite);
							bw.newLine();
							lineCounter ++ ;
							if (lineCounter % 10000 == 0.) {
								log.info("# " + lineCounter);
							}
						}
					}
				}	
				break ;
			default:
				throw new RuntimeException("not implemented") ;
			}

			bw.close();
			log.info("Output written to " + outputFile);


		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void readReceiverPoints() {

		BufferedReader br = IOUtils.getBufferedReader(this.receiverPointsFile);
		String line;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int lineCounter = 0;

		log.info("Reading receiver points file...");

		try {
			while( (line = br.readLine()) != null){

				if (lineCounter % 10000 == 0.) {
					log.info("# " + lineCounter);
				}

				String[] columns = line.split(this.separator);
				Id<ReceiverPoint> rpId = null;
				double x = 0;
				double y = 0;

				for(int i = 0; i < columns.length; i++){

					switch(i){
					case 0: rpId = Id.create(columns[i], ReceiverPoint.class);
					break;
					case 1: x = Double.valueOf(columns[i]);
					break;
					case 2: y = Double.valueOf(columns[i]);
					break;
					default: throw new RuntimeException("More than three columns. Aborting...");
					}
				}
				lineCounter++;
				this.rp2Coord.put(rpId, new Coord(x, y));
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private void readValues() {
		for (int ll = 0; ll < this.labels.length; ll++) {

			Map<Double, Map<Id<ReceiverPoint>, Double>> time2rp2value = new HashMap<Double, Map<Id<ReceiverPoint>, Double>>();

			String workingDirectory = this.workingDirectories[ll];
			String label = this.labels[ll];

			log.info("Reading " + label + "...");

			for (double time = startTime; time <= endTime; time = time + timeBinSize) {

				log.info("Reading time bin: " + time);

				String fileName = workingDirectory + label + "_" + Double.toString(time) + ".csv";

				try ( BufferedReader br = IOUtils.getBufferedReader(fileName) ) {
					// this will automagically use the *.gz version if a non-gzipped version does not exist. kai, jan'15

					String line = br.readLine();

					Map<Id<ReceiverPoint>, Double> rp2value = new HashMap<Id<ReceiverPoint>, Double>();
					int lineCounter = 0;

					log.info("Reading lines ");
					while ((line = br.readLine()) != null) {

						if (lineCounter % 10000 == 0.) {
							log.info("# " + lineCounter);
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
								// throw new RuntimeException("More than two columns. Aborting...");
							}
							rp2value.put(rp, value);

						}
						lineCounter++;
						time2rp2value.put(time, rp2value);
					}
				} catch (NumberFormatException | IOException e) {
					e.printStackTrace();
				}
			}

			this.label2time2rp2value.put(label, time2rp2value);
		}
	}

}
