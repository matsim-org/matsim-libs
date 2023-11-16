/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioLoader
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
package org.matsim.core.scenario;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.HouseholdsConfigGroup;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.lanes.LanesReader;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.vehicles.MatsimVehicleReader;


import java.io.UncheckedIOException;
import java.net.URL;
import java.util.*;

import static org.matsim.core.config.groups.PlansConfigGroup.PERSON_ATTRIBUTES_DEPRECATION_MESSAGE;

/**
 * Loads elements of Scenario from file. Non standardized elements
 * can also be loaded however they require a specific instance of
 * Scenario.
 * {@link #loadScenario()} reads the complete scenario from files while the
 * other load...() methods only load specific parts
 * of the scenario assuming that required parts are already
 * loaded or created by the user.
 * <p></p>
 * Design thoughts:<ul>
 * <li> Given what we have now, does it make sense to leave this class public?  yy kai, mar'11
 * </ul>
 *
 * @see org.matsim.core.scenario.MutableScenario
 *
 * @author dgrether
 */
// deliberately non-public.  Use method in ScenarioUtils.
class ScenarioLoaderImpl {

	private static final Logger log = LogManager.getLogger(ScenarioLoaderImpl.class);

	private final Config config;

	private final MutableScenario scenario;

	private Map<Class<?>, AttributeConverter<?>> attributeConverters = Collections.emptyMap();

	@Inject
	public void setAttributeConverters(Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		log.debug( "setting "+attributeConverters );
		this.attributeConverters = attributeConverters;
	}

	ScenarioLoaderImpl(Config config) {
		this.config = config;
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(this.config);
	}

	ScenarioLoaderImpl(Scenario scenario) {
		this.scenario = (MutableScenario) scenario;
		this.config = this.scenario.getConfig();
	}

	/**
	 * Loads all mandatory Scenario elements and
	 * if activated in config's scenario module/group
	 * optional elements.
	 * @return the Scenario
	 */
	Scenario loadScenario() {
//		String currentDir = new File("tmp").getAbsolutePath();
//		currentDir = currentDir.substring(0, currentDir.length() - 3);
//		log.info("loading scenario from base directory: " + currentDir);
		// the above is not used and thus only causing confusion in the log output.  kai, sep'18

		this.loadNetwork();
		this.loadActivityFacilities();
		this.loadPopulation();
		this.loadHouseholds(); // tests internally if the file is there
		this.loadTransit(); // tests internally if the file is there
		this.loadTransitVehicles(); // tests internally if the file is there
		if (this.config.vehicles().getVehiclesFile()!=null ) {
			this.loadVehicles() ;
		}
		if (this.config.network().getLaneDefinitionsFile()!=null ) {
			this.loadLanes();
		}
		return this.scenario;
	}

