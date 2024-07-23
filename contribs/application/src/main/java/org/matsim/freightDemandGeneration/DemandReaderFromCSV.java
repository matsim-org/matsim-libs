/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.freightDemandGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.freight.carriers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This DemandReaderFromCSV reads all demand information given in the read CSV
 * file and creates the demand for the carriers. While the process of creating
 * the demand, the consistency of the information will be checked.
 *
 * @author Ricardo Ewert
 */
public final class DemandReaderFromCSV {
	private static final Logger log = LogManager.getLogger(DemandReaderFromCSV.class);
	private static final Random rand = new Random(4711);
	private static double roundingError;

	/**
	 * DemandInformationElement is a set of information being read from the input
	 * file. Several DemandInformationElement can be read in for one carrier. This
	 * is necessary for creating configurations of the demand. Not every parameter
	 * should be set for creating the demand. While the process of creating the
	 * demand, the consistency of the information will be checked. If this demand
	 * creates a service, the information for the firstJobElement should be set. If
	 * this demand creates a shipment, the firstJobElement is the pickup and the
	 * secondJobElement is the delivery.
	 */
	static class DemandInformationElement {

		/**
		 * Name of the carrier with this demand configuration.
		 */
		private final String carrierName;
		/**
		 * Sets the amount of demand which should be handled.
		 */
		private final Integer demandToDistribute;
		/**
		 * Sets the number of jobs which should be handled.
		 */
		private final Integer numberOfJobs;
		/**
		 * Sets the share of the population which has this service/pickup.
		 */
		private final Double shareOfPopulationWithFirstJobElement;
		/**
		 * Sets the areas where the services/pickups should be created. Therefore, a
		 * shape input is necessary.
		 */
		private final String[] areasFirstJobElement;
		/**
		 * Sets the number of service/pickup locations.
		 */
		private final Integer numberOfFirstJobElementLocations;
		/**
		 * Sets the locations of the service/pickup job. This should be the linkIds as a
		 * String[].
		 */
		private final String[] locationsOfFirstJobElement;
		/**
		 * Time unit for one demand unit for the services/pickups.
		 */
		private final Integer firstJobElementTimePerUnit;
		/**
		 * TimeWindow for the services/pickups.
		 */
		private final TimeWindow firstJobElementTimeWindow;
		/**
		 * Sets the share of the population which has this delivery.
		 */
		private final Double shareOfPopulationWithSecondJobElement;
		/**
		 * Sets the areas where the deliveries should be created. Therefore, a shape
		 * input is necessary.
		 */
		private final String[] areasSecondJobElement;
		/**
		 * Sets the number of delivery locations.
		 */
		private final Integer numberOfSecondJobElementLocations;
		/**
		 * Sets the locations of the deliveries. This should be the linkIds as a
		 * String[].
		 */
		private final String[] locationsOfSecondJobElement;
		/**
		 * Time unit for one demand unit for the deliveries.
		 */
		private final Integer secondJobElementTimePerUnit;
		/**
		 * TimeWindow for the deliveries.
		 */
		private final TimeWindow secondJobElementTimeWindow;
		/**
		 * Type of the demand information. Options: service or shipment
		 */
		private final String typeOfDemand;

		/**
		 * @author Ricardo
		 *
		 */
		public static class Builder {
			private final String carrierName;
			private final Integer demandToDistribute;
			private Integer numberOfJobs = null;
			private Double shareOfPopulationWithFirstJobElement = null;
			private String[] areasFirstJobElement = null;
			private Integer numberOfFirstJobElementLocations = null;
			private String[] locationsOfFirstJobElement = null;
			private Integer firstJobElementTimePerUnit = null;
			private TimeWindow firstJobElementTimeWindow = null;
			private Double shareOfPopulationWithSecondJobElement = null;
			private String[] areasSecondJobElement = null;
			private Integer numberOfSecondJobElementLocations = null;
			private String[] locationsOfSecondJobElement = null;
			private Integer secondJobElementTimePerUnit = null;
			private TimeWindow secondJobElementTimeWindow = null;
			private String typeOfDemand = "service";

			public static Builder newInstance(String carrierName, Integer demandToDistribute) {
				return new Builder(carrierName, demandToDistribute);
			}

			private Builder(String carrierName, Integer demandToDistribute) {
				super();
				this.carrierName = carrierName;
				this.demandToDistribute = demandToDistribute;
			}

			public void setNumberOfJobs(Integer numberOfJobs) {
				this.numberOfJobs = numberOfJobs;
			}

			public void setShareOfPopulationWithFirstJobElement(Double shareOfPopulationWithFirstJobElement) {
				this.shareOfPopulationWithFirstJobElement = shareOfPopulationWithFirstJobElement;
			}

			public void setAreasFirstJobElement(String[] areasFirstJobElement) {
				this.areasFirstJobElement = areasFirstJobElement;
			}

			public void setNumberOfFirstJobElementLocations(Integer numberOfFirstJobElementLocations) {
				this.numberOfFirstJobElementLocations = numberOfFirstJobElementLocations;
			}

			public void setLocationsOfFirstJobElement(String[] locationsOfFirstJobElement) {
				this.locationsOfFirstJobElement = locationsOfFirstJobElement;
			}

			public void setFirstJobElementTimePerUnit(Integer firstJobElementTimePerUnit) {
				this.firstJobElementTimePerUnit = firstJobElementTimePerUnit;
			}

			public void setFirstJobElementTimeWindow(TimeWindow firstJobElementTimeWindow) {
				this.firstJobElementTimeWindow = firstJobElementTimeWindow;
			}

			public void setShareOfPopulationWithSecondJobElement(Double shareOfPopulationWithSecondJobElement) {
				this.shareOfPopulationWithSecondJobElement = shareOfPopulationWithSecondJobElement;
			}

			public void setAreasSecondJobElement(String[] areasSecondJobElement) {
				this.areasSecondJobElement = areasSecondJobElement;
			}

			public void setNumberOfSecondJobElementLocations(Integer numberOfSecondJobElementLocations) {
				this.numberOfSecondJobElementLocations = numberOfSecondJobElementLocations;
			}

			public void setLocationsOfSecondJobElement(String[] locationsOfSecondJobElement) {
				this.locationsOfSecondJobElement = locationsOfSecondJobElement;
			}

			public void setSecondJobElementTimePerUnit(Integer secondJobElementTimePerUnit) {
				this.secondJobElementTimePerUnit = secondJobElementTimePerUnit;
			}

			public void setSecondJobElementTimeWindow(TimeWindow secondJobElementTimeWindow) {
				this.secondJobElementTimeWindow = secondJobElementTimeWindow;
			}

			public void setTypeOfDemand(String typeOfDemand) {
				this.typeOfDemand = typeOfDemand;
			}

			public DemandInformationElement build() {
				return new DemandInformationElement(this);
			}
		}

		private DemandInformationElement(Builder builder) {
			carrierName = builder.carrierName;
			demandToDistribute = builder.demandToDistribute;
			numberOfJobs = builder.numberOfJobs;
			shareOfPopulationWithFirstJobElement = builder.shareOfPopulationWithFirstJobElement;
			areasFirstJobElement = builder.areasFirstJobElement;
			numberOfFirstJobElementLocations = builder.numberOfFirstJobElementLocations;
			locationsOfFirstJobElement = builder.locationsOfFirstJobElement;
			firstJobElementTimePerUnit = builder.firstJobElementTimePerUnit;
			firstJobElementTimeWindow = builder.firstJobElementTimeWindow;
			shareOfPopulationWithSecondJobElement = builder.shareOfPopulationWithSecondJobElement;
			areasSecondJobElement = builder.areasSecondJobElement;
			numberOfSecondJobElementLocations = builder.numberOfSecondJobElementLocations;
			locationsOfSecondJobElement = builder.locationsOfSecondJobElement;
			secondJobElementTimePerUnit = builder.secondJobElementTimePerUnit;
			secondJobElementTimeWindow = builder.secondJobElementTimeWindow;
			typeOfDemand = builder.typeOfDemand;
		}

		public String getCarrierName() {
			return carrierName;
		}

		public Integer getDemandToDistribute() {
			return demandToDistribute;
		}

		public Integer getNumberOfJobs() {
			return numberOfJobs;
		}

		public Double getShareOfPopulationWithFirstJobElement() {
			return shareOfPopulationWithFirstJobElement;
		}

		public String[] getAreasFirstJobElement() {
			return areasFirstJobElement;
		}

		public Integer getNumberOfFirstJobElementLocations() {
			return numberOfFirstJobElementLocations;
		}

		public String[] getLocationsOfFirstJobElement() {
			return locationsOfFirstJobElement;
		}

		public Integer getFirstJobElementTimePerUnit() {
			return firstJobElementTimePerUnit;
		}

		public TimeWindow getFirstJobElementTimeWindow() {
			return firstJobElementTimeWindow;
		}

