package org.matsim.smallScaleCommercialTrafficGeneration;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.facilities.ActivityFacility;
import org.matsim.freight.carriers.*;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetCommercialTourSpecifications;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

import static org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.getVehicleStartTime;
import static org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.getVehicleTourDuration;

public class DefaultVehicleSelection implements VehicleSelection{
	private static final Logger log = LogManager.getLogger(GenerateSmallScaleCommercialTrafficDemand.class);

	//Configurations
	private int jspritIterations = 0;

	//Needed for all computations (static)
	private final static Random rnd = MatsimRandom.getRandom();

	//Needed for this computation
	GetCommercialTourSpecifications getCommercialTourSpecifications;
	Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone;
	TripDistributionMatrix odMatrix;
	Map<String, Object2DoubleMap<String>> resultingDataPerZone;
	Map<String, Map<Id<Link>, Link>> linksPerZone;

	/**
	 * Creates the carriers and the related demand, based on the generated
	 * TripDistributionMatrix.
	 * @param scenario Scenario (loaded from your config), where the carriers will be put into
	 * @param getCommercialTourSpecifications
	 * @param facilitiesPerZone
	 * @param jspritIterations
	 * @param odMatrix Can be generated in {@link GenerateSmallScaleCommercialTrafficDemand}
	 * @param smallScaleCommercialTrafficType Selected traffic types. Options: commercialPersonTraffic, goodsTraffic
	 * @param resultingDataPerZone Data distribution to zones (Given in {@link GenerateSmallScaleCommercialTrafficDemand}
	 * @param linksPerZone
	 */
	@Override
	public void createCarriers(Scenario scenario,
							   GetCommercialTourSpecifications getCommercialTourSpecifications,
							   Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone,
							   int jspritIterations,
							   TripDistributionMatrix odMatrix,
							   String smallScaleCommercialTrafficType,
							   Map<String, Object2DoubleMap<String>> resultingDataPerZone,
							   Map<String, Map<Id<Link>, Link>> linksPerZone){
		//Save the given data
		RandomGenerator rng = new MersenneTwister(scenario.getConfig().global().getRandomSeed());
		this.getCommercialTourSpecifications = getCommercialTourSpecifications;
		this.facilitiesPerZone = facilitiesPerZone;
		this.jspritIterations = jspritIterations;
		this.odMatrix = odMatrix;
		this.resultingDataPerZone = resultingDataPerZone;
		this.linksPerZone = linksPerZone;

		int maxNumberOfCarrier = odMatrix.getListOfPurposes().size() * odMatrix.getListOfZones().size()
			* odMatrix.getListOfModesOrVehTypes().size();
		int createdCarrier = 0;
		int fixedNumberOfVehiclePerTypeAndLocation = 1; //TODO possible improvement, perhaps check KiD

		EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration> tourDistribution = getCommercialTourSpecifications.createTourDistribution(smallScaleCommercialTrafficType, rng);

		Map<GenerateSmallScaleCommercialTrafficDemand.StopDurationGoodTrafficKey, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds>> stopDurationTimeSelector = getCommercialTourSpecifications.createStopDurationDistributionPerCategory(smallScaleCommercialTrafficType, rng);

		CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);
		Map<Id<VehicleType>, VehicleType> additionalCarrierVehicleTypes = scenario.getVehicles().getVehicleTypes();

		// Only a vehicle with cost information will work properly
		additionalCarrierVehicleTypes.values().stream()
			.filter(vehicleType -> vehicleType.getCostInformation().getCostsPerSecond() != null)
			.forEach(vehicleType -> carrierVehicleTypes.getVehicleTypes().putIfAbsent(vehicleType.getId(), vehicleType));

		for (VehicleType vehicleType : carrierVehicleTypes.getVehicleTypes().values()) {
			CostInformation costInformation = vehicleType.getCostInformation();
			VehicleUtils.setCostsPerSecondInService(costInformation, costInformation.getCostsPerSecond());
			VehicleUtils.setCostsPerSecondWaiting(costInformation, costInformation.getCostsPerSecond());
		}

