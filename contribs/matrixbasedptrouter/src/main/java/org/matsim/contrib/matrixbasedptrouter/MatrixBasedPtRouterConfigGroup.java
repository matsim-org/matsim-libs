/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.matrixbasedptrouter;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public final class MatrixBasedPtRouterConfigGroup extends ReflectiveConfigGroup {
	static final Logger log = Logger.getLogger(MatrixBasedPtRouterConfigGroup.class) ;

	public static final String GROUP_NAME="matrixBasedPtRouter" ;

	public static final String PT_STOPS = "ptStopsFile"; // tnicolai: originally, this was named "ptStops" in the matsim config. old config files need to be adjusted
	public static final String USING_PT_STOPS = "usingPtStops";
	public static final String PT_TRAVEL_TIMES = "ptTravelTimesFile"; // tnicolai: originally, this was named "ptTravelTimes" in the matsim config. old config files need to be adjusted
	public static final String PT_TRAVEL_DISTANCES = "ptTravelDistancesFile"; // tnicolai: originally, this was named "ptTravelDistances" in the matsim config. old config files need to be adjusted
	public static final String PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH = "usingTravelTimesAndDistances"; // tnicolai: originally, this was named "useTravelTimesAndDistances" in the matsim config. old config files need to be adjusted
	private String ptStopsInputFile;
	private String ptTravelTimesInputFile;
	private String ptTravelDistancesInputFile;
	private boolean usingTravelTimesAndDistances = false ;
	private boolean usingPtStops = false ;

	public MatrixBasedPtRouterConfigGroup() {
		super(GROUP_NAME);
	}

	@StringSetter(PT_STOPS)
	public void setPtStopsInputFile(String ptStops){
		this.ptStopsInputFile = ptStops;
	}
	@StringGetter(PT_STOPS)
	public String getPtStopsInputFile(){
		return this.ptStopsInputFile;
	}
	@StringSetter(PT_TRAVEL_TIMES)
	public void setPtTravelTimesInputFile(String ptTravelTimes){
		this.ptTravelTimesInputFile = ptTravelTimes;
	}
	@StringGetter(PT_TRAVEL_TIMES)
	public String getPtTravelTimesInputFile(){
		return this.ptTravelTimesInputFile;
	}
	@StringSetter(PT_TRAVEL_DISTANCES )
	public void setPtTravelDistancesInputFile(String ptTravelDistances){
		this.ptTravelDistancesInputFile = ptTravelDistances;
	}
	@StringGetter(PT_TRAVEL_DISTANCES )
	public String getPtTravelDistancesInputFile(){
		return this.ptTravelDistancesInputFile;
	}
	@StringSetter(PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH)
	public void setUsingTravelTimesAndDistances( boolean val ) {
		this.usingTravelTimesAndDistances = val ;
	}
	@StringGetter(PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH)
	public boolean isUsingTravelTimesAndDistances() {
		return this.usingTravelTimesAndDistances ;
	}
	@StringGetter(USING_PT_STOPS)
	public boolean isUsingPtStops() {
		return usingPtStops;
	}
	@StringSetter(USING_PT_STOPS)
	public void setUsingPtStops(boolean usingPtStops) {
		this.usingPtStops = usingPtStops;
	}

	@Override
	protected void checkConsistency() {
		boolean problem = false ;
		if( isUsingPtStops() ) {
			log.info(MatrixBasedPtRouterConfigGroup.USING_PT_STOPS + " switch is set to true. Trying to find pt stops file ...");
			// checking for pt stops
			if( getPtStopsInputFile() != null){
				File ptStopsFile = new File(getPtStopsInputFile());
				if(ptStopsFile.exists()){
					log.info("Found pt stops file " + getPtStopsInputFile());
				} else {
					problem = true ;
					log.error("Pt stops file " + getPtStopsInputFile() + " not found although switch is set to true! Will abort ...");

				}

				// checking for other input files
				if( isUsingTravelTimesAndDistances() ) {
					log.info(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " switch is set to true. Trying to find travel times and distances files ...");

					if(new File( getPtTravelTimesInputFile() ).exists() && new File( getPtTravelDistancesInputFile() ).exists()){
						log.info("Found travel times and travel distances input files:");
						log.info("Travel times input file: " + getPtTravelTimesInputFile());
						log.info("Travel distances input file: " + getPtTravelDistancesInputFile() );
					} else {
						problem = true ;
						log.error("Travel times and/or travel distances input files not found!  Will abort ...");
						log.warn("Travel times input file name: " + getPtTravelTimesInputFile());
						log.warn("Travel distances input file name: " + getPtTravelDistancesInputFile());
					}
				} else {
					log.info(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " switch is set to false. Additional travel times and distances files will not be read!");
				}

			} else {
				problem = true ;
				log.warn("No pt stops file given although switch is set to true!  Will abort ...");
			}
		} else {
			log.info(MatrixBasedPtRouterConfigGroup.USING_PT_STOPS + " switch is set to false. Matrix based pt router will not be initialized.");
		}
		if ( problem ) {
			throw new RuntimeException("found fatal problem in matrix based pt router initialization; aborting ...") ;
		}

	}

}
