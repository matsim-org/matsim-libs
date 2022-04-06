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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.opengis.feature.simple.SimpleFeature;

/**
 * This CarrierReaderFromCSV reads all carrier information given in the read CSV
 * file and creates the carriers. While the process of creating the carriers the
 * consistency of the information will be checked.
 * 
 * @author Ricardo Ewert
 *
 */
public final class CarrierReaderFromCSV {
	private static final Logger log = Logger.getLogger(CarrierReaderFromCSV.class);

	/**
	 * CarrierInformationElement is a set of information being read from the input
	 * file. For one carrier several CarrierInformationElement can be read in. This
	 * is necessary for creating different configurations of the vehicles. Not every
	 * parameter should be set for creating the carrier. While the process of
	 * creating the carriers the consistency of the information will be checked.
	 */
	static class CarrierInformationElement {
		/**
		 * Name of carrier of this information element.
		 */
		private String carrierName;
		/**
		 * VehicleTypes which can be used by this carrier
		 */
		private String[] vehilceTypes;
		/**
		 * Number of depots which should be created for each vehicleType
		 */
		private int numberOfDepotsPerType;
		/**
		 * Set locations for vehicle depots. This should be the linkIds as a String[].
		 */
		private String[] vehicleDepots;
		/**
		 * Sets the area where the created depots should be located. Therefore a shape
		 * input is necessary.
		 */
		private String[] areaOfAdditonalDepots;
		/**
		 * Sets the fleetsize of this carrier. Options: finite or infinite
		 */
		private FleetSize fleetSize;
		/**
		 * Sets the start time of the vehicles
		 */
		private int vehicleStartTime;
		/**
		 * Sets the end time of the vehicles
		 */
		private int vehicleEndTime;
		/**
		 * Sets the number of jsprit iterations for this carrier
		 */
		private int jspritIterations;
		/**
		 * Sets a the fixed number of vehicles per vehicleType and location. If this
		 * number is e.g. 3.: for each vehicleType 3 vehicles at each location will be
		 * created and the fleetsize is finite.
		 */
		private int fixedNumberOfVehilcePerTypeAndLocation;

		/**
		 * Creates a new CarrierInformationElement.
		 * 
		 * @param carrierName
		 * @param vehilceTypes
		 * @param numberOfDepotsPerType
		 * @param vehicleDepots
		 * @param areaOfAdditonalDepots
		 * @param fleetSize
		 * @param vehicleStartTime
		 * @param vehicleEndTime
		 * @param jspritIterations
		 * @param fixedNumberOfVehilcePerTypeAndLocation
		 */
		public CarrierInformationElement(String carrierName, String[] vehilceTypes, int numberOfDepotsPerType,
				String[] vehicleDepots, String[] areaOfAdditonalDepots, FleetSize fleetSize, int vehicleStartTime,
				int vehicleEndTime, int jspritIterations, int fixedNumberOfVehilcePerTypeAndLocation) {
			this.setCarrierName(carrierName);
			this.setVehicleTypes(vehilceTypes);
			this.setNumberOfDepotsPerType(numberOfDepotsPerType);
			this.setVehicleDepots(vehicleDepots);
			this.setAreaOfAdditonalDepots(areaOfAdditonalDepots);
			this.setJspritIterations(jspritIterations);
			this.setFleetSize(fleetSize);
			this.setVehicleStartTime(vehicleStartTime);
			this.setVehicleEndTime(vehicleEndTime);
			this.setFixedNumberOfVehilcePerTypeAndLocation(fixedNumberOfVehilcePerTypeAndLocation);
		}

		public String getName() {
			return carrierName;
		}

		void setCarrierName(String carrierName) {
			this.carrierName = carrierName;
		}

		public String[] getVehicleTypes() {
			return vehilceTypes;
		}

		void setVehicleTypes(String[] vehicleTypes) {
			this.vehilceTypes = vehicleTypes;
		}

		public String[] getVehicleDepots() {
			return vehicleDepots;
		}

