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

import com.vividsolutions.jts.geom.Envelope;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.analysis.vsp.qgis.GraduatedSymbolRenderer;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisWriter;
import org.matsim.contrib.analysis.vsp.qgis.RendererFactory;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ikaddoura
 *
 */
public class ProcessNoiseImmissions {
	
	private static final Logger log = Logger.getLogger(ProcessNoiseImmissions.class);

	private final double startTime = 3600.;
	private final double timeBinSize = 3600.;
	private final double endTime = 24. * 3600.;
	
	private final double receiverPointGap;
	private final String workingDirectory;
	private String receiverPointsFile;
		
	private final String separator = ";";
	private final String label = "immission";
	
	private final String outputPath;
	
	private BufferedWriter bw;
	private Map<Double, Map<Id<ReceiverPoint>, Double>> time2rp2value = new HashMap<Double, Map<Id<ReceiverPoint>, Double>>();
	
	public ProcessNoiseImmissions(String workingDirectory, String receiverPointsFile, double receiverPointGap) {
		this.workingDirectory = workingDirectory;
		this.receiverPointsFile = receiverPointsFile;
		this.receiverPointGap = receiverPointGap;
		this.outputPath = workingDirectory;
	}

	public static void main(String[] args) {
		String workingDirectory = "/Users/ihab/Documents/workspace/runs-svn/cn/output/cn/ITERS/it.100/immissions/";
		String receiverPointsFile = "/Users/ihab/Documents/workspace/runs-svn/cn/output/cn/receiverPoints/receiverPoints.csv";

		ProcessNoiseImmissions readNoiseFile = new ProcessNoiseImmissions(workingDirectory, receiverPointsFile, 100);
		readNoiseFile.run();
	}
	
	public void run() {
		
		String outputFile = outputPath + label + "_processed.csv";
		
		try {
			
			for (double time = startTime; time <= endTime; time = time + timeBinSize) {
				
				log.info("Reading time bin: " + time);

				String fileName = workingDirectory + label + "_" + Double.toString(time) + ".csv";
				BufferedReader br = IOUtils.getBufferedReader(fileName);
				
				String line = null;
				line = br.readLine();

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
//							throw new RuntimeException("More than two columns. Aborting...");
						}
						rp2value.put(rp, value);
						
					}
					lineCounter++;
					time2rp2value.put(time, rp2value);
				}
			}
			
			BufferedReader br = IOUtils.getBufferedReader(this.receiverPointsFile);
			String line = br.readLine();
			
			Map<Id<ReceiverPoint>, Coord> rp2Coord = new HashMap<Id<ReceiverPoint>, Coord>();
			int lineCounter = 0;
			
			log.info("Reading receiver points file");
			
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
				rp2Coord.put(rpId, new Coord(x, y));
				
			}
			
			bw = new BufferedWriter(new FileWriter(outputFile));
			
			// write headers
			bw.write("Receiver Point Id;x;y");
			
			for (double time = startTime; time <= endTime; time = time + timeBinSize) {
				bw.write(";" + label + "_" + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS));
			}
			
			bw.write(";Lden;L_6-9;L_16-19");

			bw.newLine();

			// fill table
			for (Id<ReceiverPoint> rp : time2rp2value.get(endTime).keySet()) {
				bw.write(rp.toString() + ";" + rp2Coord.get(rp).getX() + ";" + rp2Coord.get(rp).getY());
				
				for (double time = startTime; time <= endTime; time = time + timeBinSize) {
					bw.write(";" + time2rp2value.get(time).get(rp));
				}
				
				// aggregate time intervals
	
				double termDay = 0.;
				// day: 7-19
				for (double time = 8 * 3600.; time <= 19 * 3600.; time = time + timeBinSize) {
					termDay = termDay + Math.pow(10, time2rp2value.get(time).get(rp) / 10);
				}
				
				double termEvening = 0.;
				// evening: 19-23
				for (double time = 20 * 3600.; time <= 23 * 3600.; time = time + timeBinSize) {
					termEvening = termEvening + Math.pow(10, (time2rp2value.get(time).get(rp) + 5) / 10);
				}

				double termNight = 0.;
				// night: 23-7
				
				// nightA: 23-24
				for (double time = 24 * 3600.; time <= 24 * 3600.; time = time + timeBinSize) {
					termNight = termNight + Math.pow(10, (time2rp2value.get(time).get(rp) + 10) / 10);
				}
				// nightB: 0-7
				for (double time = 1 * 3600.; time <= 7 * 3600.; time = time + timeBinSize) {
					termNight = termNight + Math.pow(10, (time2rp2value.get(time).get(rp) + 10) / 10);
				}
			
				double Lden = 10 * Math.log10(1./24. * (termDay + termEvening + termNight));
				bw.write(";" + Lden);
				
				double term69 = 0.;
				for (double time = 7 * 3600.; time <= 9 * 3600.; time = time + timeBinSize) {
					term69 = term69 + Math.pow(10, (time2rp2value.get(time).get(rp)) / 10);
				}
				double L_69 = 10 * Math.log10(1./3. * term69);
				bw.write(";" + L_69);
				
				double term1619 = 0.;
				for (double time = 17 * 3600.; time <= 19 * 3600.; time = time + timeBinSize) {
					term1619 = term1619 + Math.pow(10, (time2rp2value.get(time).get(rp)) / 10);
				}
				double L_1619 = 10 * Math.log10(1./3. * term1619);
				bw.write(";" + L_1619);
				
				bw.newLine();
			}				
			
			bw.close();
			log.info("Output written to " + outputFile);
		}
		
		catch (IOException e1) {
			e1.printStackTrace();
		}

		String qGisProjectFile = "immission.qgs";
		
		QGisWriter writer = new QGisWriter(TransformationFactory.DHDN_GK4, workingDirectory);
			
// ################################################################################################################################################
		Envelope envelope = new Envelope(4568808,5803042,4622772,5844280);
		writer.setEnvelope(envelope);
				
		VectorLayer noiseLayer = new VectorLayer("noise", outputFile, QGisConstants.geometryType.Point, true);
		noiseLayer.setDelimiter(";");
		noiseLayer.setXField("x");
		noiseLayer.setYField("y");

        GraduatedSymbolRenderer renderer = RendererFactory.createNoiseRenderer(noiseLayer, this.receiverPointGap);
		renderer.setRenderingAttribute("Lden");
		
		writer.addLayer(noiseLayer);
		
// ################################################################################################################################################
		
		writer.write(qGisProjectFile);
		
	}

}
