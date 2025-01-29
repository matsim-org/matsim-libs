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
	public static class DemandInformationElement {

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

	public static class LinkPersonPair {
		Link link;
		Person person;

		LinkPersonPair(Link link, Person person) {
			this.link = link;
			this.person = person;
		}

		public Link getLink() {
			return link;
		}

		public Person getPerson() {
			return person;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LinkPersonPair that = (LinkPersonPair) o;
			return Objects.equals(link, that.link) && Objects.equals(person, that.person);
		}

		@Override
		public int hashCode() {
			return Objects.hash(link, person);
		}

		public void setLink(Link link) {
			this.link = link;
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
	 * @param demandGenerationSpecification				Calculator for the job duration
	 * @throws IOException if the csv file cannot be read
	 */
	static void readAndCreateDemand(Scenario scenario, Path csvLocationDemand,
									ShpOptions.Index indexShape, boolean combineSimilarJobs,
									CoordinateTransformation crsTransformationNetworkAndShape, Population population, String shapeCategory,
									DemandGenerationSpecification demandGenerationSpecification) throws IOException {

		Set<DemandInformationElement> demandInformation = readDemandInformation(csvLocationDemand);
		checkNewDemand(scenario, demandInformation, indexShape, shapeCategory);
		createDemandForCarriers(scenario, indexShape, demandInformation, population, combineSimilarJobs,
				crsTransformationNetworkAndShape, demandGenerationSpecification);
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
						for (SimpleFeature singlePolygon : indexShape.getAllFeatures()){
							if (singlePolygon.getAttribute(shapeCategory).toString().equals(demand)) {
								isInShape = true;
								break;
							}
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
	 * @param scenario                         Scenario
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param demandInformation                Set<DemandInformationElement> with the demand information
	 * @param population                       Population
	 * @param combineSimilarJobs               boolean if the jobs of the same carrier with same location and time will be combined
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param demandGenerationSpecification				Calculator for the job duration
	 */
	static void createDemandForCarriers(Scenario scenario, ShpOptions.Index indexShape,
										Set<DemandInformationElement> demandInformation, Population population, boolean combineSimilarJobs,
										CoordinateTransformation crsTransformationNetworkAndShape, DemandGenerationSpecification demandGenerationSpecification) {

		for (DemandInformationElement newDemandInformationElement : demandInformation) {
			log.info("Create demand for carrier {}", newDemandInformationElement.getCarrierName());
			if (newDemandInformationElement.getTypeOfDemand().equals("service"))
				createServices(scenario, newDemandInformationElement, indexShape, population,
						crsTransformationNetworkAndShape, demandGenerationSpecification);
			else if (newDemandInformationElement.getTypeOfDemand().equals("shipment"))
				createShipments(scenario, newDemandInformationElement, indexShape, population,
						crsTransformationNetworkAndShape, demandGenerationSpecification);
		}
		if (combineSimilarJobs)
			combineSimilarJobs(scenario);
		demandGenerationSpecification.recalculateJobDurations(scenario);
	}

	/**
	 * Creates the services.
	 *
	 * @param scenario                         Scenario
	 * @param newDemandInformationElement      single DemandInformationElement
	 * @param indexShape                       ShpOptions.Index
	 * @param population                       Population
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param demandGenerationSpecification				Calculator for the job duration
	 */
	private static void createServices(Scenario scenario, DemandInformationElement newDemandInformationElement,
									   ShpOptions.Index indexShape, Population population,
									   CoordinateTransformation crsTransformationNetworkAndShape, DemandGenerationSpecification demandGenerationSpecification) {

		int countOfLinks = 1;
		int distributedDemand = 0;
		Double shareOfPopulationWithThisService = newDemandInformationElement.getShareOfPopulationWithFirstJobElement();
		Integer numberOfJobs;
		String[] areasForServiceLocations = newDemandInformationElement.getAreasFirstJobElement();
		String[] locationsOfServices = newDemandInformationElement.getLocationsOfFirstJobElement();
		Integer numberOfServiceLocations = newDemandInformationElement.getNumberOfFirstJobElementLocations();
		ArrayList<LinkPersonPair> usedServiceLocationsOrPersons = new ArrayList<>();
		int numberOfLinksInNetwork = scenario.getNetwork().getLinks().size();
		HashMap<Id<Person>, Person> possiblePersonsForService = new HashMap<>();
		HashMap<Id<Person>, TreeMap <Double, String>> nearestLinkPerPerson = new HashMap<>();

		// set number of jobs
		if (shareOfPopulationWithThisService == null) {
			numberOfJobs = newDemandInformationElement.getNumberOfJobs();
			if (population != null)
				log.warn(
					"You have a population but no share of the population for the demand. The number of jobs will be set to the number of jobs you set in the csv file. The population will not be used for the demand generation.");
		}
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
			int sampledNumberPossibleServices = (int) Math.round((sampleTo / sampleSizeInputPopulation) * numberPossibleServices);
			if (sampleSizeInputPopulation == sampleTo || samplingOption.equals("changeDemandOnLocation"))
				numberOfJobs = numberPossibleServices;
			else if (samplingOption.equals("changeNumberOfLocationsWithDemand")) {
				numberOfJobs = sampledNumberPossibleServices;
				numberPossibleServices = numberOfJobs;
			}
			else
				throw new RuntimeException(
					"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			if (numberPossibleServices != 0)
				numberOfServiceLocations = numberPossibleServices;
		}
		// find possible links for the services
		ArrayList<LinkPersonPair> possibleLinkPersonPairsForService = findAllPossibleLinks(scenario, indexShape,
			crsTransformationNetworkAndShape, numberOfServiceLocations, areasForServiceLocations,
			locationsOfServices, possiblePersonsForService);

		if (locationsOfServices != null)
			for (String selectedLinkIdService : locationsOfServices)
				if (possibleLinkPersonPairsForService.stream().noneMatch(linkPersonPair -> linkPersonPair.getLink().getId().toString().equals(selectedLinkIdService)))
					throw new RuntimeException("The selected link " + selectedLinkIdService
						+ " for the service is not part of the possible links. Please check!");

		log.info("Number of service locations for this job element of carrier {}: {}", newDemandInformationElement.getCarrierName(), numberOfServiceLocations);
		int demandToDistribute = demandGenerationSpecification.getDemandToDistribute(newDemandInformationElement, possiblePersonsForService, null);

		if (numberOfJobs == null) {
			// creates services with a demand of 1
			if (possibleLinkPersonPairsForService.size() > demandToDistribute) {
				for (int i = 0; i < demandToDistribute; i++) {

					LinkPersonPair linkPersonPair = findNextUsedLinkPersonPair(scenario, indexShape, possibleLinkPersonPairsForService, numberOfJobs,
							areasForServiceLocations, locationsOfServices, usedServiceLocationsOrPersons,
							possiblePersonsForService, nearestLinkPerPerson, crsTransformationNetworkAndShape, i);
					int demandForThisLink = 1;
					double serviceTime = demandGenerationSpecification.calculateServiceDuration(newDemandInformationElement.getFirstJobElementTimePerUnit(), demandForThisLink);
					usedServiceLocationsOrPersons.add(linkPersonPair);
					Id<CarrierService> idNewService = Id.create(
						createJobId(scenario, newDemandInformationElement, linkPersonPair.getLink().getId(), null),
						CarrierService.class);
					CarrierService.Builder builder = CarrierService.Builder.newInstance(idNewService, linkPersonPair.getLink().getId(),
							demandForThisLink).setServiceDuration(serviceTime);
					CarrierService thisService = builder.setServiceStartingTimeWindow(newDemandInformationElement.getFirstJobElementTimeWindow())
						.build();
					CarriersUtils.getCarriers(scenario).getCarriers()
						.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getServices()
						.put(thisService.getId(), thisService);
				}
			} else
			// creates a demand on each link, demand depends on the length of the link
			{
				if (possibleLinkPersonPairsForService.isEmpty())
					throw new RuntimeException(
						"Not enough links in the shape file to distribute the demand. Select an different shapefile or check the CRS of the shapefile and network");
				if (numberOfServiceLocations != null)
					throw new RuntimeException(
						"Because the demand is higher than the number of links, the demand will be distributed evenly over all links. You selected a certain number of service locations, which is not possible here!");
				double sumOfPossibleLinkLength = possibleLinkPersonPairsForService.stream().mapToDouble(linkPersonPair -> linkPersonPair.getLink().getLength()).sum();
				for (Link link : possibleLinkPersonPairsForService.stream().map(LinkPersonPair::getLink).toList()) {
					int demandForThisLink = demandGenerationSpecification.calculateDemandBasedOnLinkLength(countOfLinks, distributedDemand, demandToDistribute, possibleLinkPersonPairsForService.size(),
						sumOfPossibleLinkLength, link);
					countOfLinks++;
					Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
					int handledDemand = 0;
					//the number of jobs on this link is calculated based on the available vehicles
					double largestPossibleDemandSize = getLargestVehicleCapacity(thisCarrier);
					while (handledDemand < demandForThisLink) {
						int singleDemandForThisLink;
						if (demandForThisLink - handledDemand < largestPossibleDemandSize)
							singleDemandForThisLink = demandForThisLink - handledDemand;
						else
							singleDemandForThisLink = (int)largestPossibleDemandSize;
						handledDemand = handledDemand + singleDemandForThisLink;
						double serviceTime = demandGenerationSpecification.calculateServiceDuration(
							newDemandInformationElement.getFirstJobElementTimePerUnit(), singleDemandForThisLink);

						Id<CarrierService> idNewService = Id.create(
							createJobId(scenario, newDemandInformationElement, link.getId(), null),
							CarrierService.class);
						if (demandToDistribute > 0 && singleDemandForThisLink > 0) {
							CarrierService.Builder builder = CarrierService.Builder.newInstance(idNewService, link.getId(), singleDemandForThisLink).setServiceDuration(serviceTime);
							CarrierService thisService = builder.setServiceStartingTimeWindow(newDemandInformationElement.getFirstJobElementTimeWindow())
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
				LinkPersonPair linkPersonPair;
				if (numberOfServiceLocations == null || usedServiceLocationsOrPersons.size() < numberOfServiceLocations) {
					if (locationsOfServices != null && locationsOfServices.length > i) {
						linkPersonPair = new LinkPersonPair(scenario.getNetwork().getLinks().get(Id.createLinkId(locationsOfServices[i])), null);
					} else
						linkPersonPair = findNextUsedLinkPersonPair(scenario, indexShape, possibleLinkPersonPairsForService,
							numberOfServiceLocations, areasForServiceLocations, locationsOfServices,
							usedServiceLocationsOrPersons, possiblePersonsForService, nearestLinkPerPerson,
							crsTransformationNetworkAndShape, i);
				} else {
					linkPersonPair = usedServiceLocationsOrPersons.stream().skip(rand.nextInt(usedServiceLocationsOrPersons.size() - 1)).findFirst().get();
				}
				int demandForThisLink = demandGenerationSpecification.calculateDemandForThisLinkWithFixNumberOfJobs(demandToDistribute, numberOfJobs, distributedDemand,
					linkPersonPair, null, i);
				Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
				int handledDemand = 0;
				//the number of jobs on this link is calculated based on the available vehicles
				double largestPossibleDemandSize = getLargestVehicleCapacity(thisCarrier);
				while (handledDemand < demandForThisLink || demandToDistribute == 0) {
					int singleDemandForThisLink;
					if (demandForThisLink - handledDemand < largestPossibleDemandSize)
						singleDemandForThisLink = demandForThisLink - handledDemand;
					else
						singleDemandForThisLink = (int)largestPossibleDemandSize;
					handledDemand = handledDemand + singleDemandForThisLink;
					double serviceTime = demandGenerationSpecification.calculateServiceDuration(
						newDemandInformationElement.getFirstJobElementTimePerUnit(), singleDemandForThisLink);
					usedServiceLocationsOrPersons.add(linkPersonPair);

					Id<CarrierService> idNewService = Id.create(
						createJobId(scenario, newDemandInformationElement, linkPersonPair.getLink().getId(), null), CarrierService.class);
					if ((demandToDistribute > 0 && singleDemandForThisLink > 0) || demandToDistribute == 0) {
						CarrierService.Builder builder = CarrierService.Builder.newInstance(idNewService, linkPersonPair.getLink().getId(),
								singleDemandForThisLink).setServiceDuration(serviceTime);
						CarrierService thisService = builder.setServiceStartingTimeWindow(newDemandInformationElement.getFirstJobElementTimeWindow())
							.build();
						CarriersUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class)).getServices()
							.put(thisService.getId(), thisService);
					if (demandToDistribute == 0)
						break;
					}
				}
				distributedDemand = distributedDemand + demandForThisLink;
			}
		}
	}

	/**
	 * Creates the shipments of a carrier.
	 *
	 * @param scenario                         Scenario
	 * @param newDemandInformationElement      single DemandInformationElement
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param population                       Population
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param demandGenerationSpecification				Calculator for the job duration
	 */
	private static void createShipments(Scenario scenario, DemandInformationElement newDemandInformationElement,
										ShpOptions.Index indexShape, Population population,
										CoordinateTransformation crsTransformationNetworkAndShape, DemandGenerationSpecification demandGenerationSpecification) {

		int countOfLinks = 1;
		int distributedDemand = 0;
		Double shareOfPopulationWithThisPickup = newDemandInformationElement.getShareOfPopulationWithFirstJobElement();
		Double shareOfPopulationWithThisDelivery = newDemandInformationElement
			.getShareOfPopulationWithSecondJobElement();
		Integer numberOfJobs;
		Integer numberOfPickupLocations = newDemandInformationElement.getNumberOfFirstJobElementLocations();
		Integer numberOfDeliveryLocations = newDemandInformationElement.getNumberOfSecondJobElementLocations();
		String[] areasForPickupLocations = newDemandInformationElement.getAreasFirstJobElement();
		String[] areasForDeliveryLocations = newDemandInformationElement.getAreasSecondJobElement();
		String[] setLocationsOfPickup = newDemandInformationElement.getLocationsOfFirstJobElement();
		String[] setLocationsOfDelivery = newDemandInformationElement.getLocationsOfSecondJobElement();
		ArrayList<LinkPersonPair> usedPickupLocationsOrPersons = new ArrayList<>();
		ArrayList<LinkPersonPair> usedDeliveryLocationsOrPersons = new ArrayList<>();
		HashMap<Id<Person>, Person> possiblePersonsPickup = new HashMap<>();
		HashMap<Id<Person>, Person> possiblePersonsDelivery = new HashMap<>();
		HashMap<Id<Person>, TreeMap <Double, String>> nearestLinkPerPersonPickup = new HashMap<>();
		HashMap<Id<Person>, TreeMap <Double, String>> nearestLinkPerPersonDelivery = new HashMap<>();

		// set number of jobs part 1
		if (shareOfPopulationWithThisPickup == null && shareOfPopulationWithThisDelivery == null){
			if (population != null)
				log.warn(
					"You have a population but no share of the population for the demand. The number of jobs will be set to the number of jobs you set in the csv file. The population will not be used for the demand generation.");
			numberOfJobs = newDemandInformationElement.getNumberOfJobs();
		}
		else if (population == null)
			throw new RuntimeException(
				"No population found although input parameter <ShareOfPopulationWithThisDemand> is set");
		else {

			double sampleSizeInputPopulation = (double) population.getAttributes().getAttribute("sampleSize");
			double sampleTo = (double) population.getAttributes().getAttribute("samplingTo");
			String samplingOption = String.valueOf(population.getAttributes().getAttribute("samplingOption"));

			//Pickup
			if (setLocationsOfPickup == null || setLocationsOfPickup.length != numberOfPickupLocations)
				if (areasForPickupLocations != null || indexShape != null)
					possiblePersonsPickup = findPossiblePersons(population, areasForPickupLocations, indexShape,
						crsTransformationNetworkAndShape);
				else
					possiblePersonsPickup.putAll(population.getPersons());

			//Delivery
			if (setLocationsOfDelivery == null || setLocationsOfDelivery.length != numberOfDeliveryLocations)
				if (areasForDeliveryLocations != null || indexShape != null) {
					possiblePersonsDelivery = findPossiblePersons(population, areasForDeliveryLocations, indexShape,
						crsTransformationNetworkAndShape);
				} else {
					possiblePersonsDelivery.putAll(population.getPersons());
				}

			// set number of jobs part 2, upsampling
			int numberPossibleJobsPickup = 0;
			int numberPossibleJobsDelivery = 0;
			if (shareOfPopulationWithThisPickup != null)
				numberPossibleJobsPickup = (int) Math
					.round(shareOfPopulationWithThisPickup * possiblePersonsPickup.size());
			if (shareOfPopulationWithThisDelivery != null)
				numberPossibleJobsDelivery = (int) Math
					.round(shareOfPopulationWithThisDelivery * possiblePersonsDelivery.size());

			int sampledNumberPossibleJobsPickup = (int) Math.round((sampleTo / sampleSizeInputPopulation) * numberPossibleJobsPickup);
			int sampledNumberPossibleJobsDelivery = (int) Math.round((sampleTo / sampleSizeInputPopulation) * numberPossibleJobsDelivery);
			if (numberPossibleJobsPickup > numberPossibleJobsDelivery) {
				if (sampleSizeInputPopulation == sampleTo ||samplingOption.equals("changeDemandOnLocation")) {
					numberOfJobs = numberPossibleJobsPickup;
				} else if (samplingOption.equals("changeNumberOfLocationsWithDemand")) {
					numberOfJobs = sampledNumberPossibleJobsPickup;
					numberPossibleJobsPickup = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsDelivery = sampledNumberPossibleJobsDelivery;
				} else
					throw new RuntimeException(
						"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			} else {
				if (sampleSizeInputPopulation == sampleTo ||samplingOption.equals("changeDemandOnLocation")) {
					numberOfJobs = numberPossibleJobsDelivery;
				} else if (samplingOption.equals("changeNumberOfLocationsWithDemand")) {
					numberOfJobs = sampledNumberPossibleJobsDelivery;
					numberPossibleJobsDelivery = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsPickup = sampledNumberPossibleJobsPickup;
				} else
					throw new RuntimeException(
						"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			}

			if (numberPossibleJobsPickup != 0)
				numberOfPickupLocations = numberPossibleJobsPickup;
			if (numberPossibleJobsDelivery != 0)
				numberOfDeliveryLocations = numberPossibleJobsDelivery;

			log.info("Number of possible pickup locations for this job element of carrier {}: {}", newDemandInformationElement.getCarrierName(), numberOfPickupLocations);
			log.info("Number of possible delivery locations for this job element of carrier {}: {}", newDemandInformationElement.getCarrierName(), numberOfDeliveryLocations);
		}

		// find possible Links for delivery and pickup
		ArrayList<LinkPersonPair> possibleLinkPersonPairsPickup = findAllPossibleLinks(scenario, indexShape,
			crsTransformationNetworkAndShape, numberOfPickupLocations, areasForPickupLocations,
			setLocationsOfPickup, possiblePersonsPickup);
		log.info("Possible links or persons for pickup: {}", possibleLinkPersonPairsPickup.size());

		ArrayList<LinkPersonPair> possibleLinkPersonPairsDelivery = findAllPossibleLinks(scenario, indexShape,
			crsTransformationNetworkAndShape, numberOfDeliveryLocations, areasForDeliveryLocations,
			setLocationsOfDelivery, possiblePersonsDelivery);
		log.info("Possible links or persons for delivery: {}", possibleLinkPersonPairsDelivery.size());

		if (possibleLinkPersonPairsPickup.isEmpty())
			throw new RuntimeException(
				"Not enough possible links to distribute the pickups. Select an different shapefile or check the CRS of the shapefile and network.");
		if (possibleLinkPersonPairsDelivery.isEmpty())
			throw new RuntimeException(
				"Not enough possible links to distribute the deliveries. Select an different shapefile or check the CRS of the shapefile and network.");

		if (setLocationsOfPickup != null)
			for (String selectedLinkIdPickups : setLocationsOfPickup)
				if (possibleLinkPersonPairsPickup.stream().noneMatch(linkPersonPair -> linkPersonPair.getLink().getId().toString().equals(selectedLinkIdPickups)))
					throw new RuntimeException("The selected link " + selectedLinkIdPickups
						+ " for pickup is not part of the possible links for pickup. Please check!");

		if (setLocationsOfDelivery != null)
			if (numberOfDeliveryLocations < setLocationsOfDelivery.length)
				log.warn("You selected more certain locations than the set number of locations. Randomly selected locations will be used.");
			else
				for (String selectedLinkIdDelivery : setLocationsOfDelivery)
					if (possibleLinkPersonPairsDelivery.stream().noneMatch(linkPersonPair -> linkPersonPair.getLink().getId().toString().equals(selectedLinkIdDelivery)))
						throw new RuntimeException("The selected link " + selectedLinkIdDelivery
							+ " for delivery is not part of the possible links for delivery. Please check!");

		int demandToDistribute = demandGenerationSpecification.getDemandToDistribute(newDemandInformationElement, possiblePersonsPickup, possiblePersonsDelivery);

		// distribute the demand over the network because no number of jobs is selected
		if (numberOfJobs == null) {
			log.info("Creates shipments with a demand of 1 or proportional to link length.");
			// creates shipments with a demand of 1
			if (possibleLinkPersonPairsPickup.size() > demandToDistribute || Objects.requireNonNull(possibleLinkPersonPairsDelivery).size() > demandToDistribute) {
				for (int i = 0; i < demandToDistribute; i++) {
					LinkPersonPair pickupLinkPersonPair = findNextUsedLinkPersonPair(scenario, indexShape, possibleLinkPersonPairsPickup,
						numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup, usedPickupLocationsOrPersons,
						possiblePersonsPickup, nearestLinkPerPersonPickup, crsTransformationNetworkAndShape, i);
					LinkPersonPair deliveryLinkPersonPair = findNextUsedLinkPersonPair(scenario, indexShape, possibleLinkPersonPairsDelivery,
						numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
						usedDeliveryLocationsOrPersons, possiblePersonsDelivery, nearestLinkPerPersonDelivery,
						crsTransformationNetworkAndShape, i);

					int demandForThisLink = 1;
					if (!usedPickupLocationsOrPersons.contains(pickupLinkPersonPair))
						usedPickupLocationsOrPersons.add(pickupLinkPersonPair);
					if (!usedDeliveryLocationsOrPersons.contains(deliveryLinkPersonPair))
						usedDeliveryLocationsOrPersons.add(deliveryLinkPersonPair);

					createSingleShipment(scenario, newDemandInformationElement, pickupLinkPersonPair.getLink(), deliveryLinkPersonPair.getLink(), demandForThisLink,
						demandGenerationSpecification);
				}
			} else
			// creates a demand on each link, demand depends on the length of the link
			{
				if (numberOfPickupLocations != null && numberOfDeliveryLocations != null)
					throw new RuntimeException(
						"Because the demand is higher than the number of links, the demand will be distributed evenly over all links. You selected a certain number of pickup and delivery locations, which is not possible here!");
				ArrayList<LinkPersonPair> demandBasesLinks;
				double sumOfDemandBasedLinks;
				boolean pickupIsDemandBase = true;
				LinkPersonPair pickupLinkPersonPair = null;
				LinkPersonPair deliveryLinkPersonPair = null;
				double sumOfPossibleLinkLengthPickup = possibleLinkPersonPairsPickup.stream().mapToDouble(LinkPersonPair -> LinkPersonPair.getLink().getLength()).sum();
				double sumOfPossibleLinkLengthDelivery = possibleLinkPersonPairsDelivery.stream().mapToDouble(LinkPersonPair -> LinkPersonPair.getLink().getLength()).sum();
				if (numberOfPickupLocations == null && numberOfDeliveryLocations == null)
					if (possibleLinkPersonPairsPickup.size() > possibleLinkPersonPairsDelivery.size()) {
						demandBasesLinks = possibleLinkPersonPairsPickup;
						sumOfDemandBasedLinks = sumOfPossibleLinkLengthPickup;
					} else {
						demandBasesLinks = possibleLinkPersonPairsDelivery;
						sumOfDemandBasedLinks = sumOfPossibleLinkLengthDelivery;
						pickupIsDemandBase = false;
					}
				else if (numberOfPickupLocations != null) {
					demandBasesLinks = possibleLinkPersonPairsDelivery;
					sumOfDemandBasedLinks = sumOfPossibleLinkLengthDelivery;
					pickupIsDemandBase = false;
				} else {
					demandBasesLinks = possibleLinkPersonPairsPickup;
					sumOfDemandBasedLinks = sumOfPossibleLinkLengthPickup;
				}
				for (Link demandBasedLink : demandBasesLinks.stream().map(LinkPersonPair::getLink).toList()) {
					int demandForThisLink = demandGenerationSpecification.calculateDemandBasedOnLinkLength(countOfLinks, distributedDemand, demandToDistribute, demandBasesLinks.size(), sumOfDemandBasedLinks,
						demandBasedLink);
					if (pickupIsDemandBase) {
						pickupLinkPersonPair = new LinkPersonPair(demandBasedLink, null);
						deliveryLinkPersonPair = findNextLinkPersonPair(scenario, indexShape, crsTransformationNetworkAndShape,
							deliveryLinkPersonPair,
							usedDeliveryLocationsOrPersons, possibleLinkPersonPairsDelivery, numberOfDeliveryLocations, areasForDeliveryLocations,
							setLocationsOfDelivery,
							possiblePersonsDelivery, nearestLinkPerPersonDelivery, countOfLinks);
					} else {
						deliveryLinkPersonPair = new LinkPersonPair(demandBasedLink, null);
						pickupLinkPersonPair = findNextLinkPersonPair(scenario, indexShape, crsTransformationNetworkAndShape, pickupLinkPersonPair,
							usedPickupLocationsOrPersons,
							possibleLinkPersonPairsPickup, numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup,
							possiblePersonsPickup,
							nearestLinkPerPersonPickup, countOfLinks);
					}
					countOfLinks++;
					if (!usedPickupLocationsOrPersons.contains(pickupLinkPersonPair))
						usedPickupLocationsOrPersons.add(pickupLinkPersonPair);
					if (!usedDeliveryLocationsOrPersons.contains(deliveryLinkPersonPair))
						usedDeliveryLocationsOrPersons.add(deliveryLinkPersonPair);

					if (demandForThisLink > 0) {
						createSingleShipment(scenario, newDemandInformationElement, pickupLinkPersonPair.getLink(), deliveryLinkPersonPair.getLink(),
							demandForThisLink, demandGenerationSpecification);
					}
					distributedDemand = distributedDemand + demandForThisLink;
				}
			}
		} else { // if a certain number of shipments is selected

			if (demandToDistribute != 0 && demandToDistribute < numberOfJobs) {
				numberOfJobs = demandToDistribute;
				log.warn(
					"The resulting number of jobs is not feasible, because the demand is smaller then the number of jobs. Number of jobs is reduced to demand!");
				log.info("New number of jobs: {}", numberOfJobs);
			}

			for (int i = 0; i < numberOfJobs; i++) {

				LinkPersonPair pickupLinkPersonPair = findNextUsedLinkPersonPair(scenario, indexShape, possibleLinkPersonPairsPickup,
					numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup, usedPickupLocationsOrPersons,
					possiblePersonsPickup, nearestLinkPerPersonPickup, crsTransformationNetworkAndShape, i);

				LinkPersonPair deliveryLinkPersonPair = findNextUsedLinkPersonPair(scenario, indexShape, possibleLinkPersonPairsDelivery,
					numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
					usedDeliveryLocationsOrPersons, possiblePersonsDelivery, nearestLinkPerPersonDelivery,
					crsTransformationNetworkAndShape, i);

				int demandForThisLink = demandGenerationSpecification.calculateDemandForThisLinkWithFixNumberOfJobs(demandToDistribute, numberOfJobs, distributedDemand, pickupLinkPersonPair, deliveryLinkPersonPair, i);

				if (demandToDistribute == 0 || demandForThisLink > 0) {
					if (!usedPickupLocationsOrPersons.contains(pickupLinkPersonPair))
						usedPickupLocationsOrPersons.add(pickupLinkPersonPair);
					if (!usedDeliveryLocationsOrPersons.contains(deliveryLinkPersonPair))
						usedDeliveryLocationsOrPersons.add(deliveryLinkPersonPair);
					createSingleShipment(scenario, newDemandInformationElement, pickupLinkPersonPair.getLink(), deliveryLinkPersonPair.getLink(),
						demandForThisLink, demandGenerationSpecification);
					distributedDemand = distributedDemand + demandForThisLink;
				}
				// This could mean that the demand is not distributed evenly which is assumed by the number of jobs calculations.
				// So if we have a distributed demand but this single demand is 0, we have to not count this as a job
				else if (distributedDemand < demandToDistribute)
					i--;
			}
		}

//		//NEW: if more possible persons than demand -> add to parcelsPerPerson
//		if (!possiblePersonsDelivery.isEmpty() && !Objects.equals(selectedDemandDistributionOption, "noSelection")) {
//			for (Id<Person> person : possiblePersonsDelivery.keySet()) {
//				parcelsPerPerson.put(person, new HashMap<>());
//				int age = (int) population.getPersons().get(person).getAttributes().getAttribute("age");
//				parcelsPerPerson.get(person).put(age, "0 \t"+getHomeCoord(population.getPersons().get(person)).getX()+"\t"+getHomeCoord(population.getPersons().get(person)).getY());
//			}
//		} //TODO check if this is necessary
	}

	/**
	 * Finds the next used link for a job element.
	 *
	 * @param scenario                         Scenario
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param LinkPersonPair                   LinkPersonPair for selection
	 * @param usedLocationsOrPersons           Already used locations or persons
	 * @param possibleLinkPersonPairs          Possible LinkPersonPairs
	 * @param numberOfLocations                Number of locations
	 * @param areasForLocations                Areas for locations
	 * @param setLocations                     Set locations
	 * @param possiblePersons                  Possible persons
	 * @param nearestLinkPerPerson             Nearest link per person
	 * @param countOfLinks                     Count of links
	 * @return the next LinkPersonPair
	 */
	private static LinkPersonPair findNextLinkPersonPair(Scenario scenario, ShpOptions.Index indexShape,
														 CoordinateTransformation crsTransformationNetworkAndShape,
														 LinkPersonPair LinkPersonPair,
														 ArrayList<LinkPersonPair> usedLocationsOrPersons,
														 ArrayList<LinkPersonPair> possibleLinkPersonPairs, Integer numberOfLocations,
														 String[] areasForLocations, String[] setLocations,
														 HashMap<Id<Person>, Person> possiblePersons,
														 HashMap<Id<Person>, TreeMap <Double, String>> nearestLinkPerPerson, int countOfLinks) {
		while (LinkPersonPair == null || usedLocationsOrPersons.contains(LinkPersonPair)) {
			LinkPersonPair = findNextUsedLinkPersonPair(scenario, indexShape, possibleLinkPersonPairs,
				numberOfLocations, areasForLocations, setLocations,
				usedLocationsOrPersons, possiblePersons, nearestLinkPerPerson,
				crsTransformationNetworkAndShape, countOfLinks - 1);
			if (usedLocationsOrPersons.size() == possibleLinkPersonPairs.size()
				|| (numberOfLocations != null
				&& usedLocationsOrPersons.size() == numberOfLocations))
				break;
		}
		return LinkPersonPair;
	}

	/**
	 * Creates a single shipment.
	 *
	 * @param scenario                    Scenario
	 * @param newDemandInformationElement single DemandInformationElement
	 * @param linkPickup                  Link for the pickup
	 * @param linkDelivery                Link for the delivery
	 * @param demandForThisLink           Demand for this link
	 * @param demandGenerationSpecification			Calculator for the job duration
	 */
	private static void createSingleShipment(Scenario scenario, DemandInformationElement newDemandInformationElement,
											 Link linkPickup, Link linkDelivery, int demandForThisLink, DemandGenerationSpecification demandGenerationSpecification) {

		Carrier thisCarrier = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create(newDemandInformationElement.getCarrierName(), Carrier.class));
		double largestPossibleDemandSize = getLargestVehicleCapacity(thisCarrier);
		int handledDemand = 0;
		TimeWindow timeWindowPickup = newDemandInformationElement.getFirstJobElementTimeWindow();
		TimeWindow timeWindowDelivery = newDemandInformationElement.getSecondJobElementTimeWindow();

		while (handledDemand < demandForThisLink || demandForThisLink == 0) {
			Id<CarrierShipment> idNewShipment = Id.create(createJobId(scenario, newDemandInformationElement,
				linkPickup.getId(), linkDelivery.getId()), CarrierShipment.class);
			int singleDemandForThisLink;
			if (demandForThisLink - handledDemand < largestPossibleDemandSize)
				singleDemandForThisLink = demandForThisLink - handledDemand;
			else
				singleDemandForThisLink = (int)largestPossibleDemandSize;
			handledDemand = handledDemand + singleDemandForThisLink;
			double serviceTimePickup = demandGenerationSpecification.calculatePickupDuration(newDemandInformationElement.getFirstJobElementTimePerUnit(), singleDemandForThisLink);
			double serviceTimeDelivery = demandGenerationSpecification.calculateDeliveryDuration(newDemandInformationElement.getSecondJobElementTimePerUnit(), singleDemandForThisLink);

			CarrierShipment thisShipment = CarrierShipment.Builder
				.newInstance(idNewShipment, linkPickup.getId(), linkDelivery.getId(), singleDemandForThisLink)
				.setPickupDuration(serviceTimePickup).setPickupStartingTimeWindow(timeWindowPickup)
				.setDeliveryDuration(serviceTimeDelivery).setDeliveryStartingTimeWindow(timeWindowDelivery)
				.build();
			thisCarrier.getShipments().put(thisShipment.getId(), thisShipment);
			if (demandForThisLink == 0)
				break;
		}
	}

	/**
	 * Method calculates the number of jobs for a demand on one link based on the largest vehicle capacity of the carrier.
	 *
	 * @param thisCarrier       the carrier of a job
	 * @return Number of jobs for this demand
	 */
	private static double getLargestVehicleCapacity(Carrier thisCarrier) {
		double largestVehicleCapacity = 0;
		for (CarrierVehicle vehicle :
			thisCarrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			if (vehicle.getType().getCapacity().getOther() > largestVehicleCapacity) {
				largestVehicleCapacity = vehicle.getType().getCapacity().getOther();
			}
		}
		return largestVehicleCapacity;
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

	/**
	 * If jobs of a carrier have the same characteristics (time window, location),
	 * they will be combined to one job.
	 *
	 * @param scenario Scenario
	 */
	private static void combineSimilarJobs(Scenario scenario) {
		log.warn(
			"The number of Jobs will be reduced if jobs have the same characteristics (e.g. time, location, carrier)");
		for (Carrier thisCarrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			double largestPossibleDemandSize = getLargestVehicleCapacity(thisCarrier);

			if (!thisCarrier.getShipments().isEmpty()) {
				int shipmentsBeforeConnection = thisCarrier.getShipments().size();
				HashMap<Id<CarrierShipment>, CarrierShipment> shipmentsToRemove = new HashMap<>();
				ArrayList<CarrierShipment> shipmentsToAdd = new ArrayList<>();
				for (Id<CarrierShipment> baseShipmentId : thisCarrier.getShipments().keySet()) {
					if (!shipmentsToRemove.containsKey(baseShipmentId)) {
						CarrierShipment baseShipment = thisCarrier.getShipments().get(baseShipmentId);
						HashMap<Id<CarrierShipment>, CarrierShipment> shipmentsToConnect = new HashMap<>();
						shipmentsToConnect.put(baseShipmentId, baseShipment);
						for (Id<CarrierShipment> thisShipmentId : thisCarrier.getShipments().keySet()) {
							if (!shipmentsToRemove.containsKey(thisShipmentId)) {
								CarrierShipment thisShipment = thisCarrier.getShipments().get(thisShipmentId);
								if (baseShipment.getId() != thisShipment.getId()
									&& baseShipment.getPickupLinkId() == thisShipment.getPickupLinkId()
									&& baseShipment.getDeliveryLinkId() == thisShipment.getDeliveryLinkId()) {
									if (baseShipment.getPickupStartingTimeWindow() == thisShipment.getPickupStartingTimeWindow()) {
										if (baseShipment.getDeliveryStartingTimeWindow() == thisShipment.getDeliveryStartingTimeWindow())
											shipmentsToConnect.put(thisShipmentId, thisShipment);
									}
								}
							}
						}

						int demandForThisLink = 0;
						double serviceTimePickup = 0;
						double serviceTimeDelivery = 0;
						int countForThisShipment = 0;

						for (CarrierShipment carrierShipment : shipmentsToConnect.values()) {
							countForThisShipment++;
							// checks if the demand is too high for the vehicle, if yes create a new shipment and reset the demand and durations
							if (demandForThisLink + carrierShipment.getCapacityDemand() > largestPossibleDemandSize) {
								log.info("Demand for link {} is too high for the vehicle. A new shipment will be created.", baseShipment.getPickupLinkId());
								Id<CarrierShipment> idNewShipment = Id.create(baseShipment.getId().toString() + "_" + shipmentsToAdd.size(), CarrierShipment.class);

								CarrierShipment newShipment = CarrierShipment.Builder
									.newInstance(idNewShipment, baseShipment.getPickupLinkId(), baseShipment.getDeliveryLinkId(), demandForThisLink)
									.setPickupDuration(serviceTimePickup)
									.setPickupStartingTimeWindow(baseShipment.getPickupStartingTimeWindow())
									.setDeliveryDuration(serviceTimeDelivery)
									.setDeliveryStartingTimeWindow(baseShipment.getDeliveryStartingTimeWindow()).build();
								shipmentsToAdd.add(newShipment);
								demandForThisLink = 0;
								serviceTimePickup = 0;
								serviceTimeDelivery = 0;
							}
							demandForThisLink = demandForThisLink + carrierShipment.getCapacityDemand();
							serviceTimePickup = serviceTimePickup + carrierShipment.getPickupDuration();
							serviceTimeDelivery = serviceTimeDelivery + carrierShipment.getDeliveryDuration();
							shipmentsToRemove.put(carrierShipment.getId(), carrierShipment);
							// if the last shipment is reached, create a new shipment with the remaining demand
							if (countForThisShipment == shipmentsToConnect.size()) {
								Id<CarrierShipment> idNewShipment = Id.create(baseShipment.getId().toString() + "_" + shipmentsToAdd.size(), CarrierShipment.class);

								CarrierShipment newShipment = CarrierShipment.Builder
									.newInstance(idNewShipment, baseShipment.getPickupLinkId(), baseShipment.getDeliveryLinkId(), demandForThisLink)
									.setPickupDuration(serviceTimePickup)
									.setPickupStartingTimeWindow(baseShipment.getPickupStartingTimeWindow())
									.setDeliveryDuration(serviceTimeDelivery)
									.setDeliveryStartingTimeWindow(baseShipment.getDeliveryStartingTimeWindow()).build();
								shipmentsToAdd.add(newShipment);
							}
						}
					}
				}
				for (CarrierShipment id : shipmentsToRemove.values())
					thisCarrier.getShipments().remove(id.getId(), id);

				for (CarrierShipment carrierShipment : shipmentsToAdd) {
					thisCarrier.getShipments().put(carrierShipment.getId(), carrierShipment);
				}
				if (shipmentsBeforeConnection - thisCarrier.getShipments().size() > 0)
					log.warn("Number of reduced shipments for carrier {}: {}", thisCarrier.getId().toString(),
						shipmentsBeforeConnection - thisCarrier.getShipments().size());
			}
			if (!thisCarrier.getServices().isEmpty()) {
				int servicesBeforeConnection = thisCarrier.getServices().size();
				HashMap<Id<CarrierService>, CarrierService> servicesToRemove = new HashMap<>();
				ArrayList<CarrierService> servicesToAdd = new ArrayList<>();
				for (Id<CarrierService> baseServiceId : thisCarrier.getServices().keySet()) {
					if (!servicesToRemove.containsKey(baseServiceId)) {
						CarrierService baseService = thisCarrier.getServices().get(baseServiceId);
						HashMap<Id<CarrierService>, CarrierService> servicesToConnect = new HashMap<>();
						servicesToConnect.put(baseServiceId, baseService);
						for (Id<CarrierService> thisServiceId : thisCarrier.getServices().keySet()) {
							if (!servicesToRemove.containsKey(thisServiceId)) {
								CarrierService thisService = thisCarrier.getServices().get(thisServiceId);
								if (baseService.getId() != thisService.getId()
									&& baseService.getServiceLinkId() == thisService.getServiceLinkId() && baseService
									.getServiceStaringTimeWindow() == thisService.getServiceStaringTimeWindow())
									servicesToConnect.put(thisServiceId, thisService);
							}
						}
						int demandForThisLink = 0;
						double serviceTimeService = 0;
						int countForThisService = 0;
						for (CarrierService carrierService : servicesToConnect.values()) {
							countForThisService++;
							// checks if the demand is too high for the vehicle, if yes create a new service and reset the demand and service time
							if (demandForThisLink + carrierService.getCapacityDemand() > largestPossibleDemandSize) {
								log.info("Demand for link {} is too high for the vehicle. A new shipment will be created.", baseService.getServiceLinkId());
								Id<CarrierService> idNewService = Id.create(baseService.getId().toString() + "_" + servicesToAdd.size(), CarrierService.class);

								CarrierService.Builder builder = CarrierService.Builder
									.newInstance(idNewService, baseService.getServiceLinkId(), demandForThisLink)
									.setServiceDuration(serviceTimeService);
								CarrierService newService = builder.setServiceStartingTimeWindow(baseService.getServiceStaringTimeWindow()).build();
								servicesToAdd.add(newService);
								demandForThisLink = 0;
								serviceTimeService = 0;
							}
							demandForThisLink = demandForThisLink + carrierService.getCapacityDemand();
							serviceTimeService = serviceTimeService + carrierService.getServiceDuration();
							servicesToRemove.put(carrierService.getId(), carrierService);
							// if the last service is reached, create a new service with the remaining demand
							if (countForThisService == servicesToConnect.size()) {
								Id<CarrierService> idNewService = Id.create(baseService.getId().toString() + "_" + servicesToAdd.size(), CarrierService.class);

								CarrierService.Builder builder = CarrierService.Builder
									.newInstance(idNewService, baseService.getServiceLinkId(), demandForThisLink)
									.setServiceDuration(serviceTimeService);
								CarrierService newService = builder.setServiceStartingTimeWindow(baseService.getServiceStaringTimeWindow()).build();
								servicesToAdd.add(newService);
							}
						}
					}
				}
				for (CarrierService id : servicesToRemove.values())
					thisCarrier.getServices().remove(id.getId(), id);
				for (CarrierService carrierService : servicesToAdd) {
					thisCarrier.getServices().put(carrierService.getId(), carrierService);
				}
				if (servicesBeforeConnection - thisCarrier.getServices().size() > 0)
					log.warn("Number of reduced services for carrier {}: {}", thisCarrier.getId().toString(),
						servicesBeforeConnection - thisCarrier.getServices().size());
			}
		}
	}

	/**
	 * Finds and returns all possible links for this job.
	 *
	 * @param scenario                         Scenario
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param numberOfLocations                Number of locations for this demand
	 * @param areasForLocations                Areas for the locations
	 * @param setLocations                     Selected locations
	 * @param possiblePersons                  Persons that are possible for this demand
	 * @return HashMap with all possible links
	 */
	private static ArrayList<LinkPersonPair> findAllPossibleLinks(Scenario scenario,
																  ShpOptions.Index indexShape, CoordinateTransformation crsTransformationNetworkAndShape,
																  Integer numberOfLocations, String[] areasForLocations, String[] setLocations,
																  HashMap<Id<Person>, Person> possiblePersons) {
		log.info("Finding possible links for the demand in the selected areas {}", Arrays.toString(areasForLocations));
		ArrayList<LinkPersonPair> possibleLinkPersonPairs = new ArrayList<>();

		if (numberOfLocations == null) {
			for (Link link : scenario.getNetwork().getLinks().values())
				if (!link.getId().toString().contains("pt") && (!link.getAttributes().getAsMap().containsKey(
					"type") || !link.getAttributes().getAsMap().get("type").toString().contains(
					"motorway")) && FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
					areasForLocations, crsTransformationNetworkAndShape)) {
					possibleLinkPersonPairs.add(new LinkPersonPair(link, null));
				}
		} else {
			LinkPersonPair newPossibleLinkPersonPair;
			while (possibleLinkPersonPairs.size() < numberOfLocations) {
				newPossibleLinkPersonPair = findPossibleLinkPersonPairForDemand(possibleLinkPersonPairs, possiblePersons,
					indexShape, areasForLocations, numberOfLocations, scenario, setLocations,
					crsTransformationNetworkAndShape);
				if (!possibleLinkPersonPairs.contains(newPossibleLinkPersonPair))
					possibleLinkPersonPairs.add(newPossibleLinkPersonPair);
				if (!possiblePersons.isEmpty() && possibleLinkPersonPairs.size() == possiblePersons.size())
						break;
			}
		}
		return possibleLinkPersonPairs;
	}

	/**
	 * Finds the next link or person which can be used as a location.
	 * If persons for locations are selected, the nearest link for each person will be used and added to the return LinkPersonPair.
	 * If no persons are selected, only a possible link will be returned.
	 * If the maximum number of locations is reached, a random location of the already used locations will be returned, if not a new LinkPersonPair will be found.
	 *
	 * @param scenario                         Scenario
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param possibleLinkPersonPairs          All possible links
	 * @param selectedNumberOfLocations        Number of locations for this demand
	 * @param areasForLocations                Areas for the locations
	 * @param selectedLocations                Selected locations
	 * @param usedLocationsOrPersons           Already used locations or persons for this demand
	 * @param possiblePersons                  Persons that are possible for this demand
	 * @param nearestLinkPerPerson             Nearest link for each person
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param i                                Counter for the number of locations
	 * @return Next link for the demand
	 */
	private static LinkPersonPair findNextUsedLinkPersonPair(Scenario scenario, ShpOptions.Index indexShape,
															 ArrayList<LinkPersonPair> possibleLinkPersonPairs, Integer selectedNumberOfLocations, String[] areasForLocations,
															 String[] selectedLocations, ArrayList<LinkPersonPair> usedLocationsOrPersons, HashMap<Id<Person>, Person> possiblePersons,
															 HashMap<Id<Person>, TreeMap <Double, String>> nearestLinkPerPerson,
															 CoordinateTransformation crsTransformationNetworkAndShape, int i) {
		LinkPersonPair linkPersonPair = null;
		if (selectedNumberOfLocations == null || usedLocationsOrPersons.size() < selectedNumberOfLocations) {
			if (selectedLocations != null && selectedLocations.length > i) {
				linkPersonPair = new LinkPersonPair(scenario.getNetwork().getLinks().get(Id.createLinkId(selectedLocations[i])), null);
			} else
				while (linkPersonPair == null || (possibleLinkPersonPairs.size() > usedLocationsOrPersons.size()
					&& usedLocationsOrPersons.contains(linkPersonPair))) {
					linkPersonPair = findPossibleLinkPersonPairForDemand(possibleLinkPersonPairs, possiblePersons,
						indexShape, areasForLocations, selectedNumberOfLocations, scenario, selectedLocations,
						crsTransformationNetworkAndShape);
					if (linkPersonPair.getPerson() != null) {
						// the link finding for the persons, because this was not done before
						if (!nearestLinkPerPerson.containsKey(linkPersonPair.getPerson().getId()))
							findLinksForPerson(scenario, nearestLinkPerPerson, linkPersonPair.getPerson());
						for (String linkId : nearestLinkPerPerson.get(linkPersonPair.getPerson().getId()).values()) {
							Link linkForPerson = scenario.getNetwork().getLinks().get(Id.createLinkId(linkId));
							if (checkLinkAttributesForDemand(linkForPerson)) {
								linkPersonPair.setLink(linkForPerson);
								break;
							}
						}
						if (linkPersonPair.getLink() == null) {
							linkPersonPair = null;
						}
					}
				}
		} else {
			linkPersonPair = usedLocationsOrPersons.get(rand.nextInt(usedLocationsOrPersons.size()));
		}
		return linkPersonPair;
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
		log.info("Finding possible persons for the demand in the selected areas {}", Arrays.toString(areasForJobElementLocations));
		HashMap<Id<Person>, Person> possiblePersons = new HashMap<>();

		for (Person person : population.getPersons().values()) {
			Coord coord = getHomeCoord(person);
			if (crsTransformationNetworkAndShape != null)
				coord = crsTransformationNetworkAndShape.transform(coord);

			if (FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape,
				areasForJobElementLocations, crsTransformationNetworkAndShape))
				possiblePersons.put(person.getId(), person);
		}
		log.info("Number of possible persons for the demand: {}", possiblePersons.size());
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
								   HashMap<Id<Person>, TreeMap <Double, String>> nearestLinkPerPerson, Person person) {
		Coord homePoint = getHomeCoord(person);
		Link nearestLink = NetworkUtils.getNearestLinkExactly(scenario.getNetwork(), homePoint);
		// if the nearest link is not feasible, the next feasible link will be searched
		if (checkLinkAttributesForDemand(nearestLink)) {
			nearestLinkPerPerson.computeIfAbsent(person.getId(), k -> new TreeMap <>())
				.put(NetworkUtils.getEuclideanDistance(homePoint, nearestLink.getCoord()), nearestLink.getId().toString());
			return;
		}
		for (Link link : scenario.getNetwork().getLinks().values())
			if (checkLinkAttributesForDemand(link)) {
				Coord middlePointLink = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(link);
				double distance = NetworkUtils.getEuclideanDistance(homePoint, middlePointLink);
				if (!nearestLinkPerPerson.containsKey(person.getId())
					|| distance < nearestLinkPerPerson.get(person.getId()).keySet().iterator().next()) {
					nearestLinkPerPerson.computeIfAbsent(person.getId(), k -> new TreeMap <>())
						.put(distance, link.getId().toString());
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
	static Coord getHomeCoord(Person person) {
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
	 * Searches a possible LinkPersonPair for the demand.
	 *
	 * @param possibleLinkPersonPairs          HashMap with all possible links
	 * @param possiblePersons                  HashMap with all possible persons
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param areasForTheDemand                Areas for the demand
	 * @param selectedNumberOfLocations        Number of locations for this demand
	 * @param scenario                         Scenario
	 * @param selectedLocations                Selected locations
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @return The selected link for the demand
	 */
	private static LinkPersonPair findPossibleLinkPersonPairForDemand(ArrayList<LinkPersonPair> possibleLinkPersonPairs,
																	  HashMap<Id<Person>, Person> possiblePersons,
																	  ShpOptions.Index indexShape, String[] areasForTheDemand,
																	  Integer selectedNumberOfLocations,
																	  Scenario scenario, String[] selectedLocations,
																	  CoordinateTransformation crsTransformationNetworkAndShape) {
		LinkPersonPair linkPersonPair = null;
		LinkPersonPair newLinkPersonPair;

		if (selectedNumberOfLocations == null)
			selectedNumberOfLocations = 0;
		while (linkPersonPair == null) {
			if (possibleLinkPersonPairs.size() < selectedNumberOfLocations) {
				if (selectedLocations != null && selectedLocations.length > possibleLinkPersonPairs.size()) {
					Link newLink = scenario.getNetwork().getLinks()
						.get(Id.createLinkId(selectedLocations[possibleLinkPersonPairs.size()]));
					linkPersonPair = new LinkPersonPair(newLink, null);
					break;
				} else {
					if (possiblePersons.isEmpty()) {
						Link newLink = scenario.getNetwork().getLinks().values().stream()
							.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
						newLinkPersonPair = new LinkPersonPair(newLink, null);
					}
					else {
						newLinkPersonPair = new LinkPersonPair(null, possiblePersons.values().stream().skip(rand.nextInt(possiblePersons.size()))
							.findFirst().get());
					}
				}
			} else {
					newLinkPersonPair = possibleLinkPersonPairs.stream().skip(rand.nextInt(possibleLinkPersonPairs.size())).findFirst().get();
			}
			// check if the selected link is possible for the demand
			if (newLinkPersonPair.getLink() != null && checkLinkFeasibility(indexShape, areasForTheDemand, crsTransformationNetworkAndShape, newLinkPersonPair.getLink())) {
				linkPersonPair = newLinkPersonPair;
			}
			// the check of the related link for a person will be done later when a person is selected for a demand
			else if (newLinkPersonPair.getPerson() != null)
				linkPersonPair = newLinkPersonPair;
		}

		return linkPersonPair;
	}

	/**
	 * Checks if the link is feasible for the demand.
	 *
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param areasForTheDemand                Areas for the demand
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape file
	 * @param newLink                          new Link for the demand
	 * @return True if the link is feasible for the demand, false if not
	 */
	private static boolean checkLinkFeasibility(ShpOptions.Index indexShape, String[] areasForTheDemand,
												CoordinateTransformation crsTransformationNetworkAndShape, Link newLink) {
		return checkLinkAttributesForDemand(newLink)
			&& (indexShape == null || FreightDemandGenerationUtils.checkPositionInShape(newLink, null,
			indexShape, areasForTheDemand, crsTransformationNetworkAndShape));
	}

	/**
	 * Checks if a link is suitable for the demand.
	 *
	 * @param linkForPerson Link for the person
	 * @return True if the link is suitable for the demand, false if not
	 */
	private static boolean checkLinkAttributesForDemand(Link linkForPerson) {
		return !linkForPerson.getId().toString().contains("pt") && (!linkForPerson.getAttributes().getAsMap().containsKey("type")
			|| !linkForPerson.getAttributes().getAsMap().get("type").toString().contains("motorway"));
	}
}
