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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.options.ShpOptions;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This CarrierReaderFromCSV reads all carrier information given in the read CSV
 * file and creates the carriers. While the process of creating the carriers, the
 * consistency of the information will be checked.
 *
 * @author Ricardo Ewert
 *
 */
public final class CarrierReaderFromCSV {
	private static final Logger log = LogManager.getLogger(CarrierReaderFromCSV.class);

	/**
	 * CarrierInformationElement is a set of information being read from the input
	 * file. For one carrier several CarrierInformationElement can be read in. This
	 * is necessary for creating different configurations of the vehicles. Not every
	 * parameter should be set for creating the carrier. While the process of
	 * creating the carriers, the consistency of the information will be checked.
	 */
	static class CarrierInformationElement {
		/**
		 * Name of the carrier of this information element.
		 */
		private final String carrierName;
		/**
		 * VehicleTypes which can be used by this carrier
		 */
		private final String[] vehicleTypes;
		/**
		 * Number of depots which should be created for each vehicleType
		 */
		private final int numberOfDepotsPerType;
		/**
		 * Set locations for vehicle depots. This should be the linkIds as a String[].
		 */
		private List<String> vehicleDepots;
		/**
		 * Sets the area where the created depots should be located. Therefore, a shape
		 * input is necessary.
		 */
		private final String[] areaOfAdditionalDepots;
		/**
		 * Sets the fleetsize of this carrier. Options: finite or infinite
		 */
		private final FleetSize fleetSize;
		/**
		 * Sets the start time of the vehicles
		 */
		private final int vehicleStartTime;
		/**
		 * Sets the end time of the vehicles
		 */
		private final int vehicleEndTime;
		/**
		 * Sets the number of jsprit iterations for this carrier
		 */
		private final int jspritIterations;
		/**
		 * Sets a fixed number of vehicles per vehicleType and location. If this
		 * number is e.g., 3.: for each vehicleType 3 vehicles at each location will be
		 * created, and the fleetsize is finite.
		 */
		private int fixedNumberOfVehiclePerTypeAndLocation;

		public static class Builder {
			private final String carrierName;
			private String[] vehicleTypes = null;
			private int numberOfDepotsPerType = 0;
			private List<String> vehicleDepots = null;
			private String[] areaOfAdditionalDepots = null;
			private FleetSize fleetSize = null;
			private int vehicleStartTime = 0;
			private int vehicleEndTime = 0;
			private int jspritIterations = 0;
			private int fixedNumberOfVehiclePerTypeAndLocation = 0;

			public static Builder newInstance(String carrierName) {
				return new Builder(carrierName);
			}

			private Builder(String carrierName) {
				super();
				this.carrierName = carrierName;
			}

			public String getCarrierName() {
				return carrierName;
			}

			public void setVehicleTypes(String[] vehicleTypes) {
				this.vehicleTypes = vehicleTypes;
			}

			public void setNumberOfDepotsPerType(int numberOfDepotsPerType) {
				this.numberOfDepotsPerType = numberOfDepotsPerType;
			}

			public void setVehicleDepots(List<String> vehicleDepots) {
				this.vehicleDepots = vehicleDepots;
			}

			public void setAreaOfAdditionalDepots(String[] areaOfAdditionalDepots) {
				this.areaOfAdditionalDepots = areaOfAdditionalDepots;
			}

			public void setFleetSize(FleetSize fleetSize) {
				this.fleetSize = fleetSize;
			}

			public void setVehicleStartTime(int carrierStartTime) {
				this.vehicleStartTime = carrierStartTime;
			}

			public void setVehicleEndTime(int vehicleEndTime) {
				this.vehicleEndTime = vehicleEndTime;
			}

			public void setJspritIterations(int jspritIterations) {
				this.jspritIterations = jspritIterations;
			}

			public void setFixedNumberOfVehiclePerTypeAndLocation(int fixedNumberOfVehiclePerTypeAndLocation) {
				this.fixedNumberOfVehiclePerTypeAndLocation = fixedNumberOfVehiclePerTypeAndLocation;
			}

			public CarrierInformationElement build() {
				return new CarrierInformationElement(this);
			}
		}

