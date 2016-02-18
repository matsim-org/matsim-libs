/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.tmp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to extract all the observed activities from the Digicore activity
 * chains, and checking if they fall within the ten validation areas for which
 * we will do clustering parameter validation.
 *
 * @author jwjoubert
 */
public class ExtractAllStudyAreaActivities {
	private final static Logger LOG = Logger.getLogger(ExtractAllStudyAreaActivities.class);

	public static void main(String[] args) {
		Header.printHeader(ExtractAllStudyAreaActivities.class.toString(), args);
		
		String inputFolder = args[0];
		String outputFile = args[1];
		Double HEX_WIDTH = Double.parseDouble(args[2]); 
		Integer numberOfThreads = Integer.parseInt(args[3]);
		
		/* Build the QuadTree covering the ten study areas for validation. */
		QuadTree<Coord> qt = buildQuadTree();
		
		/* Get all the files */
		List<File> listOfFiles = FileUtils.sampleFiles(new File(inputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		
		Counter counter = new Counter("   vehicles # ");
		
		/* Set up the multi-threaded infrastructure. */
		LOG.info("Setting up multi-threaded infrastructure");
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<List<Coord>>> jobs = new ArrayList<Future<List<Coord>>>();
		
		LOG.info("Processing the vehicle files...");
		for(File file : listOfFiles){
			Callable<List<Coord>> job = new ExtractorCallable(qt, file, counter, HEX_WIDTH);
			Future<List<Coord>> result = threadExecutor.submit(job);
			jobs.add(result);
		}
		counter.printCounter();
		LOG.info("Done processing vehicle files...");
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}

		/* Consolidate the output */
		LOG.info("Consolidating output...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("Long,Lat,X,Y");
			bw.newLine();
			
			for(Future<List<Coord>> job : jobs){
				List<Coord> result = job.get();
				for(Coord coord : result){
					Coord actCoordWgs84 = ct.transform(coord);
					bw.write(String.format("%.6f,%.6f,%.2f,%.2f\n", actCoordWgs84.getX(), actCoordWgs84.getY(), coord.getX(), coord.getY()));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Couldn't get thread job result.");
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException("Couldn't get thread job result.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		counter.printCounter();
		
		Header.printFooter();
	}

	/**
	 * Building a basic QuadTree containing just the centroid coordinates of the
	 * ten validation areas. This is done for the Nelson Mandela Bay Metropole.
	 * 
	 * @param qt
	 * @param coord
	 * @return
	 */
	private static QuadTree<Coord> buildQuadTree(){
		QuadTree<Coord> qt = new QuadTree<Coord>(130000.0, -3707000.0, 152000.0, -3684000.0);

		final double y9 = -3685018.8482;
		qt.put(130048.2549,-3685018.8482, new Coord(130048.2549, y9));
		final double y8 = -3702339.3562;
		qt.put(148048.2549,-3702339.3562, new Coord(148048.2549, y8));
		final double y7 = -3704504.4197;
		qt.put(148798.2549,-3704504.4197, new Coord(148798.2549, y7));
		final double y6 = -3706669.4833;
		qt.put(149548.2549,-3706669.4833, new Coord(149548.2549, y6));
		final double y5 = -3706669.4833;
		qt.put(151048.2549,-3706669.4833, new Coord(151048.2549, y5));
		final double y4 = -3701473.3308;
		qt.put(148048.2549,-3701473.3308, new Coord(148048.2549, y4));
		final double y3 = -3697143.2038;
		qt.put(146548.2549,-3697143.2038, new Coord(146548.2549, y3));
		final double y2 = -3704937.4325;
		qt.put(146548.2549,-3704937.4325, new Coord(146548.2549, y2));
		final double y1 = -3705803.4579;
		qt.put(148048.2549,-3705803.4579, new Coord(148048.2549, y1));
		final double y = -3684152.8228;
		qt.put(130048.2549,-3684152.8228, new Coord(130048.2549, y));

		return qt;
	}
	
	/**
	 * Implementing a multi-threaded analysis for extracting the activities from
	 * activity chains. Each thread is passed a vehicle file, which is parsed,
	 * and then analysed.
	 * 
	 * @author jwjoubert
	 */
	private static class ExtractorCallable implements Callable<List<Coord>>{
		private final QuadTree<Coord> qt;
		private final File file;
		public Counter counter;
		private final double width;
		
		public ExtractorCallable(QuadTree<Coord> qt, File file, Counter counter, double width) {
			this.qt = qt;
			this.file = file;
			this.counter = counter;
			this.width = width;
		}

		@Override
		public List<Coord> call() throws Exception {
			List<Coord> list = new ArrayList<Coord>();

			/* Parse the vehicle from file. */
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(file.getAbsolutePath());
			DigicoreVehicle vehicle = dvr.getVehicle();

			/* Check how far EACH activity is. If it is within the threshold,
			 * then write it to file. */
			for(DigicoreChain chain : vehicle.getChains()){
				for(DigicoreActivity act : chain.getAllActivities()){
					Coord actCoord = act.getCoord();
					Coord closest = qt.getClosest(actCoord.getX(), actCoord.getY());
					double dist = CoordUtils.calcEuclideanDistance(actCoord, closest);
					if(dist <= this.width){
						list.add(actCoord);
					}
				}
			}

			counter.incCounter();
			return list;
		}
	}

}