		for (Integer purpose : odMatrix.getListOfPurposes()) {
			for (String startZone : odMatrix.getListOfZones()) {
				for (String modeORvehType : odMatrix.getListOfModesOrVehTypes()) {
					boolean isStartingLocation = false;
					checkIfIsStartingPosition:
					{
						for (String possibleStopZone : odMatrix.getListOfZones()) {
							if (!modeORvehType.equals("pt") && !modeORvehType.equals("op"))
								if (odMatrix.getTripDistributionValue(startZone, possibleStopZone, modeORvehType,
									purpose, smallScaleCommercialTrafficType) != 0) {
									isStartingLocation = true;
									break checkIfIsStartingPosition;
								}
						}
					}

					if (isStartingLocation) {
						double occupancyRate = 0;
						String[] possibleVehicleTypes = null;
						ArrayList<String> startCategory = new ArrayList<>();
						ArrayList<String> stopCategory = new ArrayList<>();
						stopCategory.add("Employee Primary Sector");
						stopCategory.add("Employee Construction");
						stopCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Retail");
						stopCategory.add("Employee Traffic/Parcels");
						stopCategory.add("Employee Tertiary Sector Rest");
						stopCategory.add("Inhabitants");
						if (purpose == 1) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
								occupancyRate = 1.5;
							}
							startCategory.add("Employee Secondary Sector Rest");
							stopCategory.clear();
							stopCategory.add("Employee Secondary Sector Rest");
						} else if (purpose == 2) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
								occupancyRate = 1.6;
							}
							startCategory.add("Employee Secondary Sector Rest");
						} else if (purpose == 3) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Retail");
							startCategory.add("Employee Tertiary Sector Rest");
						} else if (purpose == 4) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Traffic/Parcels");
						} else if (purpose == 5) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
								occupancyRate = 1.7;
							}
							startCategory.add("Employee Construction");
						} else if (purpose == 6) {
							startCategory.add("Inhabitants");
						}
						if (smallScaleCommercialTrafficType.equals("goodsTraffic")) {
							occupancyRate = 1.;
							switch (modeORvehType) {
								case "vehTyp1" ->
									possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"}; // possible to add more types, see source
								case "vehTyp2" ->
									possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
								case "vehTyp3", "vehTyp4" ->
									possibleVehicleTypes = new String[]{"light8t", "light8t_electro"};
								case "vehTyp5" ->
									possibleVehicleTypes = new String[]{"medium18t", "medium18t_electro", "heavy40t", "heavy40t_electro"};
							}
						}

						// use only types of the possibleTypes which are in the given types file
						List<String> vehicleTypes = new ArrayList<>();
						assert possibleVehicleTypes != null;

						for (String possibleVehicleType : possibleVehicleTypes) {
							if (CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().containsKey(
								Id.create(possibleVehicleType, VehicleType.class)))
								vehicleTypes.add(possibleVehicleType);
						}
						// find a start category with existing employees in this zone
						Collections.shuffle(startCategory, rnd);
						String selectedStartCategory = startCategory.getFirst();
						for (int count = 1; resultingDataPerZone.get(startZone).getDouble(selectedStartCategory) == 0; count++) {
							if (count <= startCategory.size())
								selectedStartCategory = startCategory.get(rnd.nextInt(startCategory.size()));
							else
								selectedStartCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
						}
						String carrierName = null;
						if (smallScaleCommercialTrafficType.equals("goodsTraffic")) {
							carrierName = "Carrier_Goods_" + startZone + "_purpose_" + purpose + "_" + modeORvehType;
						} else if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic"))
							carrierName = "Carrier_Business_" + startZone + "_purpose_" + purpose;
						int numberOfDepots = odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType, purpose,
							smallScaleCommercialTrafficType);
						CarrierCapabilities.FleetSize fleetSize = CarrierCapabilities.FleetSize.FINITE;
						ArrayList<String> vehicleDepots = new ArrayList<>();
						createdCarrier++;
						log.info("Create carrier number {} of a maximum Number of {} carriers.", createdCarrier, maxNumberOfCarrier);
						log.info("Carrier: {}; depots: {}; services: {}", carrierName, numberOfDepots,
							(int) Math.ceil(odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType,
								purpose, smallScaleCommercialTrafficType) / occupancyRate));
						createNewCarrierAndAddVehicleTypes(scenario, purpose, startZone,
							selectedStartCategory, carrierName, vehicleTypes, numberOfDepots, fleetSize,
							fixedNumberOfVehiclePerTypeAndLocation, vehicleDepots, smallScaleCommercialTrafficType,
							tourDistribution);
						log.info("Create services for carrier: {}", carrierName);
						for (String stopZone : odMatrix.getListOfZones()) {
							int trafficVolumeForOD = Math.round((float)odMatrix.getTripDistributionValue(startZone,
								stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType));
							int numberOfJobs = (int) Math.ceil(trafficVolumeForOD / occupancyRate);
							if (numberOfJobs == 0)
								continue;
							// find a category for the tour stop with existing employees in this zone
							String selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							while (resultingDataPerZone.get(stopZone).getDouble(selectedStopCategory) == 0)
								selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							String[] serviceArea = new String[]{stopZone};
							int serviceTimePerStop;
							if (selectedStartCategory.equals("Inhabitants"))
								serviceTimePerStop = getServiceTimePerStop(stopDurationTimeSelector, startCategory.getFirst(), modeORvehType, smallScaleCommercialTrafficType);
							else
								serviceTimePerStop = getServiceTimePerStop(stopDurationTimeSelector, selectedStartCategory, modeORvehType, smallScaleCommercialTrafficType);

							TimeWindow serviceTimeWindow = TimeWindow.newInstance(0, 36 * 3600); // extended time window, so that late tours can handle it
							createServices(scenario, vehicleDepots, selectedStopCategory, carrierName,
								numberOfJobs, serviceArea, serviceTimePerStop, serviceTimeWindow);
						}
					}
				}
			}
		}

