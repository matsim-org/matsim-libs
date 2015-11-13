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
package playground.ikaddoura.noise2.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.ikaddoura.noise2.data.ReceiverPoint;

/**
 * @author ikaddoura
 *
 */
public final class MergeNoiseCSVFile {
	// lv final. kai

	private double startTime = 3600.;
	private double timeBinSize = 3600.;
	private double endTime = 30. * 3600.;

	private String workingDirectory = "/Users/ihab/Documents/workspace/runs-svn/cn/output/cn/ITERS/it.100/damages_receiverPoint/";
	private String receiverPointsFile = "/Users/ihab/Documents/workspace/runs-svn/cn/output/cn/receiverPoints/receiverPoints.csv";

	private String separator = ";";
	private String label = "damages_receiverPoint";

	public static enum OutputFormat { ihab, xyt } ;
	private OutputFormat outputFormat = OutputFormat.ihab ;

	private Map<Double, Map<Id<ReceiverPoint>, Double>> time2rp2value = new HashMap<Double, Map<Id<ReceiverPoint>, Double>>();
	private double threshold = 0. ;

	public final void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public static void main(String[] args) {
		MergeNoiseCSVFile readNoiseFile = new MergeNoiseCSVFile();
		readNoiseFile.run();
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public void setReceiverPointsFile(String receiverPointsFile) {
		this.receiverPointsFile = receiverPointsFile;
	}

	public final void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

	public final void setLabel(String label) {
		this.label = label;
	}

	public final void run() {
		// lv final. kai

		String outputPath = workingDirectory;

		String outputFile = outputPath + label + "_merged.csv";


		try ( BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile)) ){
			// so-called "try-with-resources". Kai

			for (double time = startTime; time <= endTime; time = time + timeBinSize) {

				System.out.println("Reading time bin: " + time);

				//				String fileName = workingDirectory + "100." + label + "_" + Double.toString(time) + ".csv";
				String fileName = workingDirectory + label + "_" + Double.toString(time) + ".csv";
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

			System.out.println("Reading receiver points file");

			while( (line = br.readLine()) != null){

				if (lineCounter % 10000 == 0.) {
					System.out.println("# " + lineCounter);
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

			Logger.getLogger(this.getClass()).info( "writing to " + outputFile ) ;

			switch( this.outputFormat ) {
			// yy should probably become different classes. kai
			case ihab:
				bw.write("Receiver Point Id;x;y");
				for (double time = startTime; time <= endTime; time = time + timeBinSize) {
					bw.write(";" + label + "_" + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS));
				}
				break;
			case xyt:
				bw.write("Receiver Point Id;x;y;time;" + label);
				break;
			default:
				throw new RuntimeException("not implemented") ;
			}

			bw.newLine();

			// fill table
			switch( this.outputFormat ) {
			case ihab:
				for (Id<ReceiverPoint> rp : time2rp2value.get(endTime).keySet()) {
					bw.write(rp.toString() + ";" + rp2Coord.get(rp).getX() + ";" + rp2Coord.get(rp).getY());
					for (double time = startTime; time <= endTime; time = time + timeBinSize) {
						bw.write(";" + time2rp2value.get(time).get(rp));
					}
					bw.newLine();
				}				
				break;
			case xyt:
				for (double time = startTime; time <= endTime; time = time + timeBinSize) {
					for (Id<ReceiverPoint> rp : time2rp2value.get(endTime).keySet()) {
						final Double value = time2rp2value.get(time).get(rp);
						if ( value > threshold ) {
							bw.write(rp.toString() + ";" + rp2Coord.get(rp).getX() + ";" + rp2Coord.get(rp).getY());
							bw.write(";" + time + ";" + value);
							bw.newLine();
						}
					}
				}	
				break ;
			default:
				throw new RuntimeException("not implemented") ;
			}

			bw.close();
			System.out.println("Output written to " + outputFile);

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		//		String time = "16:00:00";
		//		String qGisProjectFile = "immission.qgs";
		//		
		//		QGisWriter writer = new QGisWriter(TransformationFactory.DHDN_GK4, workingDirectory);
		//			
		//// ################################################################################################################################################
		//		double[] extent = {4568808,5803042,4622772,5844280};
		//		writer.setExtent(extent);
		//				
		//		VectorLayer noiseLayer = new VectorLayer("noise", outputFile, QGisConstants.geometryType.Point, true);
		//		noiseLayer.setDelimiter(";");
		//		noiseLayer.setXField("x");
		//		noiseLayer.setYField("y");
		//		
		//		NoiseRenderer renderer = new NoiseRenderer(noiseLayer);
		//		renderer.setRenderingAttribute("immission_" + time);
		//		
		//		writer.addLayer(noiseLayer);
		//		
		//// ################################################################################################################################################
		//		
		//		writer.write(qGisProjectFile);

	}

}