		public void setVehicleDepots(String[] vehicleDepots) {
			this.vehicleDepots = vehicleDepots;
		}

		public void addVehicleDepots(String[] vehicleDepots, String newDepot) {
			String[] newdepotList = new String[vehicleDepots.length + 1];
			int count = 0;
			for (int cnt = 0; cnt < vehicleDepots.length; cnt++, count++) {
				newdepotList[cnt] = vehicleDepots[cnt];
			}
			newdepotList[count] = newDepot;
			this.vehicleDepots = newdepotList;
		}

		public FleetSize getFleetSize() {
			return fleetSize;
		}

		public void setFleetSize(FleetSize fleetSize) {
			this.fleetSize = fleetSize;
		}

		public int getVehicleStartTime() {
			return vehicleStartTime;
		}

		public void setVehicleStartTime(int carrierStartTime) {
			this.vehicleStartTime = carrierStartTime;
		}

		public int getVehicleEndTime() {
			return vehicleEndTime;
		}

		public void setVehicleEndTime(int vehicleEndTime) {
			this.vehicleEndTime = vehicleEndTime;
		}

		public int getJspritIterations() {
			return jspritIterations;
		}

		public void setJspritIterations(int jspritIterations) {
			this.jspritIterations = jspritIterations;
		}

		public int getNumberOfDepotsPerType() {
			return numberOfDepotsPerType;
		}

		public void setNumberOfDepotsPerType(int numberOfDepotsPerType) {
			this.numberOfDepotsPerType = numberOfDepotsPerType;
		}

		public String[] getAreaOfAdditonalDepots() {
			return areaOfAdditonalDepots;
		}

		public void setAreaOfAdditonalDepots(String[] areaOfAdditonalDepots) {
			this.areaOfAdditonalDepots = areaOfAdditonalDepots;
		}

		public int getFixedNumberOfVehilcePerTypeAndLocation() {
			return fixedNumberOfVehilcePerTypeAndLocation;
		}

		public void setFixedNumberOfVehilcePerTypeAndLocation(int fixedNumberOfVehilcePerTypeAndLocation) {
			this.fixedNumberOfVehilcePerTypeAndLocation = fixedNumberOfVehilcePerTypeAndLocation;
		}
	}

