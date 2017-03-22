/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleReader.java
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

package playground.southafrica.freight.digicore.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.algorithms.AddVehicleToContainerAlgorithm;
import playground.southafrica.freight.digicore.io.algorithms.DigicoreVehiclesAlgorithm;

public class DigicoreVehiclesReader extends MatsimXmlParser {
	private final static String DIGICORE_VEHICLES_V1 = "digicoreVehicles_v1.dtd";
	private final static String DIGICORE_VEHICLES_V2 = "digicoreVehicles_v2.dtd";
	private final static Logger LOG = Logger.getLogger(DigicoreVehiclesReader.class);
	private MatsimXmlParser delegate = null;
	private DigicoreVehicles vehicles;
	private List<DigicoreVehiclesAlgorithm> algorithms;

	
	/**
	 * Creates a new reader for Digicore vehicle files.
	 */
	public DigicoreVehiclesReader(DigicoreVehicles vehicles) {
		this.vehicles = vehicles;
		
		/* Add the default algorithm. */
		algorithms = new ArrayList<DigicoreVehiclesAlgorithm>();
		algorithms.add(new AddVehicleToContainerAlgorithm(vehicles));
	}
	
	
	/**
	 * Removes all the current {@link DigicoreVehiclesAlgorithm}s.
	 */
	public void clearAlgorithms(){
		this.algorithms = new ArrayList<>();
	}
	
	
	/**
	 * Adds an algorithm. This method checks that there is not a duplicate
	 * instance of the default {@link AddVehicleToContainerAlgorithm}.
	 */
	public void addAlgorithm(DigicoreVehiclesAlgorithm algorithm){
		if(algorithm instanceof AddVehicleToContainerAlgorithm){
			/* Check if an existing default algorithm isn't loaded already. */
			boolean duplicate = false;
			Iterator<DigicoreVehiclesAlgorithm> iterator = this.algorithms.iterator();
			while(iterator.hasNext() & !duplicate){
				DigicoreVehiclesAlgorithm instance = iterator.next();
				if(instance instanceof AddVehicleToContainerAlgorithm){
					LOG.error("Duplicate default algorithm");
					throw new IllegalArgumentException("This should not happen. " + 
							"Vehicles will be added multiple times to the same " + 
							"container, which will result in later errors.");
				}
			}
			if(!duplicate){
				this.algorithms.add(algorithm);
			}
		} else{
			this.algorithms.add(algorithm);
		}
	}
	
	/**
	 * Returns the current number of algorithms.
	 * 
	 * @return
	 */
	public int getNumberOfAlgorithms(){
		return this.algorithms.size();
	}
	
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		this.delegate.startTag(name, atts, context);
	}

	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}
	
	
	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only digicoreVehicles-type is v1
		if (DIGICORE_VEHICLES_V1.equals(doctype)) {
			this.delegate = new DigicoreVehiclesReader_v1(this.vehicles);
			LOG.info("Using digicoreVehicles_v1 reader.");
			LOG.warn("Currently V1 vehicles do not support algorithms.");
			LOG.warn("Only the default (adding vehicle to container) is applied.");
		} else if(DIGICORE_VEHICLES_V2.equals(doctype)) {
			this.delegate = new DigicoreVehiclesReader_v2(this.vehicles, this.algorithms);
			LOG.info("Using digicoreVehicles_v2 reader.");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
		
		/* Report the available/loaded algorithms. */
		LOG.info("The following algorithms will be applied to each vehicle:");
		for(DigicoreVehiclesAlgorithm algorithm : this.algorithms){
			LOG.info("   " + algorithm.getClass().toString());
		}
	}

}