		public Double getShareOfPopulationWithSecondJobElement() {
			return shareOfPopulationWithSecondJobElement;
		}

		public String[] getAreasSecondJobElement() {
			return areasSecondJobElement;
		}

		public Integer getNumberOfSecondJobElementLocations() {
			return numberOfSecondJobElementLocations;
		}

		public String[] getLocationsOfSecondJobElement() {
			return locationsOfSecondJobElement;
		}

		public Integer getSecondJobElementTimePerUnit() {
			return secondJobElementTimePerUnit;
		}

		public TimeWindow getSecondJobElementTimeWindow() {
			return secondJobElementTimeWindow;
		}

		public String getTypeOfDemand() {
			return typeOfDemand;
		}
	}

	/**
	 * Reads the csv with the demand information and adds this demand to the related
	 * carriers.
	 *
	 * @param scenario                         Scenario
	 * @param csvLocationDemand                Path to the csv file with the demand information
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param combineSimilarJobs               boolean if the jobs of the same carrier with same location and time will be combined
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param population                       Population
	 * @param shapeCategory                    Column name in the shape file for the data connection in the csv files
	 * @throws IOException						if the csv file cannot be read
	 */
	static void readAndCreateDemand(Scenario scenario, Path csvLocationDemand,
									ShpOptions.Index indexShape, boolean combineSimilarJobs,
									CoordinateTransformation crsTransformationNetworkAndShape, Population population, String shapeCategory) throws IOException {

		Set<DemandInformationElement> demandInformation = readDemandInformation(csvLocationDemand);
		checkNewDemand(scenario, demandInformation, indexShape, shapeCategory);
		createDemandForCarriers(scenario, indexShape, demandInformation, population, combineSimilarJobs,
				crsTransformationNetworkAndShape);
	}

	/**
	 * Reads the demand information from the csv file and checks if the information
	 * is consistent
	 *
	 * @param csvLocationDemand Path to the csv file with the demand information
	 * @return Set<DemandInformationElement>
	 * @throws IOException if the csv file cannot be read
	 */
	static Set<DemandInformationElement> readDemandInformation(Path csvLocationDemand) throws IOException {

		Set<DemandInformationElement> demandInformation = new HashSet<>();
		CSVParser parse = new CSVParser(Files.newBufferedReader(csvLocationDemand),
				CSVFormat.Builder.create(CSVFormat.TDF).setHeader().setSkipHeaderRecord(true).build());

		for (CSVRecord record : parse) {
			DemandInformationElement.Builder builder;
			if (!record.get("carrierName").isBlank() && !record.get("demandToDistribute").isBlank())
				builder = DemandInformationElement.Builder.newInstance(record.get("carrierName"),
						Integer.parseInt(record.get("demandToDistribute")));
			else
				throw new RuntimeException(
						"Minimum one DemandInformationElement has no name or demand. Both is necessary. Please check the input csv file!");
			if (!record.get("numberOfJobs").isBlank())
				builder.setNumberOfJobs(Integer.parseInt(record.get("numberOfJobs")));
			if (!record.get("shareOfPopulationWithFirstJobElement").isBlank())
				builder.setShareOfPopulationWithFirstJobElement(
						Double.parseDouble(record.get("shareOfPopulationWithFirstJobElement")));
			if (!record.get("areasFirstJobElement").isBlank())
				builder.setAreasFirstJobElement(record.get("areasFirstJobElement").split(";"));
			if (!record.get("numberOfFirstJobElementLocations").isBlank())
				builder.setNumberOfFirstJobElementLocations(
						Integer.parseInt(record.get("numberOfFirstJobElementLocations")));
			if (!record.get("locationsOfFirstJobElement").isBlank())
				builder.setLocationsOfFirstJobElement(record.get("locationsOfFirstJobElement").split(";"));
			if (!record.get("firstJobElementTimePerUnit").isBlank())
				builder.setFirstJobElementTimePerUnit(Integer.parseInt(record.get("firstJobElementTimePerUnit")));
			if (!record.get("firstJobElementStartTime").isBlank() || !record.get("firstJobElementEndTime").isBlank())
				builder.setFirstJobElementTimeWindow(
						TimeWindow.newInstance(Integer.parseInt(record.get("firstJobElementStartTime")),
								Integer.parseInt(record.get("firstJobElementEndTime"))));
			if (!record.get("shareOfPopulationWithSecondJobElement").isBlank())
				builder.setShareOfPopulationWithSecondJobElement(
						Double.parseDouble(record.get("shareOfPopulationWithSecondJobElement")));
			if (!record.get("areasSecondJobElement").isBlank())
				builder.setAreasSecondJobElement(record.get("areasSecondJobElement").split(";"));
			if (!record.get("numberOfSecondJobElementLocations").isBlank())
				builder.setNumberOfSecondJobElementLocations(
						Integer.parseInt(record.get("numberOfSecondJobElementLocations")));
			if (!record.get("locationsOfSecondJobElement").isBlank())
				builder.setLocationsOfSecondJobElement(record.get("locationsOfSecondJobElement").split(";"));
			if (!record.get("secondJobElementTimePerUnit").isBlank()) {
				builder.setSecondJobElementTimePerUnit(Integer.parseInt(record.get("secondJobElementTimePerUnit")));
				builder.setTypeOfDemand("shipment");
			}
			if (!record.get("secondJobElementStartTime").isBlank() || !record.get("secondJobElementEndTime").isBlank())
				builder.setSecondJobElementTimeWindow(
						TimeWindow.newInstance(Integer.parseInt(record.get("secondJobElementStartTime")),
								Integer.parseInt(record.get("secondJobElementEndTime"))));
			DemandInformationElement newDemandInformationElement = builder.build();
			demandInformation.add(newDemandInformationElement);
		}
		return demandInformation;
	}

