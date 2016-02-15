/* *********************************************************************** *
 * project: org.matsim.*
 * DumpDataAtEnd.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.controler.corelisteners;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.io.File;

@Singleton
final class DumpDataAtEndImpl implements DumpDataAtEnd, ShutdownListener {

	@Inject
	private Config config;

	@Inject
	private ControlerConfigGroup controlerConfigGroup;

	@Inject
	private VspExperimentalConfigGroup vspConfig;

	@Inject
	private Network network;

	@Inject
	private Population population;

	@Inject
	private ActivityFacilities activityFacilities;

	@Inject
	private Vehicles vehicles;

	@Inject(optional = true)
	private TransitSchedule transitSchedule = null;

	@Inject(optional = true)
	@Transit
	private Vehicles transitVehicles = null;

	@Inject(optional = true)
	private Counts<Link> counts = null;

	@Inject
	private Households households;

	@Inject
	private Lanes lanes;

	@Inject
	private OutputDirectoryHierarchy controlerIO;

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if ( event.isUnexpected() ) {
			return ;
		}
		
		// dump plans
		new PopulationWriter(population, network).write(controlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
		final ObjectAttributes personAttributes = population.getPersonAttributes();
		if ( personAttributes!=null ) {
			ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(personAttributes) ;
			writer.setPrettyPrint(true);
			writer.writeFile( controlerIO.getOutputFilename( Controler.FILENAME_PERSON_ATTRIBUTES ) );
		}
		// dump network
		new NetworkWriter(network).write(controlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
		// dump config
		new ConfigWriter(config).write(controlerIO.getOutputFilename(Controler.FILENAME_CONFIG));
		// dump facilities
		try {
			new FacilitiesWriter(activityFacilities).write(controlerIO.getOutputFilename("output_facilities.xml.gz"));
		} catch ( Exception ee ) {}
		if (config.network().isTimeVariantNetwork()) {
			new NetworkChangeEventsWriter().write(controlerIO.getOutputFilename("output_change_events.xml.gz"),
					((NetworkImpl) network).getNetworkChangeEvents());
		}
		try {			
			if ( transitSchedule != null ) {
				new TransitScheduleWriter(transitSchedule).writeFile(controlerIO.getOutputFilename("output_transitSchedule.xml.gz"));
			}
		} catch ( Exception ee ) { }
		try {
			if ( transitVehicles != null ) {
				new VehicleWriterV1(transitVehicles).writeFile(controlerIO.getOutputFilename("output_transitVehicles.xml.gz"));
			}
		} catch ( Exception ee ) {} 
		try {
			new VehicleWriterV1(vehicles).writeFile(controlerIO.getOutputFilename("output_vehicles.xml.gz"));
		} catch ( Exception ee ) {}
		try {
			new HouseholdsWriterV10(households).writeFile(controlerIO.getOutputFilename(Controler.FILENAME_HOUSEHOLDS));
		} catch ( Exception ee ) {}
		try {
			new LaneDefinitionsWriter20(lanes).write(controlerIO.getOutputFilename(Controler.FILENAME_LANES));
		} catch ( Exception ee ) {}
		try {
			if ( counts != null ) {
				new CountsWriter(counts).write( controlerIO.getOutputFilename( Controler.FILENAME_COUNTS ) );
			}
		} catch ( Exception ee ) {}
		if (!event.isUnexpected() && vspConfig.isWritingOutputEvents()) {
			try {
				File toFile = new File(	controlerIO.getOutputFilename("output_events.xml.gz"));
				File fromFile = new File(controlerIO.getIterationFilename(controlerConfigGroup.getLastIteration(), "events.xml.gz"));
				IOUtils.copyFile(fromFile, toFile);
			} catch ( Exception ee ) {
				Logger.getLogger(this.getClass()).error("writing output events did not work; probably parameters were such that no events were "
						+ "generated in the final iteration") ;
			}
		}
	}

}
