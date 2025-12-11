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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
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
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesWriter;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.Collections;
import java.util.Map;

@Singleton
final class DumpDataAtEndImpl implements DumpDataAtEnd, ShutdownListener {
	private static final Logger log = LogManager.getLogger( DumpDataAtEndImpl.class );

	@Inject
	private Config config;

	@Inject
	private ControllerConfigGroup controllerConfigGroup;

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
	private Scenario scenario;

	@Inject
	private OutputDirectoryHierarchy controlerIO;

	@Inject
	private Map<Class<?>,AttributeConverter<?>> attributeConverters = Collections.emptyMap();
	// (yyyy Feels plausible to have them but how can they be set?  And isn't there a more global way to set the attribute converters?  kai, feb'24)

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

		if (!event.isUnexpected() && this.vspConfig.isWritingOutputEvents() && (this.controllerConfigGroup.getWriteEventsInterval()!=0)) {
			dumpOutputEvents(event.getIteration());
		}
		dumpOutputTrips(event.getIteration());
        dumpOutputLegs(event.getIteration());
		dumpOutputActivities(event.getIteration());
		dumpExperiencedPlans(event.getIteration());

		if (controllerConfigGroup.getCleanItersAtEnd() == ControllerConfigGroup.CleanIterations.delete) {
			this.controlerIO.deleteIterationDirectory();
		}
	}

	private void dumpOutputEvents(int iteration) {
		for (ControllerConfigGroup.EventsFileFormat format : this.controllerConfigGroup.getEventsFileFormats()) {
			try{
				Controler.DefaultFiles file;
				switch (format) {
					case xml:
						file = Controler.DefaultFiles.events;
						break;
					case pb:
						file = Controler.DefaultFiles.eventsPb;
						break;
					case json:
						file = Controler.DefaultFiles.eventsJson;
						break;
					default:
						continue;
				}

				IOUtils.copyFile(this.controlerIO.getIterationFilename(iteration, file),
						this.controlerIO.getOutputFilename(file));
			} catch (Exception ee) {
				LogManager.getLogger(this.getClass()).error("writing output events did not work; probably parameters were such that no events were "
						+ "generated in the final iteration");
			}
		}
	}

	private void dumpOutputTrips(int iteration) {
		try {
			IOUtils.copyFile(this.controlerIO.getIterationFilename(iteration, Controler.DefaultFiles.tripscsv),
					this.controlerIO.getOutputFilename(Controler.DefaultFiles.tripscsv));
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass()).error("writing output trips did not work; probably parameters were such that no trips CSV were "
					+ "generated in the final iteration");
		}
	}

	private void dumpOutputActivities(int iteration) {
		try {
			IOUtils.copyFile(this.controlerIO.getIterationFilename(iteration, Controler.DefaultFiles.activitiescsv),
					this.controlerIO.getOutputFilename(Controler.DefaultFiles.activitiescsv));
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass()).error("writing output activities did not work; probably parameters were such that no activities CSV were "
					+ "generated in the final iteration");
		}
	}

    private void dumpOutputLegs(int iteration) {
        try {
			IOUtils.copyFile(this.controlerIO.getIterationFilename(iteration, Controler.DefaultFiles.legscsv),
					this.controlerIO.getOutputFilename(Controler.DefaultFiles.legscsv));
        } catch (Exception ee) {
            LogManager.getLogger(this.getClass()).error("writing output legs did not work; probably parameters were such that no legs CSV were "
                    + "generated in the final iteration");
        }
    }

	private void dumpExperiencedPlans(int iteration) {
		if (this.config.scoring().isWriteExperiencedPlans() ) {
			try {
				IOUtils.copyFile(this.controlerIO.getIterationFilename(iteration, Controler.DefaultFiles.experiencedPlans),
						this.controlerIO.getOutputFilename(Controler.DefaultFiles.experiencedPlans));
			} catch ( Exception ee ) {
				LogManager.getLogger(this.getClass()).error("writing output experienced plans did not work; probably parameters were such that they "
						+ "were not generated in the final iteration", ee);
			}
		}
	}

	private void dumpCounts() {
		try {
			if (this.counts != null ) {
//				final String inputCRS = this.config.counts().getInputCRS();
//				final String internalCRS = this.config.global().getCoordinateSystem();

//				if ( inputCRS == null ) {
					new CountsWriter(this.counts).write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.counts));
//				}
//				else {
//					log.info( "re-projecting counts from "+internalCRS+" back to "+inputCRS+" for export" );
//
//					final CoordinateTransformation transformation =
//							TransformationFactory.getCoordinateTransformation(
//									internalCRS,
//									inputCRS );
//
//					new CountsWriter( transformation , this.counts).write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.counts));
//				}
				// we said at some point that we are no longer projecting back for the final output.  For Counts, this was so far not
				// adapted in this direction.  I assume that the reason was that the CountsWriter took a transformation, not the
				// coordinate string itself, and so it was not possible to automatically at the CRS string as attribute into the file.
				//  I adapted that now (I hope).  kai, feb'24
			}
		} catch ( Exception ee ) {
			log.error("Exception writing counts.", ee);
		}
	}

	private void dumpLanes() {
		try {
			if ( this.lanes!=null && !this.lanes.getLanesToLinkAssignments().isEmpty() ){
				new LanesWriter( this.lanes ).write( this.controlerIO.getOutputFilename( Controler.DefaultFiles.lanes ) );
			}
		} catch ( Exception ee ) {
			log.error("Exception writing lanes.", ee);
		}
	}

	private void dumpHouseholds() {
		try {
			HouseholdsWriterV10 writer = new HouseholdsWriterV10(this.households);
			writer.putAttributeConverters(this.attributeConverters);
			writer.writeFile(this.controlerIO.getOutputFilename(Controler.DefaultFiles.households));
		} catch ( Exception ee ) {
			log.error("Exception writing households.", ee);
		}
	}

	private void dumpVehicles() {
		try {
			new MatsimVehicleWriter(this.vehicles).writeFile(this.controlerIO.getOutputFilename(Controler.DefaultFiles.vehicles));
			Vehicles allVehicles = VehicleUtils.getOrCreateAllvehicles( scenario );
			if ( allVehicles!=null && !allVehicles.getVehicleTypes().isEmpty() ){
				new MatsimVehicleWriter( allVehicles ).writeFile( this.controlerIO.getOutputFilename( Controler.DefaultFiles.allVehicles ) );
			}
		} catch ( Exception ee ) {
			log.error("Exception writing vehicles.", ee);
		}
	}

	private void dumpTransitVehicles() {
		try {
			if (this.transitVehicles != null ) {
				new MatsimVehicleWriter(this.transitVehicles).writeFile(this.controlerIO.getOutputFilename(Controler.DefaultFiles.transitVehicles));
			}
		} catch ( Exception ee ) {
			log.error("Exception writing transit vehicles.", ee);
		}
	}

	private void dumpTransitSchedule() {
		try {
			if (this.transitSchedule != null ) {
				new TransitScheduleWriter(this.transitSchedule).writeFile(this.controlerIO.getOutputFilename(Controler.DefaultFiles.transitSchedule));
			}
		} catch ( Exception ee ) {
			log.error("Exception writing transit schedule.", ee);
		}
	}

	private void dumpNetworkChangeEvents() {
		if (this.config.network().isTimeVariantNetwork()) {
			new NetworkChangeEventsWriter().write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.changeEvents),
					NetworkUtils.getNetworkChangeEvents(this.network));
		}
	}

	private void dumpFacilities() {
		// dump facilities
		try {
			new FacilitiesWriter(this.activityFacilities).write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.facilities));
		} catch ( Exception ee ) {
			log.error("Exception writing facilities.", ee);
		}
	}

	private void dumpConfig() {
		// dump config
		new ConfigWriter(this.config).write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.config, ControllerConfigGroup.CompressionType.none));
		new ConfigWriter(this.config, ConfigWriter.Verbosity.minimal).write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.configReduced, ControllerConfigGroup.CompressionType.none));
	}

	private void dumpNetwork() {
		// dump network
		new NetworkWriter(this.network).write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.network));
	}

	private void dumpPlans() {
		// dump plans

		final PopulationWriter writer = new PopulationWriter(this.population, this.network);
		writer.putAttributeConverters(this.attributeConverters);
		writer.write(this.controlerIO.getOutputFilename(Controler.DefaultFiles.population));
	}

}