	/**
	 * Checks if the read demand information are useful to create the shipment or
	 * service demands
	 *
	 * @param scenario          Scenario
	 * @param demandInformation Set<DemandInformationElement>
	 * @param indexShape        ShpOptions.Index for the shape file
	 * @param shapeCategory     Column name in the shape file for the data connection in the csv files
	 */
	static void checkNewDemand(Scenario scenario, Set<DemandInformationElement> demandInformation,
							   ShpOptions.Index indexShape, String shapeCategory) {

		for (DemandInformationElement newDemand : demandInformation) {
			Carriers carriers = (Carriers) scenario.getScenarioElement("carriers");
			if (!carriers.getCarriers().containsKey(Id.create(newDemand.getCarrierName(), Carrier.class)))
				throw new RuntimeException(
						"The created demand is not created for an existing carrier. Please create the carrier "
								+ newDemand.getCarrierName() + " first or relate the demand to another carrier");
			if (newDemand.getNumberOfJobs() != null && newDemand.getDemandToDistribute() != 0
					&& newDemand.getDemandToDistribute() < newDemand.getNumberOfJobs())
				throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
						+ ": The number of jobs is higher than the demand. This is not possible because the minimum demand of one job is 1");
			if (newDemand.getNumberOfJobs() != null && newDemand.getNumberOfJobs() == 0)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
						+ ": The number of jobs can not be 0 !. Please check!");
			if (newDemand.getNumberOfJobs() == null
					&& (newDemand.getDemandToDistribute() == null || newDemand.getDemandToDistribute() == 0)
					&& newDemand.getShareOfPopulationWithFirstJobElement() == null
					&& newDemand.getShareOfPopulationWithSecondJobElement() == null)
				throw new RuntimeException(
						"You have to select a number of jobs, a population share or a demand. Please Check!!");
			if (newDemand.getShareOfPopulationWithFirstJobElement() != null)
				if (newDemand.getShareOfPopulationWithFirstJobElement() > 1
						|| newDemand.getShareOfPopulationWithFirstJobElement() <= 0)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": The percentage of the population should be more than 0 and maximum 100pct. Please check!");
			if (newDemand.getShareOfPopulationWithFirstJobElement() != null
					&& newDemand.getNumberOfFirstJobElementLocations() != null)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
						+ ": Select either share of population or number of locations");
			if (newDemand.getAreasFirstJobElement() != null) {
				if (indexShape == null)
					throw new RuntimeException("You selected a certain area for the carrier"
							+ newDemand.getCarrierName() + " although no shape file is loaded.");
				for (String demandArea : newDemand.getAreasFirstJobElement()) {
					boolean isInShape = false;

					for (SimpleFeature singlePolygon : indexShape.getAllFeatures())
						if ((singlePolygon.getAttribute(shapeCategory) != null
								&& singlePolygon.getAttribute(shapeCategory).equals(demandArea))) {
							isInShape = true;
							break;
						}
					if (!isInShape)
						throw new RuntimeException("The area " + demandArea + " for the demand generation of carrier "
								+ newDemand.getCarrierName() + " is not part of the given shapeFile. The areas should be in the shape file column " + shapeCategory);
				}
			}
			if (newDemand.getLocationsOfFirstJobElement() != null)
				for (String linkForDemand : newDemand.getLocationsOfFirstJobElement()) {
					if (!scenario.getNetwork().getLinks().containsKey(Id.createLinkId(linkForDemand)))
						throw new RuntimeException("The selected link " + linkForDemand + " for the demand of carrier "
								+ newDemand.getCarrierName() + " not part of the network. Please check!");
				}
			if (newDemand.getFirstJobElementTimePerUnit() == null)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
						+ ": No time per unit for one job element was selected");
			if (newDemand.getFirstJobElementTimeWindow() == null)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
						+ ": No timeWindow for one job element was selected");

			// for services
			if (newDemand.getTypeOfDemand().equals("service")) {
				if (newDemand.getNumberOfJobs() != null && newDemand.getShareOfPopulationWithFirstJobElement() != null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": Select either a numberOfJobs or a share of the population. Please check!");
				if (newDemand.getLocationsOfFirstJobElement() != null && newDemand.getNumberOfJobs() != null
						&& newDemand.getLocationsOfFirstJobElement().length > newDemand.getNumberOfJobs())
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": For creating services it is not possible to have a higher number of <locationsOfFirstJobElement> than <numberOfJobs>");
				if (newDemand.getLocationsOfFirstJobElement() != null
						&& newDemand.getNumberOfFirstJobElementLocations() != null
						&& newDemand.getLocationsOfFirstJobElement().length > newDemand
								.getNumberOfFirstJobElementLocations())
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": You selected more certain locations than the set number of locations");
			}
			// for shipments
			if (newDemand.getTypeOfDemand().equals("shipment")) {
				if (newDemand.getShareOfPopulationWithSecondJobElement() != null
						&& newDemand.getNumberOfSecondJobElementLocations() != null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": Select either share of population or number of locations");
				if (newDemand.getNumberOfJobs() != null && newDemand.getShareOfPopulationWithFirstJobElement() != null
						&& newDemand.getShareOfPopulationWithSecondJobElement() != null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": Select either a numberOfJobs or a share of the population. Please check!");
				if (newDemand.getShareOfPopulationWithSecondJobElement() != null)
					if (newDemand.getShareOfPopulationWithSecondJobElement() > 1
							|| newDemand.getShareOfPopulationWithSecondJobElement() <= 0)
						throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
								+ ": The percentage of the population should be more than 0 and maximum 100pct. Please check!");
				if (newDemand.getAreasSecondJobElement() != null) {
					if (indexShape == null)
						throw new RuntimeException("You selected a certain area for the carrier"
								+ newDemand.getCarrierName() + " although no shape file is loaded.");
					for (String demand : newDemand.getAreasSecondJobElement()) {
						boolean isInShape = false;
						for (SimpleFeature singlePolygon : indexShape.getAllFeatures())
							if (singlePolygon.getAttribute(shapeCategory).equals(demand)) {
								isInShape = true;
								break;
							}
						if (!isInShape)
							throw new RuntimeException("The area " + demand + " for the demand generation of carrier"
									+ newDemand.getCarrierName() + " is not part of the given shapeFile");
					}
				}
				if (newDemand.getLocationsOfSecondJobElement() != null)
					for (String linkForDemand : newDemand.getLocationsOfSecondJobElement()) {
						if (!scenario.getNetwork().getLinks().containsKey(Id.createLinkId(linkForDemand)))
							throw new RuntimeException(
									"The selected link " + linkForDemand + " for the demand of carrier "
											+ newDemand.getCarrierName() + " not part of the network. Please check!");
					}
				if (newDemand.getSecondJobElementTimePerUnit() == null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": No time per unit for one job element was selected");
				if (newDemand.getSecondJobElementTimeWindow() == null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierName()
							+ ": No timeWindow for one job element was selected");
			}
		}
	}

	/**
	 * Creates for every demand information the services/shipments for the carriers
	 *
	 * @param scenario                     		Scenario
	 * @param indexShape                   		ShpOptions.Index for the shape file
	 * @param demandInformation            		Set<DemandInformationElement> with the demand information
	 * @param population                   		Population
	 * @param combineSimilarJobs           		boolean if the jobs of the same carrier with same location and time will be combined
	 * @param crsTransformationNetworkAndShape 	CoordinateTransformation for the network and shape file
	 */
	static void createDemandForCarriers(Scenario scenario, ShpOptions.Index indexShape,
			Set<DemandInformationElement> demandInformation, Population population, boolean combineSimilarJobs,
			CoordinateTransformation crsTransformationNetworkAndShape) {

		for (DemandInformationElement newDemandInformationElement : demandInformation) {
			if (newDemandInformationElement.getTypeOfDemand().equals("service"))
				createServices(scenario, newDemandInformationElement, indexShape, population, combineSimilarJobs,
						crsTransformationNetworkAndShape);
			else if (newDemandInformationElement.getTypeOfDemand().equals("shipment"))
				createShipments(scenario, newDemandInformationElement, indexShape, population, combineSimilarJobs,
						crsTransformationNetworkAndShape);
		}

	}

	/**
	 * Creates the services.
	 *
	 * @param scenario                  		Scenario
	 * @param newDemandInformationElement 		single DemandInformationElement
	 * @param indexShape              			ShpOptions.Index
	 * @param population              			Population
	 * @param combineSimilarJobs      			boolean if the jobs of the same carrier with same location and time will be combined
	 * @param crsTransformationNetworkAndShape 	CoordinateTransformation for the network and shape file
	 */
	private static void createServices(Scenario scenario, DemandInformationElement newDemandInformationElement,
									   ShpOptions.Index indexShape, Population population, boolean combineSimilarJobs,
									   CoordinateTransformation crsTransformationNetworkAndShape) {

		int countOfLinks = 1;
		int distributedDemand = 0;
		roundingError = 0;
		Double shareOfPopulationWithThisService = newDemandInformationElement.getShareOfPopulationWithFirstJobElement();
		Integer numberOfJobs;
		Integer demandToDistribute = newDemandInformationElement.getDemandToDistribute();
		String[] areasForServiceLocations = newDemandInformationElement.getAreasFirstJobElement();
		String[] locationsOfServices = newDemandInformationElement.getLocationsOfFirstJobElement();
		Integer numberOfServiceLocations = newDemandInformationElement.getNumberOfFirstJobElementLocations();
		ArrayList<String> usedServiceLocations = new ArrayList<String>();
		int numberOfLinksInNetwork = scenario.getNetwork().getLinks().size();
		HashMap<Id<Person>, Person> possiblePersonsForService = new HashMap<Id<Person>, Person>();
		HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPerson = new HashMap<>();

		// set number of jobs
		if (shareOfPopulationWithThisService == null)
			numberOfJobs = newDemandInformationElement.getNumberOfJobs();
		else if (population == null)
			throw new RuntimeException(
					"No population found although input parameter <ShareOfPopulationWithThisDemand> is set");
		else {
			double sampleSizeInputPopulation = (double) population.getAttributes().getAttribute("sampleSize");
			double sampleTo = (double) population.getAttributes().getAttribute("samplingTo");
			String samplingOption = String.valueOf(population.getAttributes().getAttribute("samplingOption"));

			if (areasForServiceLocations != null)
				possiblePersonsForService = findPossiblePersons(population, areasForServiceLocations, indexShape,
						crsTransformationNetworkAndShape);
			else
				possiblePersonsForService.putAll(population.getPersons());
			int numberPossibleServices = (int) Math
					.round(shareOfPopulationWithThisService * possiblePersonsForService.size());
			if (sampleSizeInputPopulation == sampleTo)
				numberOfJobs = (int) Math.round(shareOfPopulationWithThisService * possiblePersonsForService.size());
			else if (samplingOption.equals("changeNumberOfLocationsWithDemand"))
				numberOfJobs = (int) Math.round((sampleTo / sampleSizeInputPopulation)
						* (shareOfPopulationWithThisService * possiblePersonsForService.size()));
			else if (samplingOption.equals("changeDemandOnLocation")) {
				demandToDistribute = (int) Math.round((sampleTo / sampleSizeInputPopulation) * demandToDistribute);
				numberOfJobs = (int) Math.round(shareOfPopulationWithThisService * possiblePersonsForService.size());
			} else
				throw new RuntimeException(
						"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			if (numberPossibleServices != 0)
				numberOfServiceLocations = numberPossibleServices;
		}
		// find possible links for the services
		HashMap<Id<Link>, Link> possibleLinksForService = findAllPossibleLinks(scenario, indexShape,
				crsTransformationNetworkAndShape, numberOfServiceLocations, areasForServiceLocations,
				locationsOfServices, possiblePersonsForService, nearestLinkPerPerson);

		if (locationsOfServices != null)
			for (String selectedLinkIdService : locationsOfServices)
				if (!possibleLinksForService.containsKey(Id.createLinkId(selectedLinkIdService)))
					throw new RuntimeException("The selected link " + selectedLinkIdService
							+ " for the service is not part of the possible links. Please check!");

		if (numberOfJobs == null) {
			// creates services with a demand of 1
			if (possibleLinksForService.size() > demandToDistribute) {
				for (int i = 0; i < demandToDistribute; i++) {

					Link link = findNextUsedLink(scenario, indexShape, possibleLinksForService, numberOfJobs,
							areasForServiceLocations, locationsOfServices, usedServiceLocations,
							possiblePersonsForService, nearestLinkPerPerson, crsTransformationNetworkAndShape, i);
					double serviceTime = newDemandInformationElement.getFirstJobElementTimePerUnit();
					int demandForThisLink = 1;
					usedServiceLocations.add(link.getId().toString());
					Id<CarrierService> idNewService = Id.create(
							createJobId(scenario, newDemandInformationElement, link.getId(), null),
							CarrierService.class);
					CarrierService thisService = CarrierService.Builder.newInstance(idNewService, link.getId())
							.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
							.setServiceStartTimeWindow(newDemandInformationElement.getFirstJobElementTimeWindow())
							.build();
					CarriersUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getServices()
							.put(thisService.getId(), thisService);
				}
			} else
			// creates a demand on each link, demand depends on the length of the link
			{
				if (possibleLinksForService.isEmpty())
					throw new RuntimeException(
							"Not enough links in the shape file to distribute the demand. Select an different shapefile or check the CRS of the shapefile and network");
				if (numberOfServiceLocations != null)
					throw new RuntimeException(
							"Because the demand is higher than the number of links, the demand will be distributed evenly over all links. You selected a certain number of service locations, which is not possible here!");
				double sumOfPossibleLinkLength = possibleLinksForService.values().stream().mapToDouble(Link::getLength).sum();
				for (Link link : possibleLinksForService.values()) {
					int demandForThisLink = calculateDemandBasedOnLinkLength(countOfLinks, distributedDemand, demandToDistribute, possibleLinksForService.size(),
						sumOfPossibleLinkLength, link);
					countOfLinks++;
					Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
					int numberOfJobsForDemand = calculateNumberOfJobsForDemand(thisCarrier, demandForThisLink);
					for (int i = 0; i < numberOfJobsForDemand; i++) {
						int singleDemandForThisLink = demandForThisLink / numberOfJobsForDemand;
						if (i == numberOfJobsForDemand - 1)
							singleDemandForThisLink = demandForThisLink - (numberOfJobsForDemand - 1) * singleDemandForThisLink;
						double serviceTime = newDemandInformationElement.getFirstJobElementTimePerUnit()
							* singleDemandForThisLink;
						Id<CarrierService> idNewService = Id.create(
							createJobId(scenario, newDemandInformationElement, link.getId(), null),
							CarrierService.class);
						if (demandToDistribute > 0 && singleDemandForThisLink > 0) {
							CarrierService thisService = CarrierService.Builder.newInstance(idNewService, link.getId())
								.setCapacityDemand(singleDemandForThisLink).setServiceDuration(serviceTime)
								.setServiceStartTimeWindow(newDemandInformationElement.getFirstJobElementTimeWindow())
								.build();
							thisCarrier.getServices().put(thisService.getId(), thisService);
						}
					}
					distributedDemand = distributedDemand + demandForThisLink;
				}
			}
		} else
		// if a certain number of services is selected
		{
			for (int i = 0; i < numberOfJobs; i++) {

				if (i * 2 > numberOfLinksInNetwork)
					throw new RuntimeException(
							"Not enough links in the shape file to distribute the demand. Select an different shapefile or check if shapefile and network has the same coordinateSystem.");
				Link link;
				if (numberOfServiceLocations == null || usedServiceLocations.size() < numberOfServiceLocations) {
					if (locationsOfServices != null && locationsOfServices.length > i) {
						link = scenario.getNetwork().getLinks().get(Id.createLinkId(locationsOfServices[i]));
					} else
						link = findNextUsedLink(scenario, indexShape, possibleLinksForService,
								numberOfServiceLocations, areasForServiceLocations, locationsOfServices,
								usedServiceLocations, possiblePersonsForService, nearestLinkPerPerson,
								crsTransformationNetworkAndShape, i);
				} else {
					link = scenario.getNetwork().getLinks().get(Id.createLinkId(usedServiceLocations.stream()
							.skip(rand.nextInt(usedServiceLocations.size() - 1)).findFirst().get()));
				}
				int demandForThisLink = calculateDemandForThisLink(demandToDistribute, numberOfJobs, distributedDemand, i);

				Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
				int numberOfJobsForDemand = calculateNumberOfJobsForDemand(thisCarrier, demandForThisLink);
				for (int j = 0; j < numberOfJobsForDemand; j++) {
					int singleDemandForThisLink = demandForThisLink / numberOfJobsForDemand;
					if (j == numberOfJobsForDemand - 1)
						singleDemandForThisLink = demandForThisLink - (numberOfJobsForDemand - 1) * singleDemandForThisLink;
					double serviceTime;
					if (singleDemandForThisLink == 0)
						serviceTime = newDemandInformationElement.getFirstJobElementTimePerUnit();
					else
						serviceTime = newDemandInformationElement.getFirstJobElementTimePerUnit() * demandForThisLink;
					usedServiceLocations.add(link.getId().toString());

					Id<CarrierService> idNewService = Id.create(
						createJobId(scenario, newDemandInformationElement, link.getId(), null), CarrierService.class);
					if ((demandToDistribute > 0 && singleDemandForThisLink > 0) || demandToDistribute == 0) {
						CarrierService thisService = CarrierService.Builder.newInstance(idNewService, link.getId())
							.setCapacityDemand(singleDemandForThisLink).setServiceDuration(serviceTime)
							.setServiceStartTimeWindow(newDemandInformationElement.getFirstJobElementTimeWindow())
							.build();
						CarriersUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getServices()
							.put(thisService.getId(), thisService);
					}
				}
				distributedDemand = distributedDemand + demandForThisLink;
			}
		}
		if (combineSimilarJobs)
			reduceNumberOfJobsIfSameCharacteristics(scenario, newDemandInformationElement);
	}

	/**
	 * Creates the shipments of a carrier.
	 *
	 * @param scenario 							Scenario
	 * @param newDemandInformationElement 		single DemandInformationElement
	 * @param indexShape 						ShpOptions.Index for the shape file
	 * @param population 						Population
	 * @param combineSimilarJobs 				boolean if the jobs of the same carrier with same location and time will be combined
	 * @param crsTransformationNetworkAndShape 	CoordinateTransformation for the network and shape file
	 */
	private static void createShipments(Scenario scenario, DemandInformationElement newDemandInformationElement,
										ShpOptions.Index indexShape, Population population, boolean combineSimilarJobs,
										CoordinateTransformation crsTransformationNetworkAndShape) {

		int countOfLinks = 1;
		int distributedDemand = 0;
		roundingError = 0;
		Double shareOfPopulationWithThisPickup = newDemandInformationElement.getShareOfPopulationWithFirstJobElement();
		Double shareOfPopulationWithThisDelivery = newDemandInformationElement
				.getShareOfPopulationWithSecondJobElement();
		Integer numberOfJobs;
		Integer demandToDistribute = newDemandInformationElement.getDemandToDistribute();
		Integer numberOfPickupLocations = newDemandInformationElement.getNumberOfFirstJobElementLocations();
		Integer numberOfDeliveryLocations = newDemandInformationElement.getNumberOfSecondJobElementLocations();
		String[] areasForPickupLocations = newDemandInformationElement.getAreasFirstJobElement();
		String[] areasForDeliveryLocations = newDemandInformationElement.getAreasSecondJobElement();
		String[] setLocationsOfPickup = newDemandInformationElement.getLocationsOfFirstJobElement();
		String[] setLocationsOfDelivery = newDemandInformationElement.getLocationsOfSecondJobElement();
		ArrayList<String> usedPickupLocations = new ArrayList<String>();
		ArrayList<String> usedDeliveryLocations = new ArrayList<String>();
		HashMap<Id<Person>, Person> possiblePersonsPickup = new HashMap<Id<Person>, Person>();
		HashMap<Id<Person>, Person> possiblePersonsDelivery = new HashMap<Id<Person>, Person>();
		HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPersonPickup = new HashMap<>();
		HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPersonDelivery = new HashMap<>();

		// set number of jobs
		if (shareOfPopulationWithThisPickup == null && shareOfPopulationWithThisDelivery == null)
			numberOfJobs = newDemandInformationElement.getNumberOfJobs();
		else if (population == null)
			throw new RuntimeException(
					"No population found although input parameter <ShareOfPopulationWithThisDemand> is set");
		else {
			double sampleSizeInputPopulation = (double) population.getAttributes().getAttribute("sampleSize");
			double sampleTo = (double) population.getAttributes().getAttribute("samplingTo");
			String samplingOption = String.valueOf(population.getAttributes().getAttribute("samplingOption"));

			if (areasForPickupLocations != null)
				possiblePersonsPickup = findPossiblePersons(population, areasForPickupLocations, indexShape,
						crsTransformationNetworkAndShape);
			else
				possiblePersonsPickup.putAll(population.getPersons());
			if (areasForDeliveryLocations != null)
				possiblePersonsDelivery = findPossiblePersons(population, areasForDeliveryLocations, indexShape,
						crsTransformationNetworkAndShape);
			else
				possiblePersonsDelivery.putAll(population.getPersons());

			int numberPossibleJobsPickup = 0;
			int numberPossibleJobsDelivery = 0;
			if (shareOfPopulationWithThisPickup != null)
				numberPossibleJobsPickup = (int) Math
						.round(shareOfPopulationWithThisPickup * possiblePersonsPickup.size());
			if (shareOfPopulationWithThisDelivery != null)
				numberPossibleJobsDelivery = (int) Math
						.round(shareOfPopulationWithThisDelivery * possiblePersonsDelivery.size());

			int sampledNumberPossibleJobsPickup = (int)Math.round((sampleTo / sampleSizeInputPopulation) * numberPossibleJobsPickup);
			int sampledNumberPossibleJobsDelivery = (int) Math.round((sampleTo / sampleSizeInputPopulation) * numberPossibleJobsDelivery);
			if (numberPossibleJobsPickup > numberPossibleJobsDelivery) {
				if (sampleSizeInputPopulation == sampleTo) {
					numberOfJobs = (int) Math.round(shareOfPopulationWithThisPickup * numberPossibleJobsPickup);
					numberPossibleJobsPickup = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsDelivery = (int) Math
								.round(shareOfPopulationWithThisDelivery * numberPossibleJobsDelivery);
				} else if (samplingOption.equals("changeNumberOfLocationsWithDemand")) {
					numberOfJobs = sampledNumberPossibleJobsPickup;
					numberPossibleJobsPickup = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsDelivery = sampledNumberPossibleJobsDelivery;
				} else if (samplingOption.equals("changeDemandOnLocation")) {
					demandToDistribute = (int) Math.round((sampleTo / sampleSizeInputPopulation) * demandToDistribute);
					numberOfJobs = numberPossibleJobsPickup;
				} else
					throw new RuntimeException(
							"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			} else {
				if (sampleSizeInputPopulation == sampleTo) {
					numberOfJobs = (int) Math.round(shareOfPopulationWithThisDelivery * numberPossibleJobsDelivery);
					numberPossibleJobsDelivery = numberOfJobs;
					numberPossibleJobsPickup = (int) Math
							.round(shareOfPopulationWithThisPickup * numberPossibleJobsPickup);
				} else if (samplingOption.equals("changeNumberOfLocationsWithDemand")) {
					numberOfJobs = sampledNumberPossibleJobsDelivery;
					numberPossibleJobsDelivery = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsPickup = sampledNumberPossibleJobsPickup;
				} else if (samplingOption.equals("changeDemandOnLocation")) {
					demandToDistribute = (int) Math.round((sampleTo / sampleSizeInputPopulation) * demandToDistribute);
					numberOfJobs = numberPossibleJobsDelivery;
				} else
					throw new RuntimeException(
							"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			}
			if (numberPossibleJobsPickup != 0)
				numberOfPickupLocations = numberPossibleJobsPickup;
			if (numberPossibleJobsDelivery != 0)
				numberOfDeliveryLocations = numberPossibleJobsDelivery;
		}
		// find possible Links for delivery and pickup
		HashMap<Id<Link>, Link> possibleLinksPickup = findAllPossibleLinks(scenario, indexShape,
				crsTransformationNetworkAndShape, numberOfPickupLocations, areasForPickupLocations,
				setLocationsOfPickup, possiblePersonsPickup, nearestLinkPerPersonPickup);
		HashMap<Id<Link>, Link> possibleLinksDelivery = findAllPossibleLinks(scenario, indexShape,
				crsTransformationNetworkAndShape, numberOfDeliveryLocations, areasForDeliveryLocations,
				setLocationsOfDelivery, possiblePersonsDelivery, nearestLinkPerPersonDelivery);

		if (possibleLinksPickup.isEmpty())
			throw new RuntimeException(
					"Not enough possible links to distribute the pickups. Select an different shapefile or check the CRS of the shapefile and network.");
		if (possibleLinksDelivery.isEmpty())
			throw new RuntimeException(
					"Not enough possible links to distribute the deliveries. Select an different shapefile or check the CRS of the shapefile and network.");

		if (setLocationsOfPickup != null)
			for (String selectedLinkIdPickups : setLocationsOfPickup)
				if (!possibleLinksPickup.containsKey(Id.createLinkId(selectedLinkIdPickups)))
					throw new RuntimeException("The selected link " + selectedLinkIdPickups
							+ " for pickup is not part of the possible links for pickup. Please check!");

		if (setLocationsOfDelivery != null)
			if (numberOfDeliveryLocations < setLocationsOfDelivery.length)
				log.warn("You selected more certain locations than the set number of locations. Randomly selected locations will be used.");
			else
				for (String selectedLinkIdDelivery : setLocationsOfDelivery)
					if (!possibleLinksDelivery.containsKey(Id.createLinkId(selectedLinkIdDelivery)))
						throw new RuntimeException("The selected link " + selectedLinkIdDelivery
							+ " for delivery is not part of the possible links for delivery. Please check!");

		// distribute the demand over the network because no number of jobs is selected
		if (numberOfJobs == null) {
			// creates shipments with a demand of 1
			if (possibleLinksPickup.size() > demandToDistribute || possibleLinksDelivery.size() > demandToDistribute) {
				for (int i = 0; i < demandToDistribute; i++) {
					Link linkPickup;
					Link linkDelivery;
					linkPickup = findNextUsedLink(scenario, indexShape, possibleLinksPickup,
							numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup, usedPickupLocations,
							possiblePersonsPickup, nearestLinkPerPersonPickup, crsTransformationNetworkAndShape, i);
					linkDelivery = findNextUsedLink(scenario, indexShape, possibleLinksDelivery,
							numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
							usedDeliveryLocations, possiblePersonsDelivery, nearestLinkPerPersonDelivery,
							crsTransformationNetworkAndShape, i);

					int demandForThisLink = 1;
					if (!usedPickupLocations.contains(linkPickup.getId().toString()))
						usedPickupLocations.add(linkPickup.getId().toString());
					if (!usedDeliveryLocations.contains(linkDelivery.getId().toString()))
						usedDeliveryLocations.add(linkDelivery.getId().toString());

					createSingleShipment(scenario, newDemandInformationElement, linkPickup, linkDelivery, demandForThisLink);
				}
			} else
			// creates a demand on each link, demand depends on the length of the link
			{
				if (numberOfPickupLocations != null && numberOfDeliveryLocations != null)
					throw new RuntimeException(
							"Because the demand is higher than the number of links, the demand will be distributed evenly over all links. You selected a certain number of pickup and delivery locations, which is not possible here!");
				HashMap<Id<Link>, Link> demandBasesLinks;
				double sumOfDemandBasedLinks;
				boolean pickupIsDemandBase = true;
				Link linkPickup = null;
				Link linkDelivery= null;
				double sumOfPossibleLinkLengthPickup = possibleLinksPickup.values().stream().mapToDouble(Link::getLength).sum();
				double sumOfPossibleLinkLengthDelivery = possibleLinksDelivery.values().stream().mapToDouble(Link::getLength).sum();
				if (numberOfPickupLocations == null && numberOfDeliveryLocations == null)
					if (possibleLinksPickup.size() > possibleLinksDelivery.size()) {
						demandBasesLinks = possibleLinksPickup;
						sumOfDemandBasedLinks = sumOfPossibleLinkLengthPickup;
					} else {
						demandBasesLinks = possibleLinksDelivery;
						sumOfDemandBasedLinks = sumOfPossibleLinkLengthDelivery;
						pickupIsDemandBase = false;
					}
				else if (numberOfPickupLocations != null) {
					demandBasesLinks = possibleLinksDelivery;
					sumOfDemandBasedLinks = sumOfPossibleLinkLengthDelivery;
					pickupIsDemandBase = false;
				} else {
					demandBasesLinks = possibleLinksPickup;
					sumOfDemandBasedLinks = sumOfPossibleLinkLengthPickup;
				}
				for (Link demandBasedLink : demandBasesLinks.values()) {
					int demandForThisLink = calculateDemandBasedOnLinkLength(countOfLinks, distributedDemand, demandToDistribute, demandBasesLinks.size(), sumOfDemandBasedLinks,
						demandBasedLink);
					if (pickupIsDemandBase) {
						linkPickup = demandBasedLink;
						while (linkDelivery == null || usedDeliveryLocations.contains(linkDelivery.getId().toString())) {
							linkDelivery = findNextUsedLink(scenario, indexShape, possibleLinksDelivery,
									numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
									usedDeliveryLocations, possiblePersonsDelivery, nearestLinkPerPersonDelivery,
									crsTransformationNetworkAndShape, countOfLinks - 1);
							if (usedDeliveryLocations.size() == possibleLinksDelivery.size()
									|| (numberOfDeliveryLocations != null
											&& usedDeliveryLocations.size() == numberOfDeliveryLocations))
								break;
						}
					} else {
						linkDelivery = demandBasedLink;
						while (linkPickup == null || usedPickupLocations.contains(linkPickup.getId().toString())) {
							linkPickup = findNextUsedLink(scenario, indexShape, possibleLinksPickup,
									numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup,
									usedPickupLocations, possiblePersonsPickup, nearestLinkPerPersonPickup,
									crsTransformationNetworkAndShape, countOfLinks - 1);
							if (usedPickupLocations.size() == possibleLinksPickup.size()
									|| (numberOfPickupLocations != null
											&& usedPickupLocations.size() == numberOfPickupLocations))
								break;
						}
					}
					countOfLinks++;
					if (!usedPickupLocations.contains(linkPickup.getId().toString()))
						usedPickupLocations.add(linkPickup.getId().toString());
					if (!usedDeliveryLocations.contains(linkDelivery.getId().toString()))
						usedDeliveryLocations.add(linkDelivery.getId().toString());

					if (demandForThisLink > 0) {
						createSingleShipment(scenario, newDemandInformationElement, linkPickup, linkDelivery,
							demandForThisLink);
					}
					distributedDemand = distributedDemand + demandForThisLink;
				}
			}
		} else
		// if a certain number of shipments is selected
		{
			for (int i = 0; i < numberOfJobs; i++) {

				if (demandToDistribute != 0 && demandToDistribute < numberOfJobs) {
					numberOfJobs = demandToDistribute;
					log.warn(
						"The resulting number of jobs is not feasible, because the demand is smaller then the number of jobs. Number of jobs is reduced to demand!");
				}
				Link linkPickup = findNextUsedLink(scenario, indexShape, possibleLinksPickup,
						numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup, usedPickupLocations,
						possiblePersonsPickup, nearestLinkPerPersonPickup, crsTransformationNetworkAndShape, i);
				Link linkDelivery = findNextUsedLink(scenario, indexShape, possibleLinksDelivery,
						numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
						usedDeliveryLocations, possiblePersonsDelivery, nearestLinkPerPersonDelivery,
						crsTransformationNetworkAndShape, i);
				int demandForThisLink = calculateDemandForThisLink(demandToDistribute, numberOfJobs, distributedDemand, i);

				if (!usedPickupLocations.contains(linkPickup.getId().toString()))
					usedPickupLocations.add(linkPickup.getId().toString());
				if (!usedDeliveryLocations.contains(linkDelivery.getId().toString()))
					usedDeliveryLocations.add(linkDelivery.getId().toString());

				createSingleShipment(scenario, newDemandInformationElement, linkPickup, linkDelivery,
					demandForThisLink);
				distributedDemand = distributedDemand + demandForThisLink;
			}
		}
		if (combineSimilarJobs)
			reduceNumberOfJobsIfSameCharacteristics(scenario, newDemandInformationElement);
	}

	/** Creates a single shipment.
	 * @param scenario                    Scenario
	 * @param newDemandInformationElement single DemandInformationElement
	 * @param linkPickup                  Link for the pickup
	 * @param linkDelivery                Link for the delivery
	 * @param demandForThisLink           Demand for this link
	 */
	private static void createSingleShipment(Scenario scenario, DemandInformationElement newDemandInformationElement,
											 Link linkPickup, Link linkDelivery, int demandForThisLink) {

		Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
		int numberOfJobsForDemand = calculateNumberOfJobsForDemand(thisCarrier, demandForThisLink);

		TimeWindow timeWindowPickup = newDemandInformationElement.getFirstJobElementTimeWindow();
		TimeWindow timeWindowDelivery = newDemandInformationElement.getSecondJobElementTimeWindow();

		for (int i = 0; i < numberOfJobsForDemand; i++) {
			Id<CarrierShipment> idNewShipment = Id.create(createJobId(scenario, newDemandInformationElement,
				linkPickup.getId(), linkDelivery.getId()), CarrierShipment.class);
			double serviceTimePickup;
			double serviceTimeDelivery;
			int singleDemandForThisLink = Math.round ((float) demandForThisLink / numberOfJobsForDemand);
			if (i == numberOfJobsForDemand - 1)
				singleDemandForThisLink = demandForThisLink - (numberOfJobsForDemand - 1) * singleDemandForThisLink;
			if (singleDemandForThisLink == 0) {
				serviceTimePickup = newDemandInformationElement.getFirstJobElementTimePerUnit();
				serviceTimeDelivery = newDemandInformationElement.getSecondJobElementTimePerUnit();
			} else {
				serviceTimePickup = newDemandInformationElement.getFirstJobElementTimePerUnit() * singleDemandForThisLink;
				serviceTimeDelivery = newDemandInformationElement.getSecondJobElementTimePerUnit() * singleDemandForThisLink;
			}
			CarrierShipment thisShipment = CarrierShipment.Builder
				.newInstance(idNewShipment, linkPickup.getId(), linkDelivery.getId(), singleDemandForThisLink)
				.setPickupServiceTime(serviceTimePickup).setPickupTimeWindow(timeWindowPickup)
				.setDeliveryServiceTime(serviceTimeDelivery).setDeliveryTimeWindow(timeWindowDelivery)
				.build();
			thisCarrier.getShipments().put(thisShipment.getId(), thisShipment);
		}
	}

	/**
	 * Method calculates the number of jobs for a demand on one link based on the largest vehicle capacity of the carrier.
	 *
	 * @param thisCarrier       the carrier of a job
	 * @param demandForThisLink Demand for this link
	 * @return Number of jobs for this demand
	 */
	private static int calculateNumberOfJobsForDemand(Carrier thisCarrier, int demandForThisLink) {
		double largestVehicleCapacity = 0;
		for (CarrierVehicle vehicle : thisCarrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			if (vehicle.getType().getCapacity().getOther() > largestVehicleCapacity) {
				largestVehicleCapacity = vehicle.getType().getCapacity().getOther();
			}
		}
		if (demandForThisLink > largestVehicleCapacity) {
			log.info("Demand {} is larger than the largest vehicle capacity ({}). Splitting demand into multiple jobs.", demandForThisLink, largestVehicleCapacity);
			return (int) Math.ceil((double) demandForThisLink / largestVehicleCapacity);
		}
		return 1;
	}

	/**
	 * Creates a job Id for a new job.
	 * If a certain Id is already used, a number will be added at the end until no existing job was the same Id.
	 *
	 * @param scenario 						Scenario
	 * @param newDemandInformationElement 	single DemandInformationElement
	 * @param linkPickup 					Link for the pickup
	 * @param linkDelivery 					Link for the delivery
	 * @return 								New Job Id
	 */
	private static String createJobId(Scenario scenario, DemandInformationElement newDemandInformationElement,
			Id<Link> linkPickup, Id<Link> linkDelivery) {
		String newJobId;
		if (linkDelivery != null) {
			newJobId = "Shipment_" + linkPickup + "_" + linkDelivery;
			if (CarriersUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getShipments()
					.containsKey(Id.create(newJobId, CarrierShipment.class))) {
				for (int x = 1; CarriersUtils.getCarriers(scenario).getCarriers()
						.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getShipments()
						.containsKey(Id.create(newJobId, CarrierShipment.class)); x++) {
					newJobId = "Shipment_" + linkPickup + "_" + linkDelivery + "_" + x;
				}
			}
		} else {
			newJobId = "Service_" + linkPickup;
			if (CarriersUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getServices()
					.containsKey(Id.create(newJobId, CarrierShipment.class))) {
				for (int x = 1; CarriersUtils.getCarriers(scenario).getCarriers()
						.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getServices()
						.containsKey(Id.create(newJobId, CarrierShipment.class)); x++) {
					newJobId = "Service_" + linkPickup + "_" + x;
				}
			}
		}
		return newJobId;
	}

	/** Calculates the demand for this link including checking the rounding error.
	 * @param demandToDistribute  	Demand to distribute
	 * @param numberOfJobs 	   		Number of jobs
	 * @param distributedDemand 	Already Distributed demand
	 * @param i 					Counter
	 * @return 						Demand for this link
	 */
	private static int calculateDemandForThisLink(int demandToDistribute, int numberOfJobs, int distributedDemand, int i) {

		int demandForThisLink = (int) Math.ceil((double) demandToDistribute / (double) numberOfJobs);
		if (numberOfJobs == (i + 1)) {
			demandForThisLink = demandToDistribute - distributedDemand;
		} else {
			roundingError = roundingError
				+ ((double) demandForThisLink - ((double) demandToDistribute / (double) numberOfJobs));
			if (roundingError >= 1) {
				demandForThisLink = demandForThisLink - 1;
				roundingError = roundingError - 1;
			}
		}
		return demandForThisLink;
	}

	/**
	 * @param countOfLinks				counter
	 * @param distributedDemand 		Already distributed demand
	 * @param demandToDistribute 		Demand to distribute
	 * @param maxLinks					Maximum of possible links for demand
	 * @param sumOfPossibleLinkLength	Sum of all lengths of the links
	 * @param link 						this link
	 * @return							Demand for this link
	 */
	private static int calculateDemandBasedOnLinkLength(int countOfLinks, int distributedDemand, Integer demandToDistribute,
														int maxLinks, double sumOfPossibleLinkLength, Link link) {
		int demandForThisLink;
		if (countOfLinks == maxLinks) {
			demandForThisLink = demandToDistribute - distributedDemand;
		} else {
			demandForThisLink = (int) Math
				.ceil(link.getLength() / sumOfPossibleLinkLength * (double) demandToDistribute);
			roundingError = roundingError + ((double) demandForThisLink
				- (link.getLength() / sumOfPossibleLinkLength * (double) demandToDistribute));
			if (roundingError >= 1) {
				demandForThisLink = demandForThisLink - 1;
				roundingError = roundingError - 1;
			}
		}
		return demandForThisLink;
	}
	/**
	 * If jobs of a carrier have the same characteristics (time window, location),
	 * they will be combined to one job.
	 *
	 * @param scenario 						Scenario
	 * @param newDemandInformationElement 	single DemandInformationElement
	 */
	private static void reduceNumberOfJobsIfSameCharacteristics(Scenario scenario,
			DemandInformationElement newDemandInformationElement) {

		log.warn(
				"The number of Jobs will be reduced if jobs have the same characteristics (e.g. time, location, carrier)");
		int connectedJobs = 0;
		if (newDemandInformationElement.getTypeOfDemand().equals("shipment")) {
			HashMap<Id<CarrierShipment>, CarrierShipment> shipmentsToRemove = new HashMap<>();
			ArrayList<CarrierShipment> shipmentsToAdd = new ArrayList<>();
			Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
			for (Id<CarrierShipment> baseShipmentId : thisCarrier.getShipments().keySet()) {
				if (!shipmentsToRemove.containsKey(baseShipmentId)) {
					CarrierShipment baseShipment = thisCarrier.getShipments().get(baseShipmentId);
					HashMap<Id<CarrierShipment>, CarrierShipment> shipmentsToConnect = new HashMap<>();
					shipmentsToConnect.put(baseShipmentId, baseShipment);
					for (Id<CarrierShipment> thisShipmentId : thisCarrier.getShipments().keySet()) {
						if (!shipmentsToRemove.containsKey(thisShipmentId)) {
							CarrierShipment thisShipment = thisCarrier.getShipments().get(thisShipmentId);
							if (baseShipment.getId() != thisShipment.getId()
									&& baseShipment.getFrom() == thisShipment.getFrom()
									&& baseShipment.getTo() == thisShipment.getTo()
									&& baseShipment.getPickupTimeWindow() == thisShipment.getPickupTimeWindow()
									&& baseShipment.getDeliveryTimeWindow() == thisShipment.getDeliveryTimeWindow())
								shipmentsToConnect.put(thisShipmentId, thisShipment);
						}
					}
					Id<CarrierShipment> idNewShipment = baseShipment.getId();
					int demandForThisLink = 0;
					double serviceTimePickup = 0;
					double serviceTimeDelivery = 0;
					for (CarrierShipment carrierShipment : shipmentsToConnect.values()) {
						demandForThisLink = demandForThisLink + carrierShipment.getSize();
						serviceTimePickup = serviceTimePickup + carrierShipment.getPickupServiceTime();
						serviceTimeDelivery = serviceTimeDelivery + carrierShipment.getDeliveryServiceTime();
						shipmentsToRemove.put(carrierShipment.getId(), carrierShipment);
						connectedJobs++;
					}
					CarrierShipment newShipment = CarrierShipment.Builder
							.newInstance(idNewShipment, baseShipment.getFrom(), baseShipment.getTo(), demandForThisLink)
							.setPickupServiceTime(serviceTimePickup)
							.setPickupTimeWindow(baseShipment.getPickupTimeWindow())
							.setDeliveryServiceTime(serviceTimeDelivery)
							.setDeliveryTimeWindow(baseShipment.getDeliveryTimeWindow()).build();

					shipmentsToAdd.add(newShipment);
					connectedJobs++;
				}
			}
			for (CarrierShipment id : shipmentsToRemove.values())
				thisCarrier.getShipments().remove(id.getId(), id);

			for (CarrierShipment carrierShipment : shipmentsToAdd) {
				thisCarrier.getShipments().put(carrierShipment.getId(), carrierShipment);
			}
			log.warn("Number of reduced shipments: {}", connectedJobs);
		}
		if (newDemandInformationElement.getTypeOfDemand().equals("service")) {
			HashMap<Id<CarrierService>, CarrierService> servicesToRemove = new HashMap<>();
			ArrayList<CarrierService> servicesToAdd = new ArrayList<>();
			Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
			for (Id<CarrierService> baseServiceId : thisCarrier.getServices().keySet()) {
				if (!servicesToRemove.containsKey(baseServiceId)) {
					CarrierService baseService = thisCarrier.getServices().get(baseServiceId);
					HashMap<Id<CarrierService>, CarrierService> servicesToConnect = new HashMap<>();
					servicesToConnect.put(baseServiceId, baseService);
					for (Id<CarrierService> thisServiceId : thisCarrier.getServices().keySet()) {
						if (!servicesToRemove.containsKey(thisServiceId)) {
							CarrierService thisService = thisCarrier.getServices().get(thisServiceId);
							if (baseService.getId() != thisService.getId()
									&& baseService.getLocationLinkId() == thisService.getLocationLinkId() && baseService
											.getServiceStartTimeWindow() == thisService.getServiceStartTimeWindow())
								servicesToConnect.put(thisServiceId, thisService);
						}
					}
					Id<CarrierService> idNewService = baseService.getId();
					int demandForThisLink = 0;
					double serviceTimeService = 0;
					for (CarrierService carrierService : servicesToConnect.values()) {
						demandForThisLink = demandForThisLink + carrierService.getCapacityDemand();
						serviceTimeService = serviceTimeService + carrierService.getServiceDuration();
						servicesToRemove.put(carrierService.getId(), carrierService);
						connectedJobs++;
					}
					CarrierService newService = CarrierService.Builder
							.newInstance(idNewService, baseService.getLocationLinkId())
							.setServiceDuration(serviceTimeService)
							.setServiceStartTimeWindow(baseService.getServiceStartTimeWindow())
							.setCapacityDemand(demandForThisLink).build();
					servicesToAdd.add(newService);
					connectedJobs++;
				}
			}
			for (CarrierService id : servicesToRemove.values())
				thisCarrier.getServices().remove(id.getId(), id);
			for (CarrierService carrierService : servicesToAdd) {
				thisCarrier.getServices().put(carrierService.getId(), carrierService);
			}
			log.warn("Number of reduced shipments: {}", connectedJobs);
		}
	}

	/**
	 * Finds and returns all possible links for this job.
	 *
	 * @param scenario 							Scenario
	 * @param indexShape 						ShpOptions.Index for the shape file
	 * @param crsTransformationNetworkAndShape	CoordinateTransformation for the network and shape file
	 * @param numberOfLocations					Number of locations for this demand
	 * @param areasForLocations 				Areas for the locations
	 * @param setLocations 						Selected locations
	 * @param possiblePersons					Persons that are possible for this demand
	 * @param nearestLinkPerPerson 				Nearest link for each person
	 * @return 									HashMap with all possible links
	 */
	private static HashMap<Id<Link>, Link> findAllPossibleLinks(Scenario scenario,
																ShpOptions.Index indexShape, CoordinateTransformation crsTransformationNetworkAndShape,
																Integer numberOfLocations, String[] areasForLocations, String[] setLocations,
																HashMap<Id<Person>, Person> possiblePersons,
																HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPerson) {
		HashMap<Id<Link>, Link> possibleLinks = new HashMap<>();
		if (numberOfLocations == null) {
			for (Link link : scenario.getNetwork().getLinks().values())
				if (!link.getId().toString().contains("pt") && (!link.getAttributes().getAsMap().containsKey(
					"type") || !link.getAttributes().getAsMap().get("type").toString().contains(
					"motorway")) && FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
					areasForLocations, crsTransformationNetworkAndShape)) {
					possibleLinks.put(link.getId(), link);
				}
		} else {
			Link newPossibleLink;
			while (possibleLinks.size() < numberOfLocations) {
				newPossibleLink = findPossibleLinkForDemand(possibleLinks, possiblePersons, nearestLinkPerPerson,
					indexShape, areasForLocations, numberOfLocations, scenario, setLocations,
						crsTransformationNetworkAndShape);
				if (!possibleLinks.containsKey(newPossibleLink.getId()))
					possibleLinks.put(newPossibleLink.getId(), newPossibleLink);
				if (!possiblePersons.isEmpty() && nearestLinkPerPerson.size() == possiblePersons.size())
					break;
			}
		}
		return possibleLinks;
	}

	/**
	 * Finds the next link which can be used as a location.
	 *
	 * @param scenario  						Scenario
	 * @param indexShape 						ShpOptions.Index for the shape file
	 * @param possibleLinks 					All possible links
	 * @param selectedNumberOfLocations 		Number of locations for this demand
	 * @param areasForLocations 				Areas for the locations
	 * @param selectedLocations 				Selected locations
	 * @param usedLocations 					Already used locations for this demand
	 * @param possiblePersons 					Persons that are possible for this demand
	 * @param nearestLinkPerPerson 				Nearest link for each person
	 * @param crsTransformationNetworkAndShape 	CoordinateTransformation for the network and shape file
	 * @param i 								Counter for the number of locations
	 * @return 									Next link for the demand
	 */
	private static Link findNextUsedLink(Scenario scenario, ShpOptions.Index indexShape,
			HashMap<Id<Link>, Link> possibleLinks, Integer selectedNumberOfLocations, String[] areasForLocations,
			String[] selectedLocations, ArrayList<String> usedLocations, HashMap<Id<Person>, Person> possiblePersons,
			HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPerson,
			CoordinateTransformation crsTransformationNetworkAndShape, int i) {
		Link link = null;

		if (selectedNumberOfLocations == null || usedLocations.size() < selectedNumberOfLocations) {
			if (selectedLocations != null && selectedLocations.length > i) {
				link = scenario.getNetwork().getLinks().get(Id.createLinkId(selectedLocations[i]));
			} else
				while (link == null || (possibleLinks.size() > usedLocations.size()
						&& usedLocations.contains(link.getId().toString())))
					link = findPossibleLinkForDemand(possibleLinks, possiblePersons, nearestLinkPerPerson,
						indexShape, areasForLocations, selectedNumberOfLocations, scenario, selectedLocations,
							crsTransformationNetworkAndShape);
		} else {
			link = scenario.getNetwork().getLinks()
					.get(Id.createLinkId(usedLocations.get(rand.nextInt(usedLocations.size()))));
		}
		return link;
	}

	/**
	 * Finds all persons that are possible for the demand.
	 *
	 * @param population 						Population
	 * @param areasForJobElementLocations 		Areas for the locations
	 * @param indexShape 						ShpOptions.Index for the shape file
	 * @param crsTransformationNetworkAndShape 	CoordinateTransformation for the network and shape file
	 * @return 									HashMap with all possible persons
	 */
	private static HashMap<Id<Person>, Person> findPossiblePersons(Population population,
			String[] areasForJobElementLocations, ShpOptions.Index indexShape,
			CoordinateTransformation crsTransformationNetworkAndShape) {

		HashMap<Id<Person>, Person> possiblePersons = new HashMap<>();

		for (Person person : population.getPersons().values()) {
			Coord coord = getHomeCoord(person);
			if (crsTransformationNetworkAndShape != null)
				coord = crsTransformationNetworkAndShape.transform(coord);

			if (FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape,
					areasForJobElementLocations, crsTransformationNetworkAndShape))
				possiblePersons.put(person.getId(), person);
		}
		return possiblePersons;
	}

	/**
	 * Finds the nearest link for one person.
	 *
	 * @param scenario 				Scenario
	 * @param nearestLinkPerPerson 	HashMap with the nearest link for each person
	 * @param person 				Person for which the nearest link should be found
	 */
	static void findLinksForPerson(Scenario scenario,
								   HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPerson, Person person) {
		Coord homePoint = getHomeCoord(person);
		for (Link link : scenario.getNetwork().getLinks().values())
			if (!link.getId().toString().contains("pt") && (!link.getAttributes().getAsMap().containsKey("type")
					|| !link.getAttributes().getAsMap().get("type").toString().contains("motorway"))) {

				Coord middlePointLink = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(link);
				double distance = NetworkUtils.getEuclideanDistance(homePoint, middlePointLink);
				if (!nearestLinkPerPerson.containsKey(person.getId())
						|| distance < nearestLinkPerPerson.get(person.getId()).keySet().iterator().next()) {
					nearestLinkPerPerson.put(person.getId(), new HashMap<>());
					nearestLinkPerPerson.get(person.getId()).put(distance, link.getId().toString());
				}
			}
	}

	/**
	 * Method to get the home coordinate of a person.
	 * The default is to get the home coordinate from one home activity of the selected plan.
	 * If the selected plan does not contain a home activity, the home coordinate is read from the attributes of the person.
	 *
	 * @param person 	The person for which the home coordinate should be returned.
	 * @return 			The home coordinate of the person.
	 */
	private static Coord getHomeCoord(Person person) {
		Coord homeCoord = null;
		if (person.getSelectedPlan() != null)
			homeCoord = PopulationUtils.getActivities(person.getSelectedPlan(),
				TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream().filter(
				activity -> activity.getType().contains("home")).findFirst().get().getCoord();
		if (homeCoord == null) {
			double home_x = (double) person.getAttributes().getAsMap().entrySet().stream().filter(
				entry -> entry.getKey().contains("home") && entry.getKey().contains("X")).findFirst().get().getValue();
			double home_y = (double) person.getAttributes().getAsMap().entrySet().stream().filter(
				entry -> entry.getKey().contains("home") && entry.getKey().contains("Y")).findFirst().get().getValue();
			homeCoord = new Coord(home_x, home_y);
		}
		return homeCoord;
	}

	/**
	 * Searches a possible link for the demand.
	 *
	 * @param possibleLinks 					HashMap with all possible links
	 * @param possiblePersons 					HashMap with all possible persons
	 * @param nearestLinkPerPerson				Nearest link for each person
	 * @param indexShape 						ShpOptions.Index for the shape file
	 * @param areasForTheDemand 				Areas for the demand
	 * @param selectedNumberOfLocations 		Number of locations for this demand
	 * @param scenario 							Scenario
	 * @param selectedLocations 				Selected locations
	 * @param crsTransformationNetworkAndShape 	CoordinateTransformation for the network and shape file
	 * @return 									The selected link for the demand
	 */
	private static Link findPossibleLinkForDemand(HashMap<Id<Link>, Link> possibleLinks,
												  HashMap<Id<Person>, Person> possiblePersons,
												  HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPerson,
												  ShpOptions.Index indexShape, String[] areasForTheDemand, Integer selectedNumberOfLocations,
												  Scenario scenario, String[] selectedLocations, CoordinateTransformation crsTransformationNetworkAndShape) {
		Link selectedlink = null;
		Link newLink;
		if (selectedNumberOfLocations == null)
			selectedNumberOfLocations = 0;
		while (selectedlink == null) {
			if (possibleLinks.size() < selectedNumberOfLocations) {
				if (selectedLocations != null && selectedLocations.length > possibleLinks.size()) {
					newLink = scenario.getNetwork().getLinks()
							.get(Id.createLinkId(selectedLocations[possibleLinks.size()]));
					selectedlink = newLink;
					break;
				} else {
					if (possiblePersons.isEmpty())
						newLink = scenario.getNetwork().getLinks().values().stream()
								.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
					else {
						newLink = getNewLinkForPerson(possiblePersons, nearestLinkPerPerson, scenario);
					}
				}
			} else {
				if (possiblePersons.isEmpty()) {
					newLink = possibleLinks.values().stream().skip(rand.nextInt(possibleLinks.size())).findFirst()
							.get();
				} else {
					newLink = getNewLinkForPerson(possiblePersons, nearestLinkPerPerson, scenario);
				}
			}
			if (!newLink.getId().toString().contains("pt")
					&& (!newLink.getAttributes().getAsMap().containsKey("type")
							|| !newLink.getAttributes().getAsMap().get("type").toString().contains("motorway"))
					&& (indexShape == null || FreightDemandGenerationUtils.checkPositionInShape(newLink, null,
				indexShape, areasForTheDemand, crsTransformationNetworkAndShape)))
				selectedlink = newLink;
		}
		return selectedlink;
	}

	private static Link getNewLinkForPerson(HashMap<Id<Person>, Person> possiblePersons,
											HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPerson, Scenario scenario) {
		Link newLink;
		Person person = possiblePersons.values().stream().skip(rand.nextInt(possiblePersons.size()))
			.findFirst().get();
		if (!nearestLinkPerPerson.containsKey(person.getId())) {
			findLinksForPerson(scenario, nearestLinkPerPerson, person);
		}
		newLink = scenario.getNetwork().getLinks().get(
			Id.createLinkId(nearestLinkPerPerson.get(person.getId()).values().iterator().next()));
		return newLink;
	}
}