//		System.out.println("Final results for the start time distribution");
//		tourStartTimeSelector.writeResults();

//		System.out.println("Final results for the tour duration distribution");
//		tourDurationTimeSelector.writeResults();

//		for (StopDurationGoodTrafficKey sector : stopDurationTimeSelector.keySet()) {
//			System.out.println("Final results for the stop duration distribution in sector " + sector);
//			stopDurationTimeSelector.get(sector);
//		}

		log.warn("The jspritIterations are now set to {} in this simulation!", jspritIterations);
		log.info("Finished creating {} carriers including related services.", createdCarrier);

	}

	/**
	 * Creates the services for one carrier.
	 */
	private void createServices(Scenario scenario,
								ArrayList<String> noPossibleLinks,
								String selectedStopCategory,
								String carrierName,
								int numberOfJobs,
								String[] serviceArea,
								Integer serviceTimePerStop,
								TimeWindow serviceTimeWindow) {

		String stopZone = serviceArea[0];

		for (int i = 0; i < numberOfJobs; i++) {
			Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks);
			Id<CarrierService> idNewService = Id.create(carrierName + "_" + linkId + "_" + rnd.nextInt(10000),
				CarrierService.class);

			CarrierService thisService = CarrierService.Builder.newInstance(idNewService, linkId)
				.setServiceDuration(serviceTimePerStop).setServiceStartTimeWindow(serviceTimeWindow).build();
			CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create(carrierName, Carrier.class)).getServices()
				.put(thisService.getId(), thisService);
		}
	}

	/**
	 * Creates the carrier and the related vehicles.
	 */
	private void createNewCarrierAndAddVehicleTypes(Scenario scenario,
													Integer purpose,
													String startZone,
													String selectedStartCategory,
													String carrierName,
													List<String> vehicleTypes,
													int numberOfDepots,
													CarrierCapabilities.FleetSize fleetSize,
													int fixedNumberOfVehiclePerTypeAndLocation,
													List<String> vehicleDepots,
													String smallScaleCommercialTrafficType,
													EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration> tourStartTimeSelector) {

		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);

		CarrierCapabilities carrierCapabilities;

		Carrier thisCarrier = CarriersUtils.createCarrier(Id.create(carrierName, Carrier.class));
		if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic") && purpose == 3)
			thisCarrier.getAttributes().putAttribute("subpopulation", smallScaleCommercialTrafficType + "_service");
		else
			thisCarrier.getAttributes().putAttribute("subpopulation", smallScaleCommercialTrafficType);

		thisCarrier.getAttributes().putAttribute("purpose", purpose);
		thisCarrier.getAttributes().putAttribute("tourStartArea", startZone);
		if (jspritIterations > 0)
			CarriersUtils.setJspritIterations(thisCarrier, jspritIterations);
		carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build();

		carriers.addCarrier(thisCarrier);

		while (vehicleDepots.size() < numberOfDepots) {
			Id<Link> linkId = findPossibleLink(startZone, selectedStartCategory, null);
			vehicleDepots.add(linkId.toString());
		}

		List<Double> availableVehicles = new LinkedList<>();

		for (String singleDepot : vehicleDepots) {
			GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration t = tourStartTimeSelector.sample();

			int vehicleStartTime = getVehicleStartTime(t);
			int tourDuration = getVehicleTourDuration(t);
			int vehicleEndTime = vehicleStartTime + tourDuration;
			for (String thisVehicleType : vehicleTypes) { //TODO Flottenzusammensetzung anpassen. Momentan pro Depot alle Fahrzeugtypen 1x erzeugen
				VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
					.get(Id.create(thisVehicleType, VehicleType.class));
				if (fixedNumberOfVehiclePerTypeAndLocation == 0)
					fixedNumberOfVehiclePerTypeAndLocation = 1;
				for (int i = 0; i < fixedNumberOfVehiclePerTypeAndLocation; i++) {
					CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
						.newInstance(
							Id.create(
								thisCarrier.getId().toString() + "_"
									+ (carrierCapabilities.getCarrierVehicles().size() + 1),
								Vehicle.class),
							Id.createLinkId(singleDepot), thisType)
						.setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
					availableVehicles.add((double) (tourDuration));
					carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
					if (!carrierCapabilities.getVehicleTypes().contains(thisType))
						carrierCapabilities.getVehicleTypes().add(thisType);
				}
			}

			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
	}

	/**
	 * Give a service duration based on the purpose and the trafficType under a given probability
	 *
	 * @param serviceDurationTimeSelector 		the selector for the service duration
	 * @param employeeCategory 					the category of the employee
	 * @param modeORvehType 					the mode or vehicle type
	 * @return 									the service duration
	 */
	private Integer getServiceTimePerStop(Map<GenerateSmallScaleCommercialTrafficDemand.StopDurationGoodTrafficKey, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds>> serviceDurationTimeSelector,
										  String employeeCategory,
										  String modeORvehType,
										  String smallScaleCommercialTrafficType) {
		GenerateSmallScaleCommercialTrafficDemand.StopDurationGoodTrafficKey key = null;
		if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()))
			key = GenerateSmallScaleCommercialTrafficDemand.makeStopDurationGoodTrafficKey(employeeCategory, null);
		else if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			key = GenerateSmallScaleCommercialTrafficDemand.makeStopDurationGoodTrafficKey(employeeCategory, modeORvehType);
		}
		GenerateSmallScaleCommercialTrafficDemand.DurationsBounds serviceDurationBounds = serviceDurationTimeSelector.get(key).sample();
		int serviceDurationLowerBound = serviceDurationBounds.minDuration();
		int serviceDurationUpperBound = serviceDurationBounds.maxDuration();
		return rnd.nextInt(serviceDurationLowerBound * 60, serviceDurationUpperBound * 60);
	}

	/**
	 * Finds a possible link for a service or the vehicle location.
	 */
	private Id<Link> findPossibleLink(String zone, String selectedCategory, List<String> noPossibleLinks) {
		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < facilitiesPerZone.get(zone).get(selectedCategory).size() * 2; a++) {

			ActivityFacility possibleBuilding = facilitiesPerZone.get(zone).get(selectedCategory)
				.get(rnd.nextInt(facilitiesPerZone.get(zone).get(selectedCategory).size())); //TODO Wkt für die Auswahl anpassen
			Coord centroidPointOfBuildingPolygon = possibleBuilding.getCoord();

			int numberOfPossibleLinks = linksPerZone.get(zone).size();

			// searches and selects the nearest link of the possible links in this zone
			newLink = SmallScaleCommercialTrafficUtils.findNearestPossibleLink(zone, noPossibleLinks, linksPerZone, newLink,
				centroidPointOfBuildingPolygon, numberOfPossibleLinks);
		}
		if (newLink == null)
			throw new RuntimeException("No possible link for buildings with type '" + selectedCategory + "' in zone '"
				+ zone + "' found. buildings in category: " + facilitiesPerZone.get(zone).get(selectedCategory)
				+ "; possibleLinks in zone: " + linksPerZone.get(zone).size());
		return newLink;
	}
}