		private CarrierInformationElement(Builder builder) {
			carrierName = builder.carrierName;
			vehicleTypes = builder.vehicleTypes;
			numberOfDepotsPerType = builder.numberOfDepotsPerType;
			vehicleDepots = builder.vehicleDepots;
			areaOfAdditionalDepots = builder.areaOfAdditionalDepots;
			fleetSize = builder.fleetSize;
			vehicleStartTime = builder.vehicleStartTime;
			vehicleEndTime = builder.vehicleEndTime;
			jspritIterations = builder.jspritIterations;
			fixedNumberOfVehiclePerTypeAndLocation = builder.fixedNumberOfVehiclePerTypeAndLocation;
		}

		public String getName() {
			return carrierName;
		}

		public String[] getVehicleTypes() {
			return vehicleTypes;
		}

		public int getNumberOfDepotsPerType() {
			return numberOfDepotsPerType;
		}

		public List<String> getVehicleDepots() {
			return vehicleDepots;
		}

		public void setVehicleDepots(List<String> vehicleDepots) {
			this.vehicleDepots = vehicleDepots;
		}

		public String[] getAreaOfAdditionalDepots() {
			return areaOfAdditionalDepots;
		}

		public FleetSize getFleetSize() {
			return fleetSize;
		}

		public int getVehicleStartTime() {
			return vehicleStartTime;
		}

		public int getVehicleEndTime() {
			return vehicleEndTime;
		}

		public int getJspritIterations() {
			return jspritIterations;
		}

		public int getFixedNumberOfVehiclePerTypeAndLocation() {
			return fixedNumberOfVehiclePerTypeAndLocation;
		}

		public void setFixedNumberOfVehiclePerTypeAndLocation(int fixedNumberOfVehiclePerTypeAndLocation) {
			this.fixedNumberOfVehiclePerTypeAndLocation = fixedNumberOfVehiclePerTypeAndLocation;
		}
	}

	/**
	 * Reads and create the carriers with reading the information from the csv file.
	 *
	 * @param scenario                         Scenario
	 * @param freightCarriersConfigGroup       FreightCarriersConfigGroup
	 * @param csvLocationCarrier               Path to the csv file with the carrier information
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param defaultJspritIterations          Default number of jsprit iterations
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape
	 * @param shapeCategory                    Column name in the shape file for the data connection in the csv files
	 * @throws IOException IOException
	 */
	public static void readAndCreateCarrierFromCSV(Scenario scenario, FreightCarriersConfigGroup freightCarriersConfigGroup,
												   Path csvLocationCarrier, ShpOptions.Index indexShape, int defaultJspritIterations,
												   CoordinateTransformation crsTransformationNetworkAndShape, String shapeCategory) throws IOException {

		Set<CarrierInformationElement> allNewCarrierInformation = readCarrierInformation(csvLocationCarrier);
		checkNewCarrier(allNewCarrierInformation, freightCarriersConfigGroup, scenario, indexShape, shapeCategory);
		log.info("The read carrier information from the csv are checked without errors.");
		createNewCarrierAndAddVehicleTypes(scenario, allNewCarrierInformation, freightCarriersConfigGroup, indexShape,
			defaultJspritIterations, crsTransformationNetworkAndShape);
	}

