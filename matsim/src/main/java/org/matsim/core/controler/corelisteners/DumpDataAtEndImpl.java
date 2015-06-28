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

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public final class DumpDataAtEndImpl implements DumpDataAtEnd, ShutdownListener {

	private final Scenario scenarioData;

	private final OutputDirectoryHierarchy controlerIO;

	@Inject
	public DumpDataAtEndImpl(Scenario scenarioData, OutputDirectoryHierarchy controlerIO) {
		this.scenarioData = scenarioData;
		this.controlerIO = controlerIO;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// dump plans
		new PopulationWriter(scenarioData.getPopulation(), scenarioData.getNetwork()).write(controlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
		final ObjectAttributes personAttributes = scenarioData.getPopulation().getPersonAttributes();
		if ( personAttributes!=null ) {
			ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(personAttributes) ;
			writer.setPrettyPrint(true);
			writer.writeFile( controlerIO.getOutputFilename( Controler.FILENAME_PERSON_ATTRIBUTES ) );
		}
		// dump network
		new NetworkWriter(scenarioData.getNetwork()).write(controlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
		// dump config
		new ConfigWriter(scenarioData.getConfig()).write(controlerIO.getOutputFilename(Controler.FILENAME_CONFIG));
		// dump facilities
		ActivityFacilities facilities = scenarioData.getActivityFacilities();
		if (facilities != null) {
			new FacilitiesWriter(facilities).write(controlerIO.getOutputFilename("output_facilities.xml.gz"));
		}
		if (((NetworkFactoryImpl) scenarioData.getNetwork().getFactory()).isTimeVariant()) {
			new NetworkChangeEventsWriter().write(controlerIO.getOutputFilename("output_change_events.xml.gz"),
					((NetworkImpl) scenarioData.getNetwork()).getNetworkChangeEvents());
		}
		if (this.scenarioData.getConfig().scenario().isUseTransit()) {
			new TransitScheduleWriter(this.scenarioData.getTransitSchedule()).writeFile(controlerIO.getOutputFilename("output_transitSchedule.xml.gz"));
		}
		if (this.scenarioData.getConfig().scenario().isUseTransit()) {
			new VehicleWriterV1(this.scenarioData.getTransitVehicles()).writeFile(controlerIO.getOutputFilename("output_transitVehicles.xml.gz"));
		}
		if (this.scenarioData.getConfig().vehicles().getVehiclesFile() != null ) {
			new VehicleWriterV1(this.scenarioData.getVehicles()).writeFile(controlerIO.getOutputFilename("output_vehicles.xml.gz"));
		}
		if (this.scenarioData.getConfig().scenario().isUseHouseholds()) {
			new HouseholdsWriterV10(scenarioData.getHouseholds()).writeFile(controlerIO.getOutputFilename(Controler.FILENAME_HOUSEHOLDS));
		}
		if (this.scenarioData.getConfig().scenario().isUseLanes()) {
			new LaneDefinitionsWriter20(
					(LaneDefinitions20) scenarioData.getScenarioElement(LaneDefinitions20.ELEMENT_NAME)).write(
							controlerIO.getOutputFilename(Controler.FILENAME_LANES));
		}
		if (!event.isUnexpected() && scenarioData.getConfig().vspExperimental().isWritingOutputEvents()) {
			try {
				File toFile = new File(	controlerIO.getOutputFilename("output_events.xml.gz"));
				File fromFile = new File(controlerIO.getIterationFilename(scenarioData.getConfig().controler().getLastIteration(), "events.xml.gz"));
				IOUtils.copyFile(fromFile, toFile);
			} catch ( Exception ee ) {
				Logger.getLogger(this.getClass()).error("writing output events did not work; probably parameters were such that no events were "
						+ "generated in the final iteration") ;
			}
		}
	}

}