	/**
	 * Loads the network into the scenario of this class
	 */
	private void loadNetwork() {
		if ((this.config.network() != null) && (this.config.network().getInputFile() != null)) {
			URL networkUrl = this.config.network().getInputFileURL(this.config.getContext());
			log.info("loading network from " + networkUrl);
			String inputCRS = config.network().getInputCRS();

			MatsimNetworkReader reader =
					new MatsimNetworkReader(
							inputCRS,
							config.global().getCoordinateSystem(),
							this.scenario.getNetwork());
            reader.putAttributeConverters( attributeConverters );
            reader.parse(networkUrl);

			if ((this.config.network().getChangeEventsInputFile()!= null) && this.config.network().isTimeVariantNetwork()) {
				log.info("loading network change events from " + this.config.network().getChangeEventsInputFileUrl(this.config.getContext()).getFile());
				Network network = this.scenario.getNetwork();
				List<NetworkChangeEvent> changeEvents = new ArrayList<>() ;
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network,changeEvents);
				parser.parse(this.config.network().getChangeEventsInputFileUrl(config.getContext()));
				NetworkUtils.setNetworkChangeEvents(network,changeEvents);
			}
		}
	}

	private void loadActivityFacilities() {
		if ((this.config.facilities() != null) && (this.config.facilities().getInputFile() != null)) {
			URL facilitiesFileName = this.config.facilities().getInputFileURL(config.getContext());
			log.info("loading facilities from " + facilitiesFileName);

			final String inputCRS = config.facilities().getInputCRS();
			final String internalCRS = config.global().getCoordinateSystem();

            MatsimFacilitiesReader reader = new MatsimFacilitiesReader(inputCRS, internalCRS, this.scenario.getActivityFacilities());
            reader.putAttributeConverters(attributeConverters);
            reader.parse(facilitiesFileName);

			log.info("loaded " + this.scenario.getActivityFacilities().getFacilities().size() + " facilities from " + facilitiesFileName);
		}
		else {
			log.info("no facilities file set in config, therefore not loading any facilities.  This is not a problem except if you are using facilities");
		}
		if ((this.config.facilities() != null) && (this.config.facilities().getInputFacilitiesAttributesFile() != null)) {
			if ( !this.config.facilities().isInsistingOnUsingDeprecatedFacilitiesAttributeFile() ) {
				throw new RuntimeException(FacilitiesConfigGroup.FACILITIES_ATTRIBUTES_DEPRECATION_MESSAGE) ;
			}
			URL facilitiesAttributesURL = ConfigGroup.getInputFileURL(this.config.getContext(), this.config.facilities().getInputFacilitiesAttributesFile());
			log.info("loading facility attributes from " + facilitiesAttributesURL);
			parseObjectAttributesToAttributable(
					facilitiesAttributesURL,
					scenario.getActivityFacilities().getFacilities().values(),
					"facilityAttributes not empty after going through all facilities, meaning that it contains material for facilityIDs that " +
							"are not in the container.  This is not necessarily a bug so we will continue, but note that such material " +
							"will no longer be contained in the output_* files.");
		}
		else {
			log.info("no facility-attributes file set in config, not loading any facility attributes");
		}
	}


	private void loadPopulation() {
		if ((this.config.plans() != null) && (this.config.plans().getInputFile() != null)) {
			URL populationFileName = this.config.plans().getInputFileURL(this.config.getContext());
			log.info("loading population from " + populationFileName);

            final String targetCRS = config.global().getCoordinateSystem();
			final String internalCRS = config.global().getCoordinateSystem();

            final PopulationReader reader = new PopulationReader(targetCRS, internalCRS, this.scenario);
            reader.putAttributeConverters( attributeConverters );
            reader.parse( populationFileName );

			PopulationUtils.printPlansCount(this.scenario.getPopulation()) ;
		}
		else {
			log.info("no population file set in config, not able to load population");
		}

		if ((this.config.plans() != null) && (this.config.plans().getInputPersonAttributeFile() != null)) {
			URL personAttributesURL = this.config.plans().getInputPersonAttributeFileURL(this.config.getContext());
			log.info("loading person attributes from " + personAttributesURL);
			parseObjectAttributesToAttributable(
					personAttributesURL,
					scenario.getPopulation().getPersons().values(),
					"personAttributes not empty after going through all persons, meaning that it contains material for personIDs that " +
							"are not in the population.  This is not necessarily a bug so we will continue, but note that such material " +
							"will no longer be contained in the output_* files.  (We have this happening in particular when the same personAttributes " +
							"file is used for the 10pct and the 1pct scenario. The material that is still there will follow.  kai, jun'19"
			);

			final String outputDirectory = this.config.controller().getOutputDirectory();
//			final File outDir = new File( outputDirectory );
//			if ( outDir.exists() && outDir.canWrite() ){
//				// since ScenarioLoader is supposed to only read material,  there are cases where the output directory does not exist at
//				// this stage. One could maybe write to the "config.getContext()" directory.  However, sometimes this is a URL, and thus also
//				// non-writeable, and it is even less systematic than writing into the output directory. kai, jun'19
//
//				String outFilename = outputDirectory + "/input_plans_with_person_attributes.xml.gz";
//				PopulationUtils.writePopulation( scenario.getPopulation(), outFilename );
//
//				log.warn(
//					  "a file with path=" + outFilename + " was just written in order to facilitate the transition to having person attributes inside " +
//						    "the persons. " );
//			}
			// TD says to rather not have this kind of side effect.  kai, jul'19

			if ( !this.config.plans().isInsistingOnUsingDeprecatedPersonAttributeFile() ) {
				throw new RuntimeException(PERSON_ATTRIBUTES_DEPRECATION_MESSAGE) ;
			}
		}
		else {
			log.info("no person-attributes file set in config, not loading any person attributes");
		}
	}

	private void loadHouseholds() {
		if ( (this.config.households() != null) && (this.config.households().getInputFile() != null) ) {
			URL householdsFile = this.config.households().getInputFileURL(this.config.getContext());
			log.info("loading households from " + householdsFile);
			HouseholdsReaderV10 reader = new HouseholdsReaderV10(this.scenario.getHouseholds());
			reader.putAttributeConverters(this.attributeConverters);
			reader.parse(householdsFile);
			log.info("households loaded.");
		}
		else {
			log.info("no households file set in config, not loading households");
		}
		if ((this.config.households() != null)) {
			final String fn = this.config.households().getInputHouseholdAttributesFile();
			if(( fn != null)) {
				if (!this.config.households().isInsistingOnUsingDeprecatedHouseholdsAttributeFile()) {
					throw new RuntimeException(HouseholdsConfigGroup.HOUSEHOLD_ATTRIBUTES_DEPRECATION_MESSAGE);
				}

				URL householdAttributesFileName = ConfigGroup.getInputFileURL(this.config.getContext(), fn ) ;
				log.info("loading household attributes from " + householdAttributesFileName);
				parseObjectAttributesToAttributable(
						householdAttributesFileName,
						this.scenario.getHouseholds().getHouseholds().values(),
						"householdAttributes not empty after going through all households, meaning that it contains material for householdIDs that " +
								"are not in the container.  This is not necessarily a bug so we will continue, but note that such material " +
								"will no longer be contained in the output_* files.");
			}
		}
		else {
			log.info("no household-attributes file set in config, not loading any household attributes");
		}
	}

	private void loadTransit() throws UncheckedIOException {

		if ( this.config.transit().getTransitScheduleFile() != null ) {
			URL transitScheduleFile = this.config.transit().getTransitScheduleFileURL(this.config.getContext());
			final String inputCRS = config.transit().getInputScheduleCRS();
			final String internalCRS = config.global().getCoordinateSystem();

            new TransitScheduleReader( inputCRS, internalCRS, this.scenario).readURL(transitScheduleFile );
		}
		else {
			log.info("no transit schedule file set in config, not loading any transit schedule");
		}

		if ( this.config.transit().getTransitLinesAttributesFile() != null ) {
			if (!this.config.transit().isInsistingOnUsingDeprecatedAttributeFiles()) {
				throw new RuntimeException(TransitConfigGroup.TRANSIT_ATTRIBUTES_DEPRECATION_MESSAGE);
			}

			URL transitLinesAttributesFileName = IOUtils.extendUrl(this.config.getContext(), this.config.transit().getTransitLinesAttributesFile());
			log.info("loading transit lines attributes from " + transitLinesAttributesFileName);
			parseObjectAttributesToAttributable(
					transitLinesAttributesFileName,
					this.scenario.getTransitSchedule().getTransitLines().values(),
					"transit lines attributes not empty after going through all lines, meaning that it contains material for line IDs that " +
							"are not in the container.  This is not necessarily a bug so we will continue, but note that such material " +
							"will no longer be contained in the output_* files.");
		}

		if ( this.config.transit().getTransitStopsAttributesFile() != null ) {
			if (!this.config.transit().isInsistingOnUsingDeprecatedAttributeFiles()) {
				throw new RuntimeException(TransitConfigGroup.TRANSIT_ATTRIBUTES_DEPRECATION_MESSAGE);
			}

			URL transitStopsAttributesURL = IOUtils.extendUrl(this.config.getContext(), this.config.transit().getTransitStopsAttributesFile());
			log.info("loading transit stop facilities attributes from " + transitStopsAttributesURL);
			parseObjectAttributesToAttributable(
					transitStopsAttributesURL,
					this.scenario.getTransitSchedule().getFacilities().values(),
					"transit stops attributes not empty after going through all stops, meaning that it contains material for stop IDs that " +
							"are not in the container.  This is not necessarily a bug so we will continue, but note that such material " +
							"will no longer be contained in the output_* files.");
		}
	}

	private void loadTransitVehicles() throws UncheckedIOException {
		final String vehiclesFile = this.config.transit().getVehiclesFile();
		if ( vehiclesFile != null ) {
			log.info("loading transit vehicles from " + vehiclesFile);
			new MatsimVehicleReader(this.scenario.getTransitVehicles()).readURL(this.config.transit().getVehiclesFileURL(this.config.getContext() ) );
		}
		else {
			log.info("no transit vehicles file set in config, not loading any transit vehicles");
		}
	}
	private void loadVehicles() throws UncheckedIOException {
		final String vehiclesFile = this.config.vehicles().getVehiclesFile();
		if ( vehiclesFile != null ) {
			log.info("loading vehicles from " + vehiclesFile );
			new MatsimVehicleReader(this.scenario.getVehicles()).readURL(IOUtils.extendUrl(this.config.getContext(), vehiclesFile ) );
		}
		else {
			log.info("no vehicles file set in config, not loading any vehicles");
		}
	}

	private void loadLanes() {
		String filename = this.config.network().getLaneDefinitionsFile();
		if (filename != null){
			LanesReader reader = new LanesReader(this.scenario);
			reader.readURL( ConfigGroup.getInputFileURL(this.config.getContext(), filename ) );
		}
		else {
			log.info("no lanes file set in config, not loading any lanes");
		}
	}

	private <T extends Identifiable<?> & Attributable> void parseObjectAttributesToAttributable(
			URL url,
			Iterable<T> attributables,
			String message) {
		final ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(attributes);
		reader.putAttributeConverters( attributeConverters );
		reader.parse(url);

		for( T facility : attributables ) {
			Collection<String> keys = ObjectAttributesUtils.getAllAttributeNames( attributes, facility.getId().toString() );
			for( String key : keys ){
				Object value = attributes.getAttribute( facility.getId().toString(), key );
				facility.getAttributes().putAttribute( key, value ) ;
			}
			attributes.removeAllAttributes( facility.getId().toString() );
		}
		// (some of the above could also become a static helper method in ObjectAttributesUtils, but this here seems the only
		// place within matsim core where the personAttributes are automatically read so maybe there is no need for this. kai, jun'19)

		if ( !attributes.toString().equals( "" ) ) {
			log.warn( message ) ;
			log.warn( "showing the first 1000 characters from the remaining personAttributes ...") ;
			log.warn( attributes.toString().substring( 0, Math.min(attributes.toString().length(), 1000 ) ) );
			log.warn("");
		}
	}

}
