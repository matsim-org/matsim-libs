/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreChainExtractor.java
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

package playground.southafrica.freight.digicore.extract.step3_extract;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicorePosition;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleWriter;


public class DigicoreChainExtractor implements Runnable {
	private final File vehicleFile;
	private final File outputFolder;
	private final double thresholdMinorMajor;
	private final double thresholdActivityDuration;
	private final List<String> ignitionOn;
	private final List<String> ignitionOff;
	private final String crs;
	private CoordinateTransformation ct;
	private DigicoreVehicle vehicle;

	private final Logger log = Logger.getLogger(DigicoreChainExtractor.class);
	private Counter threadCounter;

	@Override
	public void run() {
		/* Identify vehicle Id and create new vehicle. */
		String name = vehicleFile.getName().substring(0, vehicleFile.getName().indexOf("."));
		vehicle = new DigicoreVehicle(Id.create(name, Vehicle.class));

		DigicoreChain chain = new DigicoreChain();
		DigicoreActivity activity = null;

		int invalidGps = 0;
		List<String[]> activityBuffer = null;
		List<String[]> tripBuffer = null;
		List<String[]> previousTripBuffer = null;
		boolean move = true;
		boolean firstRecord = true;
		try {
			BufferedReader br = IOUtils.getBufferedReader(vehicleFile.getAbsolutePath());
			try {
				String line = br.readLine();
				while((line = br.readLine()) != null){
					String [] sa = line.split(",");

					/* Check if the coordinate is within bounds... ignore record 
					 * if not. */
					double x = Double.parseDouble(sa[2]);
					double y = Double.parseDouble(sa[3]);
					if(validSACoord(x, y)){
						if(this.ignitionOn.contains( sa[4] )){
							move = true;
						} else if(this.ignitionOff.contains( sa[4] )){
							move = false;
						} else{
							log.warn("Could not identify status " + sa[4] + " for vehicle " + name);
						}

						/* Initialise the (correct) buffer. */
						if(firstRecord){
							if(move){
								tripBuffer = new ArrayList<>();
							} else{
								activityBuffer = new ArrayList<>();
							}
							firstRecord = false;
						}

						/* Process the record. */
						if(move){
							if(activityBuffer == null && tripBuffer != null){
								/* Vehicle is still moving. */
								tripBuffer.add(sa);
							} else if(activityBuffer != null && tripBuffer == null){
								/* Vehicle has started moving. Finish activity. */
								activityBuffer.add(sa);

								/* Check if activity duration exceeds threshold. */
								long duration = (Long.parseLong(activityBuffer.get(activityBuffer.size()-1)[1]) - 
										Long.parseLong(activityBuffer.get(0)[1]));
								if(duration >= this.thresholdActivityDuration){
									/* It qualifies as an activity. */

									if(duration >= this.thresholdMinorMajor){
										activity = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
									} else{
										activity = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
									}

									/* Calculate activity centroid */
									double xSum = 0;
									double ySum = 0;
									for(String[] saa : activityBuffer){
										xSum += Double.parseDouble(saa[2]);
										ySum += Double.parseDouble(saa[3]);
									}
									Coord cOriginal = new Coord(xSum / (double) activityBuffer.size(), ySum / (double) activityBuffer.size());
									Coord cFinal = ct.transform(cOriginal);
									activity.setCoord(cFinal);

									/* Set start- and end time. */
									activity.setStartTime(Double.parseDouble(activityBuffer.get(0)[1]));
									activity.setEndTime(Double.parseDouble(activityBuffer.get(activityBuffer.size()-1)[1]));

									/* 
									 * Add the activity to the chain. 
									 */
									boolean major = activity.getType().equalsIgnoreCase("major");
									if(major){
										/* End current chain and start a new chain. */
										chain.add(activity);
										chain = cleanChain(chain);
										if(chain.isComplete()){
											/* This is a complete chain, add it to the vehicle. */
											vehicle.getChains().add(chain);
										} else{
											/* Drop the current chain. */
										}
										chain = new DigicoreChain();
										chain.add(activity);
									} else{
										/* Just add the minor activity to the current chain. */
										chain.add(activity);
									}

									activityBuffer = null;
									/* Start new trip buffer. */
									tripBuffer = new ArrayList<>();
									tripBuffer.add(sa);
								} else{
									/* It is not considered an activity. Re-instate 
									 * the previous (partial) trip.*/
									tripBuffer = previousTripBuffer;
									if(tripBuffer == null){
										tripBuffer = new ArrayList<>();
									}

									/* Now also remove the previous trace that was
									 * (possibly) already added to the activity chain. */
									if(chain.size() > 0){
										if(chain.get(chain.size()-1) instanceof DigicoreTrace){
											chain.remove(chain.size()-1);
										}
									}

									/* TODO Check if the next line is indeed correct...
									 * Is it accurate to add the (failed) activity 
									 * records to the trip as well? Jan'17 JWJ */

									// FIXME Remove after debugging.
									if(tripBuffer == null || activityBuffer == null){
										log.error("Null");
									}

									tripBuffer.addAll(activityBuffer);
									activityBuffer = null;
								}
							} else{
								log.error("The buffer combination is problematic:");
								log.error("  Is line buffer null?: " + (activityBuffer == null));
								log.error("  Is trip buffer null?: " + (tripBuffer == null));
								throw new IllegalStateException("Buffer states seem wrong.");
							}
						} else {
							if(activityBuffer == null && tripBuffer != null){
								/* Vehicle has just stopped. Finish trip.*/
								DigicoreTrace trace = convertBufferToTrace(tripBuffer, this.crs);
								chain.add(trace);
								previousTripBuffer = tripBuffer;
								tripBuffer = null;

								/* Start activity. */
								activityBuffer = new ArrayList<String[]>();
								activityBuffer.add(sa);

							} else if(activityBuffer != null && tripBuffer == null){
								/* Vehicle is still stationary. */
								activityBuffer.add(sa);

							} else{
								log.error("The buffer combination is problematic:");
								log.error("  Is line buffer null?: " + (activityBuffer == null));
								log.error("  Is trip buffer null?: " + (tripBuffer == null));
								throw new IllegalStateException("Buffer states seem wrong.");
							}
						}
					} else{
						invalidGps++;
					}
				}
			} finally {
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			e.printStackTrace();
			log.error("Vehicle with NullPointerException: " + vehicle.getId().toString());
		}

		if(invalidGps > 0){
			log.warn("Vehicle " + name + " has " + invalidGps + " records with invalid GPS positions.");
		}
		/* Write the vehicle to file if it has at least one chain. This is 
		 * currently (Nov'16) writing version 2 vehicles. When using the
		 * TurnkeyExtractor these files will be removed anyway. */
		if(vehicle.getChains().size() > 0){
			DigicoreVehicleWriter dvw = new DigicoreVehicleWriter(vehicle);
			dvw.write(this.outputFolder.getAbsolutePath() + "/" + name + ".xml.gz");
		}
		threadCounter.incCounter();
	}
	
	private boolean validSACoord(double x, double y){
		boolean valid = false;
		
		if(x >= 0.0 && x <= 45.0 && y >= -36.0 && y <= 0.0){
			valid = true;
		} else{
			log.warn("Out-of-bounds coordinate");
		}
		return valid;
	}

	/**
	 * Converts a given buffer of records into a GPS trace.
	 * @param buffer
	 * @param crs
	 * @return
	 */
	private DigicoreTrace convertBufferToTrace(List<String[]> buffer, String crs){
		DigicoreTrace dt = new DigicoreTrace(crs);
		for(String[] sa : buffer){
			long time = Long.parseLong(sa[1]);
			double lon = Double.parseDouble(sa[2]);
			double lat = Double.parseDouble(sa[3]);

			Coord c = this.ct.transform(CoordUtils.createCoord(lon, lat));

			DigicorePosition dp = new DigicorePosition(time, c.getX(), c.getY());
			dt.add(dp);
		}
		return dt;
	}

	/**
	 * TODO Currently returns exactly the same chain. No cleaning or merging
	 * of activities that are close to one another.
	 * @param chain
	 * @return
	 */
	private DigicoreChain cleanChain(DigicoreChain chain){
		return chain;
	}


	/**
	 * Default coordinate reference systems, both origin and destination, is 
	 * assumed to be `WGS84'.
	 * @param file
	 * @param outputFolder
	 * @param thresholdMinor
	 * @param thresholdActivity
	 * @param ignitionOn
	 * @param ignitionOff
	 * @param crs
	 * @param threadCounter 
	 */
	public DigicoreChainExtractor(File file, File outputFolder, double thresholdMinor, double thresholdActivity, List<String> ignitionOn, List<String> ignitionOff, String crs, Counter threadCounter) {
		this.vehicleFile = file;
		this.outputFolder = outputFolder;
		this.thresholdMinorMajor = thresholdMinor;
		this.thresholdActivityDuration = thresholdActivity;
		this.ignitionOn = ignitionOn;
		this.ignitionOff = ignitionOff;
		this.threadCounter = threadCounter;
		this.crs = crs;
		if(crs == null){
			crs = "Atlantis";
			this.ct = TransformationFactory.getCoordinateTransformation("Atlantis", "Atlantis");
		} else{
			this.ct = TransformationFactory.getCoordinateTransformation("WGS84", crs);
		}
	}

	public DigicoreVehicle getVehicle(){
		return this.vehicle;
	}

	public CoordinateTransformation getCoordinateTransformation(){
		return this.ct;
	}


}