	/**
	 * @param csvLocationCarrier Path to the csv file with the carrier information
	 * @return Set<CarrierInformationElement> Set of CarrierInformationElements
	 * @throws IOException IOException
	 */
	static Set<CarrierInformationElement> readCarrierInformation(Path csvLocationCarrier) throws IOException {
		log.info("Start reading carrier csv file: {}", csvLocationCarrier);
		Set<CarrierInformationElement> allNewCarrierInformation = new HashSet<>();
		CSVParser parse = new CSVParser(Files.newBufferedReader(csvLocationCarrier),
			CSVFormat.Builder.create(CSVFormat.TDF).setHeader().setSkipHeaderRecord(true).build());
		for (CSVRecord record : parse) {
			CarrierInformationElement.Builder builder;
			if (!record.get("carrierName").isBlank())
				builder = CarrierInformationElement.Builder.newInstance(record.get("carrierName"));
			else
				throw new RuntimeException(
					"Minimum one carrier has no name. Every carrier information has to be related to one carrier. Please check the input csv file!");
			if (!record.get("vehicleTypes").isBlank())
				builder.setVehicleTypes(record.get("vehicleTypes").split(";"));
			if (!record.get("numberOfDepots").isBlank())
				builder.setNumberOfDepotsPerType(Integer.parseInt(record.get("numberOfDepots")));
			if (!record.get("selectedVehicleDepots").isBlank())
				builder.setVehicleDepots(
					new ArrayList<>(Arrays.asList(record.get("selectedVehicleDepots").split(";"))));
			if (!record.get("areaOfAdditionalDepots").isBlank())
				builder.setAreaOfAdditionalDepots(record.get("areaOfAdditionalDepots").split(";"));
			if (!record.get("fixedNumberOfVehiclePerTypeAndLocation").isBlank())
				builder.setFixedNumberOfVehiclePerTypeAndLocation(
					Integer.parseInt(record.get("fixedNumberOfVehiclePerTypeAndLocation")));
			if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("infinite"))
				builder.setFleetSize(FleetSize.INFINITE);
			else if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("finite"))
				builder.setFleetSize(FleetSize.FINITE);
			else if (!record.get("fleetSize").isBlank())
				throw new RuntimeException("Select a valid FleetSize for the carrier: " + builder.getCarrierName()
					+ ". Possible is finite or infinite!!");
			if (!record.get("vehicleStartTime").isBlank())
				builder.setVehicleStartTime(Integer.parseInt(record.get("vehicleStartTime")));
			if (!record.get("vehicleEndTime").isBlank())
				builder.setVehicleEndTime(Integer.parseInt(record.get("vehicleEndTime")));
			if (!record.get("jspritIterations").isBlank())
				builder.setJspritIterations(Integer.parseInt(record.get("jspritIterations")));
			CarrierInformationElement newCarrierInformationElement = builder.build();
			allNewCarrierInformation.add(newCarrierInformationElement);
		}
		return allNewCarrierInformation;
	}

	/**
	 * Checks if the read carrier information is consistent.
	 *
	 * @param allNewCarrierInformation   Set of CarrierInformationElements
	 * @param freightCarriersConfigGroup FreightCarriersConfigGroup
	 * @param scenario                   Scenario
	 * @param indexShape                 ShpOptions.Index for the shape file
	 * @param shapeCategory              Column name in the shape file for the data connection in the csv files
	 */
	static void checkNewCarrier(Set<CarrierInformationElement> allNewCarrierInformation,
								FreightCarriersConfigGroup freightCarriersConfigGroup, Scenario scenario, ShpOptions.Index indexShape, String shapeCategory) {

		CarriersUtils.addOrGetCarriers(scenario);
		for (CarrierInformationElement carrierElement : allNewCarrierInformation) {
			if (CarriersUtils.getCarriers(scenario).getCarriers()
				.containsKey(Id.create(carrierElement.getName(), Carrier.class)))
				throw new RuntimeException("The Carrier " + carrierElement.getName()
					+ " being loaded from the csv is already in the given Carrier file. It is not possible to add to an existing Carrier. Please check!");
			CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
			new CarrierVehicleTypeReader(carrierVehicleTypes)
				.readFile(freightCarriersConfigGroup.getCarriersVehicleTypesFile());
			if (carrierElement.getVehicleTypes() != null)
				for (String type : carrierElement.getVehicleTypes()) {
					if (!carrierVehicleTypes.getVehicleTypes().containsKey(Id.create(type, VehicleType.class)))
						throw new RuntimeException("The selected vehicleType " + type + " of the carrier "
							+ carrierElement.getName()
							+ " in the input file is not part of imported vehicle types. Please change the type or add the type in the vehicleTypes input file!");
				}
			if (carrierElement.getVehicleDepots() != null) {
				if (carrierElement.getNumberOfDepotsPerType() < carrierElement.getVehicleDepots().size())
					throw new RuntimeException("For the carrier " + carrierElement.getName()
						+ " more certain depots than the given number of depots are selected. (numberOfDepots < selectedVehicleDepots)");

				for (String linkDepot : carrierElement.getVehicleDepots()) {
					if (!scenario.getNetwork().getLinks().containsKey(Id.create(linkDepot, Link.class)))
						throw new RuntimeException("The selected link " + linkDepot + " for a depot of the carrier "
							+ carrierElement.getName() + " is not part of the network. Please check!");
				}
			}
			if (carrierElement.getVehicleTypes() != null && carrierElement.getNumberOfDepotsPerType() == 0
				&& carrierElement.getVehicleDepots() == null)
				throw new RuntimeException(
					"If a vehicle type is selected in the input file, numberOfDepots or selectedVehicleDepots should be set. Please check carrier "
						+ carrierElement.getName());
			if ((carrierElement.getVehicleDepots() != null
				&& (carrierElement.getNumberOfDepotsPerType() > carrierElement.getVehicleDepots().size())
				&& carrierElement.getAreaOfAdditionalDepots() == null) || (carrierElement.getVehicleDepots() == null && (carrierElement.getNumberOfDepotsPerType() > 0)
				&& carrierElement.getAreaOfAdditionalDepots() == null))
				log.warn("No possible area for additional depot given. Random choice in the hole network of a possible position");
			if (carrierElement.getAreaOfAdditionalDepots() != null) {
				if (indexShape == null)
					throw new RuntimeException("For carrier " + carrierElement.getName()
						+ " a certain area for depots is selected, but no shape is read in. Please check.");
				for (String depotArea : carrierElement.getAreaOfAdditionalDepots()) {
					boolean isInShape = false;
					for (SimpleFeature singlePolygon : indexShape.getShp().readFeatures()) {
						if (singlePolygon.getAttribute(shapeCategory).equals(depotArea)) {
							isInShape = true;
							break;
						}
					}
					if (!isInShape)
						throw new RuntimeException("The area " + depotArea + " of the possible depots of carrier"
							+ carrierElement.getName() + " is not part of the given shapeFile. The areas should be in the shape file column " + shapeCategory);
				}
			}
			if (carrierElement.getFixedNumberOfVehiclePerTypeAndLocation() != 0)
				for (CarrierInformationElement existingCarrier : allNewCarrierInformation)
					if ((existingCarrier.getName().equals(carrierElement.getName())
						&& existingCarrier.getFleetSize() == FleetSize.INFINITE)
						|| carrierElement.getFleetSize() == FleetSize.INFINITE)
						throw new RuntimeException("For the carrier " + carrierElement.getName()
							+ " a infinite fleetSize configuration was set, although you want to set a fixed number of vehicles. Please check!");
			if (carrierElement.getFleetSize() != null)
				for (CarrierInformationElement existingCarrier : allNewCarrierInformation)
					if (existingCarrier.getName().equals(carrierElement.getName())
						&& existingCarrier.getFleetSize() != null
						&& existingCarrier.getFleetSize() != carrierElement.getFleetSize())
						throw new RuntimeException("For the carrier " + carrierElement.getName()
							+ " different fleetSize configuration was set. Please check and select only one!");
			if (carrierElement.getVehicleTypes() != null) {
				if (carrierElement.getVehicleStartTime() == 0 || carrierElement.getVehicleEndTime() == 0)
					throw new RuntimeException("For the vehicle types of the carrier " + carrierElement.getName()
						+ " no start and/or end time for the vehicles was selected. Please set both times!!");
				if (carrierElement.getVehicleStartTime() >= carrierElement.getVehicleEndTime())
					throw new RuntimeException("For the vehicle types of the carrier " + carrierElement.getName()
						+ " a startTime after the endTime for the vehicles was selected. Please check!");
			}
			if (carrierElement.getJspritIterations() != 0)
				for (CarrierInformationElement existingCarrier : allNewCarrierInformation)
					if (existingCarrier.getName().equals(carrierElement.getName())
						&& existingCarrier.getJspritIterations() != 0
						&& existingCarrier.getJspritIterations() != carrierElement.getJspritIterations())
						throw new RuntimeException("For the carrier " + carrierElement.getName()
							+ " different number of jsprit iterations are set. Please check!");
		}
	}

	/**
	 * Read and creates the carrier and the vehicle types.
	 *
	 * @param scenario                         Scenario
	 * @param allNewCarrierInformation         Set of CarrierInformationElements
	 * @param freightCarriersConfigGroup       FreightCarriersConfigGroup
	 * @param indexShape                       ShpOptions.Index for the shape file
	 * @param defaultJspritIterations          Default number of jsprit iterations
	 * @param crsTransformationNetworkAndShape CoordinateTransformation for the network and shape
	 */
	static void createNewCarrierAndAddVehicleTypes(Scenario scenario,
												   Set<CarrierInformationElement> allNewCarrierInformation, FreightCarriersConfigGroup freightCarriersConfigGroup,
												   ShpOptions.Index indexShape, int defaultJspritIterations,
												   CoordinateTransformation crsTransformationNetworkAndShape) {

		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypes usedCarrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);
		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(freightCarriersConfigGroup.getCarriersVehicleTypesFile());

		for (CarrierInformationElement singleNewCarrier : allNewCarrierInformation) {
			if (singleNewCarrier.getVehicleTypes() == null) {
				continue;
			}
			Carrier thisCarrier;
			CarrierCapabilities carrierCapabilities;
			if (carriers.getCarriers().containsKey(Id.create(singleNewCarrier.getName(), Carrier.class))) {
				thisCarrier = carriers.getCarriers().get(Id.create(singleNewCarrier.getName(), Carrier.class));
				carrierCapabilities = thisCarrier.getCarrierCapabilities();
				if (carrierCapabilities.getFleetSize() == null && singleNewCarrier.getFleetSize() != null)
					carrierCapabilities.setFleetSize(singleNewCarrier.getFleetSize());
				if (singleNewCarrier.getJspritIterations() > 0)
					CarriersUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
			} else {
				thisCarrier = CarriersUtils.createCarrier(Id.create(singleNewCarrier.getName(), Carrier.class));
				if (singleNewCarrier.getJspritIterations() > 0)
					CarriersUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
				carrierCapabilities = CarrierCapabilities.Builder.newInstance()
					.setFleetSize(singleNewCarrier.getFleetSize()).build();
				carriers.addCarrier(thisCarrier);
			}
			if (singleNewCarrier.getVehicleDepots() == null)
				singleNewCarrier.setVehicleDepots(new ArrayList<>());
			Random rand = new Random(singleNewCarrier.getName().hashCode());
			int cnt = 0;
			while (singleNewCarrier.getVehicleDepots().size() < singleNewCarrier.getNumberOfDepotsPerType()) {
				Link link = scenario.getNetwork().getLinks().values().stream()
					.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findAny().get();
				cnt++;
				if ((!singleNewCarrier.getVehicleDepots().contains(link.getId().toString())
					|| cnt > scenario.getNetwork().getLinks().size())
					&& !link.getId().toString().contains("pt")
					&& (!link.getAttributes().getAsMap().containsKey("type")
					|| !link.getAttributes().getAsMap().get("type").toString().contains("motorway"))
					&& FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
					singleNewCarrier.getAreaOfAdditionalDepots(), crsTransformationNetworkAndShape)) {
					singleNewCarrier.getVehicleDepots().add(link.getId().toString());
				}
			}
			for (String singleDepot : singleNewCarrier.getVehicleDepots()) {
				int countDepots = 2;
				for (String thisVehicleType : singleNewCarrier.getVehicleTypes()) {
					VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
						.get(Id.create(thisVehicleType, VehicleType.class));
					usedCarrierVehicleTypes.getVehicleTypes().putIfAbsent(Id.create(thisVehicleType, VehicleType.class),
						thisType);
					if (singleNewCarrier.getFixedNumberOfVehiclePerTypeAndLocation() == 0)
						singleNewCarrier.setFixedNumberOfVehiclePerTypeAndLocation(1);
					for (int i = 0; i < singleNewCarrier.getFixedNumberOfVehiclePerTypeAndLocation(); i++) {
						Id<Vehicle> vehilcelId = Id.create(
							thisType.getId().toString() + "_" + thisCarrier.getId().toString() + "_" + singleDepot
								+ "_start" + singleNewCarrier.getVehicleStartTime() + "_" + (i + 1),
							Vehicle.class);
						while (carrierCapabilities.getCarrierVehicles().containsKey(vehilcelId)) {
							vehilcelId = Id.create(thisType.getId().toString() + "_" + thisCarrier.getId().toString()
								+ "_" + singleDepot + "_V" + countDepots + "_start"
								+ singleNewCarrier.getVehicleStartTime() + "_" + (i + 1), Vehicle.class);
							countDepots++;
						}
						CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
							.newInstance(vehilcelId, Id.createLinkId(singleDepot), thisType)
							.setEarliestStart(singleNewCarrier.getVehicleStartTime())
							.setLatestEnd(singleNewCarrier.getVehicleEndTime()).build();
						carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
						if (!carrierCapabilities.getVehicleTypes().contains(thisType))
							carrierCapabilities.getVehicleTypes().add(thisType);
					}
				}
			}
			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
		for (Carrier carrier : carriers.getCarriers().values()) {
			if (CarriersUtils.getJspritIterations(carrier) == Integer.MIN_VALUE) {
				CarriersUtils.setJspritIterations(carrier, defaultJspritIterations);
				log.warn("The jspritIterations are now set to the default value of {} in this simulation!", defaultJspritIterations);
			}
		}
	}
}