	/**
	 * Reads and create the carriers with reading the information from the csv file.
	 * 
	 * @param scenario
	 * @param allNewCarrier
	 * @param freightConfigGroup
	 * @param csvLocationCarrier
	 * @param polygonsInShape
	 * @param defaultJspritIterations
	 * @param crsTransformationNetworkAndShape
	 * @throws IOException
	 */
	static void readAndCreateCarrierFromCSV(Scenario scenario, FreightConfigGroup freightConfigGroup,
			String csvLocationCarrier, Collection<SimpleFeature> polygonsInShape, int defaultJspritIterations,
			CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {

		log.info("Start reading carrier csv file: " + csvLocationCarrier);
		Set<CarrierInformationElement> allNewCarrierInformation = new HashSet<>();
		CSVParser parse = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader()
				.parse(IOUtils.getBufferedReader(csvLocationCarrier));
		for (CSVRecord record : parse) {
			String carrierName = null;
			if (!record.get("carrierName").isBlank())
				carrierName = record.get("carrierName");
			String[] vehilceTypes = null;
			if (!record.get("vehicleTypes").isBlank())
				vehilceTypes = record.get("vehicleTypes").split(",");
			int numberOfDepots = 0;
			if (!record.get("numberOfDepots").isBlank())
				numberOfDepots = Integer.parseInt(record.get("numberOfDepots"));
			String[] vehicleDepots = null;
			if (!record.get("selectedVehicleDepots").isBlank())
				vehicleDepots = record.get("selectedVehicleDepots").split(",");
			String[] areaOfAdditonalDepots = null;
			if (!record.get("areaOfAdditonalDepots").isBlank())
				areaOfAdditonalDepots = record.get("areaOfAdditonalDepots").split(",");
			FleetSize fleetSize = null;
			int fixedNumberOfVehilcePerTypeAndLocation = 0;
			if (!record.get("fixedNumberOfVehilcePerTypeAndLocation").isBlank())
				fixedNumberOfVehilcePerTypeAndLocation = Integer
						.parseInt(record.get("fixedNumberOfVehilcePerTypeAndLocation"));
			if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("infinite"))
				fleetSize = FleetSize.INFINITE;
			else if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("finite"))
				fleetSize = FleetSize.FINITE;
			else if (!record.get("fleetSize").isBlank())
				throw new RuntimeException("Select a valid FleetSize for the carrier: " + carrierName
						+ ". Possible is finite or infinite!!");
			int vehicleStartTime = 0;
			if (!record.get("vehicleStartTime").isBlank())
				vehicleStartTime = Integer.parseInt(record.get("vehicleStartTime"));
			int vehicleEndTime = 0;
			if (!record.get("vehicleEndTime").isBlank())
				vehicleEndTime = Integer.parseInt(record.get("vehicleEndTime"));
			int jspritIterations = 0;
			if (!record.get("jspritIterations").isBlank())
				jspritIterations = Integer.parseInt(record.get("jspritIterations"));
			CarrierInformationElement newCarrierInformationElement = new CarrierInformationElement(carrierName,
					vehilceTypes, numberOfDepots, vehicleDepots, areaOfAdditonalDepots, fleetSize, vehicleStartTime,
					vehicleEndTime, jspritIterations, fixedNumberOfVehilcePerTypeAndLocation);
			allNewCarrierInformation.add(newCarrierInformationElement);
		}
		checkNewCarrier(allNewCarrierInformation, freightConfigGroup, scenario, polygonsInShape);
		log.info("The read carrier information from the csv are checked without errors.");
		createNewCarrierAndAddVehilceTypes(scenario, allNewCarrierInformation, freightConfigGroup, polygonsInShape,
				defaultJspritIterations, crsTransformationNetworkAndShape);
	}

	/**
	 * Checks if the read carrier information are consistent.
	 * 
	 * @param allNewCarrierInformation
	 * @param freightConfigGroup
	 * @param scenario
	 * @param polygonsInShape
	 */
	private static void checkNewCarrier(Set<CarrierInformationElement> allNewCarrierInformation,
			FreightConfigGroup freightConfigGroup, Scenario scenario, Collection<SimpleFeature> polygonsInShape) {

		FreightUtils.addOrGetCarriers(scenario);
		for (CarrierInformationElement carrier : allNewCarrierInformation) {
			if (FreightUtils.getCarriers(scenario).getCarriers()
					.containsKey(Id.create(carrier.getName(), Carrier.class)))
				throw new RuntimeException("The Carrier " + carrier.getName()
						+ " being loaded from the csv is already in the given Carrier file. It is not possible to add to an existing Carrier. Please check!");

			if (carrier.getName() == null || carrier.getName().isBlank())
				throw new RuntimeException(
						"Minimum one carrier has no name. Every carrier information has to be related to one carrier. Please check the input csv file!");
			CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
			new CarrierVehicleTypeReader(carrierVehicleTypes)
					.readFile(freightConfigGroup.getCarriersVehicleTypesFile());
			if (carrier.getVehicleTypes() != null)
				for (String type : carrier.getVehicleTypes()) {
					if (!carrierVehicleTypes.getVehicleTypes().containsKey(Id.create(type, VehicleType.class)))
						throw new RuntimeException("The selected vehicleType " + type + " of the carrier "
								+ carrier.getName()
								+ " in the input file is not part of imported vehicle types. Please change the type or add the type in the vehicleTypes input file!");
				}
			if (carrier.getVehicleDepots() != null) {
				if (carrier.getNumberOfDepotsPerType() < carrier.getVehicleDepots().length)
					throw new RuntimeException("For the carrier " + carrier.getName()
							+ " more certain depots than the given number of depots are selected. (numberOfDepots < selectedVehicleDepots)");

				for (String linkDepot : carrier.getVehicleDepots()) {
					if (!scenario.getNetwork().getLinks().containsKey(Id.create(linkDepot, Link.class)))
						throw new RuntimeException("The selected link " + linkDepot + " for a depot of the carrier "
								+ carrier.getName() + " is not part of the network. Please check!");
				}
			}
			if (carrier.getVehicleTypes() != null && carrier.getNumberOfDepotsPerType() == 0
					&& carrier.getVehicleDepots() == null)
				throw new RuntimeException(
						"If a vehicle type is selected in the input file, numberOfDepots or selectedVehicleDepots should be set. Please check carrier "
								+ carrier.getName());
			if (carrier.getVehicleDepots() != null
					&& (carrier.getNumberOfDepotsPerType() > carrier.getVehicleDepots().length)
					&& carrier.getAreaOfAdditonalDepots() == null)
				log.warn(
						"No possible area for addional depot given. Random choice in the hole network of a possible position");
			if (carrier.getVehicleDepots() == null && (carrier.getNumberOfDepotsPerType() > 0)
					&& carrier.getAreaOfAdditonalDepots() == null)
				log.warn(
						"No possible area for addional depot given. Random choice in the hole network of a possible position");
			if (carrier.getAreaOfAdditonalDepots() != null) {
				if (polygonsInShape == null)
					throw new RuntimeException("For carrier " + carrier.getName()
							+ " a certain area for depots is selected, but no shape is read in. Please check.");
				for (String depotArea : carrier.getAreaOfAdditonalDepots()) {
					boolean isInShape = false;
					for (SimpleFeature singlePolygon : polygonsInShape) {
						if (singlePolygon.getAttribute("Ortsteil").equals(depotArea)
								|| singlePolygon.getAttribute("BEZNAME").equals(depotArea)) {
							isInShape = true;
							break;
						}
					}
					if (!isInShape)
						throw new RuntimeException("The area " + depotArea + " of the possible depots of carrier"
								+ carrier.getName() + " is not part of the given shapeFile");
				}
			}
			if (carrier.getFixedNumberOfVehilcePerTypeAndLocation() != 0)
				for (CarrierInformationElement existingCarrier : allNewCarrierInformation)
					if ((existingCarrier.getName().equals(carrier.getName())
							&& existingCarrier.getFleetSize() == FleetSize.INFINITE)
							|| carrier.getFleetSize() == FleetSize.INFINITE)
						throw new RuntimeException("For the carrier " + carrier.getName()
								+ " a infinite fleetSize configuration was set, although you want to set a fixed number of vehicles. Please check!");
			if (carrier.getFleetSize() != null)
				for (CarrierInformationElement existingCarrier : allNewCarrierInformation)
					if (existingCarrier.getName().equals(carrier.getName()) && existingCarrier.getFleetSize() != null
							&& existingCarrier.getFleetSize() != carrier.getFleetSize())
						throw new RuntimeException("For the carrier " + carrier.getName()
								+ " different fleetSize configuration was set. Please check and select only one!");
			if (carrier.getVehicleTypes() != null) {
				if (carrier.getVehicleStartTime() == 0 || carrier.getVehicleEndTime() == 0)
					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
							+ " no start and/or end time for the vehicles was selected. Please set both times!!");
				if (carrier.getVehicleStartTime() >= carrier.getVehicleEndTime())
					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
							+ " a startTime after the endTime for the vehicles was selected. Please check!");
			}
			if (carrier.getJspritIterations() != 0)
				for (CarrierInformationElement existingCarrier : allNewCarrierInformation)
					if (existingCarrier.getName().equals(carrier.getName())
							&& existingCarrier.getJspritIterations() != 0
							&& existingCarrier.getJspritIterations() != carrier.getJspritIterations())
						throw new RuntimeException("For the carrier " + carrier.getName()
								+ " different number of jsprit iterations are set. Please check!");
		}
	}

	/**
	 * Read and creates the carrier and the vehicle types.
	 * 
	 * @param scenario
	 * @param allNewCarrierInformation
	 * @param freightConfigGroup
	 * @param polygonsInShape
	 * @param defaultJspritIterations
	 * @param crsTransformationNetworkAndShape
	 */
	private static void createNewCarrierAndAddVehilceTypes(Scenario scenario,
			Set<CarrierInformationElement> allNewCarrierInformation, FreightConfigGroup freightConfigGroup,
			Collection<SimpleFeature> polygonsInShape, int defaultJspritIterations,
			CoordinateTransformation crsTransformationNetworkAndShape) {

		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypes usedCarrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(freightConfigGroup.getCarriersVehicleTypesFile());

		for (CarrierInformationElement singleNewCarrier : allNewCarrierInformation) {
			if (singleNewCarrier.getVehicleTypes() == null) {
				continue;
			}
			Carrier thisCarrier = null;
			CarrierCapabilities carrierCapabilities = null;
			if (carriers.getCarriers().containsKey(Id.create(singleNewCarrier.getName(), Carrier.class))) {
				thisCarrier = carriers.getCarriers().get(Id.create(singleNewCarrier.getName(), Carrier.class));
				carrierCapabilities = thisCarrier.getCarrierCapabilities();
				if (carrierCapabilities.getFleetSize() == null && singleNewCarrier.getFleetSize() != null)
					carrierCapabilities.setFleetSize(singleNewCarrier.getFleetSize());
				if (singleNewCarrier.getJspritIterations() > 0)
					CarrierUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
			} else {
				thisCarrier = CarrierUtils.createCarrier(Id.create(singleNewCarrier.getName(), Carrier.class));
				if (singleNewCarrier.getJspritIterations() > 0)
					CarrierUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
				carrierCapabilities = CarrierCapabilities.Builder.newInstance()
						.setFleetSize(singleNewCarrier.getFleetSize()).build();
				carriers.addCarrier(thisCarrier);
			}
			if (singleNewCarrier.getVehicleDepots() == null)
				singleNewCarrier.setVehicleDepots(new String[] {});
			while (singleNewCarrier.getVehicleDepots().length < singleNewCarrier.getNumberOfDepotsPerType()) {
				Random rand = new Random();
				Link link = scenario.getNetwork().getLinks().values().stream()
						.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
				if (!link.getId().toString().contains("pt")
						&& FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape,
								singleNewCarrier.getAreaOfAdditonalDepots(), crsTransformationNetworkAndShape)) {
					singleNewCarrier.addVehicleDepots(singleNewCarrier.getVehicleDepots(), link.getId().toString());
				}
			}
			for (String singleDepot : singleNewCarrier.getVehicleDepots()) {
				for (String thisVehicleType : singleNewCarrier.getVehicleTypes()) {
					VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
							.get(Id.create(thisVehicleType, VehicleType.class));
					usedCarrierVehicleTypes.getVehicleTypes().putIfAbsent(Id.create(thisVehicleType, VehicleType.class),
							thisType);
					if (singleNewCarrier.getFixedNumberOfVehilcePerTypeAndLocation() == 0)
						singleNewCarrier.setFixedNumberOfVehilcePerTypeAndLocation(1);
					for (int i = 0; i < singleNewCarrier.getFixedNumberOfVehilcePerTypeAndLocation(); i++) {
						CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(Id.create(
								thisType.getId().toString() + "_" + thisCarrier.getId().toString() + "_" + singleDepot
										+ "_start" + singleNewCarrier.getVehicleStartTime() + "_" + (i + 1),
								Vehicle.class), Id.createLinkId(singleDepot), thisType)
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
			if (CarrierUtils.getJspritIterations(carrier) == Integer.MIN_VALUE) {
				CarrierUtils.setJspritIterations(carrier, defaultJspritIterations);
				log.warn("The jspritIterations are now set to the default value of " + defaultJspritIterations
						+ " in this simulation!");
			}
		}
	}

}