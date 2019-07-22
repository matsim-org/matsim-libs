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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesWriter;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

		if (!event.isUnexpected() && vspConfig.isWritingOutputEvents() && (controlerConfigGroup.getWriteEventsInterval()!=0)) {
			dumpOutputEvents();
		}
		
		dumpExperiencedPlans() ;
	}

	private void dumpOutputEvents() {
		try {
			File toFile = new File(	controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_EVENTS_XML));
			File fromFile = new File(controlerIO.getIterationFilename(controlerConfigGroup.getLastIteration(), Controler.FILENAME_EVENTS_XML));
			try {
				Files.copy(fromFile.toPath(), toFile.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).error("writing output events did not work; probably parameters were such that no events were "
					+ "generated in the final iteration" );
		}
	}

	private void dumpExperiencedPlans() {
		if ( config.planCalcScore().isWriteExperiencedPlans() ) {
			try {
				File toFile = new File(	controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_EXPERIENCED_PLANS));
				File fromFile = new File(controlerIO.getIterationFilename(controlerConfigGroup.getLastIteration(), Controler.FILENAME_EXPERIENCED_PLANS));
				try {
					Files.copy(fromFile.toPath(), toFile.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} catch ( Exception ee ) {
				Logger.getLogger(this.getClass()).error("writing output experienced plans did not work; probably parameters were such that they "
						+ "were not generated in the final iteration", ee);
			}
		}
	}

	private void dumpCounts() {
		try {
			if ( counts != null ) {
				final String inputCRS = config.counts().getInputCRS();
				final String internalCRS = config.global().getCoordinateSystem();

				if ( inputCRS == null ) {
					new CountsWriter(counts).write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_COUNTS));
				}
				else {
					log.info( "re-projecting counts from "+internalCRS+" back to "+inputCRS+" for export" );

					final CoordinateTransformation transformation =
							TransformationFactory.getCoordinateTransformation(
									internalCRS,
									inputCRS );

					new CountsWriter( transformation , counts).write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_COUNTS));
				}
			}
		} catch ( Exception ee ) {
			log.error("Exception writing counts.", ee);
		}
	}

	private void dumpLanes() {
		try {
			new LanesWriter(lanes).write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_LANES));
		} catch ( Exception ee ) {
			log.error("Exception writing lanes.", ee);
		}
	}

	private void dumpHouseholds() {
		try {
			new HouseholdsWriterV10(households).writeFile(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_HOUSEHOLDS));
		} catch ( Exception ee ) {
			log.error("Exception writing households.", ee);
		}
	}

	private void dumpVehicles() {
		try {
			new VehicleWriterV1(vehicles).writeFile(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_VEHICLES));
		} catch ( Exception ee ) {
			log.error("Exception writing vehicles.", ee);
		}
	}

	private void dumpTransitVehicles() {
		try {
			if ( transitVehicles != null ) {
				new VehicleWriterV1(transitVehicles).writeFile(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_TRANSIT_VEHICLES));
			}
		} catch ( Exception ee ) {
			log.error("Exception writing transit vehicles.", ee);
		}
	}

	private void dumpTransitSchedule() {
		try {
			if ( transitSchedule != null ) {
				final String inputCRS = config.transit().getInputScheduleCRS();
				final String internalCRS = config.global().getCoordinateSystem();

				new TransitScheduleWriter(transitSchedule).writeFile(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_TRANSIT_SCHEDULE));
			}
		} catch ( Exception ee ) {
			log.error("Exception writing transit schedule.", ee);
		}
	}

	private void dumpNetworkChangeEvents() {
		if (config.network().isTimeVariantNetwork()) {
			new NetworkChangeEventsWriter().write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_CHANGE_EVENTS_XML),
					NetworkUtils.getNetworkChangeEvents(network));
		}
	}

	private void dumpFacilities() {
		// dump facilities
		try {
			final String inputCRS = config.facilities().getInputCRS();
			final String internalCRS = config.global().getCoordinateSystem();

			new FacilitiesWriter(activityFacilities).write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_FACILITIES));
		} catch ( Exception ee ) {
			log.error("Exception writing facilities.", ee);
		}
	}

	private void dumpConfig() {
		// dump config
		new ConfigWriter(config).write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_CONFIG));
		new ConfigWriter(config, ConfigWriter.Verbosity.minimal).write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_CONFIG_REDUCED));
	}

	private void dumpNetwork() {
		// dump network
		new NetworkWriter(network).write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_NETWORK));
	}

	private void dumpPlans() {
		// dump plans

		final PopulationWriter writer = new PopulationWriter(population, network);
		writer.putAttributeConverters( attributeConverters );
		writer.write(controlerIO.getOutputFilename(Controler.OUTPUT_PREFIX + Controler.FILENAME_POPULATION));
	}

}
