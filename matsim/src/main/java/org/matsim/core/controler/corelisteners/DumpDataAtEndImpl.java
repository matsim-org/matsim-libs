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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
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
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.util.Collections;
import java.util.Map;

@Singleton
final class DumpDataAtEndImpl implements DumpDataAtEnd, ShutdownListener {
	private static final Logger log = Logger.getLogger( DumpDataAtEndImpl.class );

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

	@Inject
	private Map<Class<?>,AttributeConverter<?>> attributeConverters = Collections.emptyMap();

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if ( event.isUnexpected() ) {
			return ;
		}
		dumpPlans();
		dumpNetwork();
		dumpConfig();
		dumpFacilities();
		dumpNetworkChangeEvents();

		dumpTransitSchedule();
		dumpTransitVehicles();
		dumpVehicles();
		dumpHouseholds();
		dumpLanes();
		dumpCounts();

		if (!event.isUnexpected() && vspConfig.isWritingOutputEvents()) {
			dumpOutputEvents();
		}
	}

	private void dumpOutputEvents() {
		try {
			File toFile = new File(	controlerIO.getOutputFilename("output_events.xml.gz"));
			File fromFile = new File(controlerIO.getIterationFilename(controlerConfigGroup.getLastIteration(), "events.xml.gz"));
			IOUtils.copyFile(fromFile, toFile);
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).error("writing output events did not work; probably parameters were such that no events were "
					+ "generated in the final iteration") ;
		}
	}

	private void dumpCounts() {
		try {
			if ( counts != null ) {
				final String inputCRS = config.counts().getInputCRS();
				final String internalCRS = config.global().getCoordinateSystem();

				if ( inputCRS == null ) {
					new CountsWriter(counts).write(controlerIO.getOutputFilename(Controler.FILENAME_COUNTS));
				}
				else {
					log.info( "re-projecting counts from "+internalCRS+" back to "+inputCRS+" for export" );

					final CoordinateTransformation transformation =
							TransformationFactory.getCoordinateTransformation(
									internalCRS,
									inputCRS );

					new CountsWriter( transformation , counts).write(controlerIO.getOutputFilename(Controler.FILENAME_COUNTS));
				}
			}
		} catch ( Exception ee ) {}
	}

	private void dumpLanes() {
		try {
			new LaneDefinitionsWriter20(lanes).write(controlerIO.getOutputFilename(Controler.FILENAME_LANES));
		} catch ( Exception ee ) {}
	}

	private void dumpHouseholds() {
		try {
			new HouseholdsWriterV10(households).writeFile(controlerIO.getOutputFilename(Controler.FILENAME_HOUSEHOLDS));
		} catch ( Exception ee ) {}
	}

	private void dumpVehicles() {
		try {
			new VehicleWriterV1(vehicles).writeFile(controlerIO.getOutputFilename("output_vehicles.xml.gz"));
		} catch ( Exception ee ) {}
	}

	private void dumpTransitVehicles() {
		try {
			if ( transitVehicles != null ) {
				new VehicleWriterV1(transitVehicles).writeFile(controlerIO.getOutputFilename("output_transitVehicles.xml.gz"));
			}
		} catch ( Exception ee ) {}
	}

	private void dumpTransitSchedule() {
		try {
			if ( transitSchedule != null ) {
				final String inputCRS = config.transit().getInputScheduleCRS();
				final String internalCRS = config.global().getCoordinateSystem();

				if ( inputCRS == null ) {
					new TransitScheduleWriter(transitSchedule).writeFile(controlerIO.getOutputFilename("output_transitSchedule.xml.gz"));
				}
				else {
					log.info( "re-projecting transit schedule from "+internalCRS+" back to "+inputCRS+" for export" );

					final CoordinateTransformation transformation =
							TransformationFactory.getCoordinateTransformation(
									internalCRS,
									inputCRS );

					new TransitScheduleWriter( transformation , transitSchedule ).writeFile(controlerIO.getOutputFilename("output_transitSchedule.xml.gz"));
				}
			}
		} catch ( Exception ee ) { }
	}

	private void dumpNetworkChangeEvents() {
		if (config.network().isTimeVariantNetwork()) {
			new NetworkChangeEventsWriter().write(controlerIO.getOutputFilename("output_change_events.xml.gz"),
					NetworkUtils.getNetworkChangeEvents(((Network) network)));
		}
	}

	private void dumpFacilities() {
		// dump facilities
		try {
			final String inputCRS = config.facilities().getInputCRS();
			final String internalCRS = config.global().getCoordinateSystem();

			if ( inputCRS == null ) {
				new FacilitiesWriter(activityFacilities).write(controlerIO.getOutputFilename("output_facilities.xml.gz"));
			}
			else {
				log.info( "re-projecting facilities from "+internalCRS+" back to "+inputCRS+" for export" );

				final CoordinateTransformation transformation =
						TransformationFactory.getCoordinateTransformation(
								internalCRS,
								inputCRS );

				new FacilitiesWriter( transformation , activityFacilities ).write(controlerIO.getOutputFilename("output_facilities.xml.gz"));
			}
		} catch ( Exception ee ) {}
	}

	private void dumpConfig() {
		// dump config
		new ConfigWriter(config).write(controlerIO.getOutputFilename(Controler.FILENAME_CONFIG));
	}

	private void dumpNetwork() {
		// dump network
		if ( config.network().getInputCRS() == null ) {
			new NetworkWriter(network).write(controlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
		}
		else {
			log.info( "re-projecting network from "+config.global().getCoordinateSystem()+" back to "+config.network().getInputCRS()+" for export" );

			final CoordinateTransformation transformation =
					TransformationFactory.getCoordinateTransformation(
							config.global().getCoordinateSystem(),
							config.network().getInputCRS() );
			new NetworkWriter( transformation , network ).write(controlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
		}
	}

	private void dumpPlans() {
		// dump plans

		final String inputCRS = config.plans().getInputCRS();
		final String internalCRS = config.global().getCoordinateSystem();

		if ( inputCRS == null ) {
			new PopulationWriter(population, network).write(controlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
		}
		else {
				log.info( "re-projecting population from "+internalCRS+" back to "+inputCRS+" for export" );

				final CoordinateTransformation transformation =
						TransformationFactory.getCoordinateTransformation(
								internalCRS,
								inputCRS );

			new PopulationWriter(transformation , population, network).write(controlerIO.getOutputFilename(Controler.FILENAME_POPULATION));

		}

		final ObjectAttributes personAttributes = population.getPersonAttributes();
		if ( personAttributes!=null ) {
			ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(personAttributes) ;
			writer.setPrettyPrint(true);
			writer.putAttributeConverters( attributeConverters );
			writer.writeFile( controlerIO.getOutputFilename( Controler.FILENAME_PERSON_ATTRIBUTES ) );
		}
	}

}
