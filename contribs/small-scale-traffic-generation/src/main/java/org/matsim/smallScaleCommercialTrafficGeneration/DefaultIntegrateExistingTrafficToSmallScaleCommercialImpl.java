package org.matsim.smallScaleCommercialTrafficGeneration;

import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.vehicles.VehicleType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.getObjectiveFunction;
import static org.matsim.smallScaleCommercialTrafficGeneration.TrafficVolumeGeneration.makeTrafficVolumeKey;

public class DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl implements IntegrateExistingTrafficToSmallScaleCommercial {
	private static final Logger log = LogManager.getLogger(DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl.class);

	/**
	 * Reduces the demand for certain zone.
	 *
	 * @param trafficVolumePerTypeAndZone_start trafficVolume for start potentials for each zone
	 * @param trafficVolumePerTypeAndZone_stop  trafficVolume for stop potentials for each zone
	 * @param modeORvehType                     selected mode or vehicleType
	 * @param purpose                           certain purpose
	 * @param startZone                         start zone
	 * @param stopZone                          end zone
	 */
	protected static void reduceVolumeForThisExistingJobElement(
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start,
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop, String modeORvehType,
		Integer purpose, String startZone, String stopZone) {

		if (startZone == null && stopZone == null)
			throw new IllegalArgumentException();

		TrafficVolumeGeneration.TrafficVolumeKey trafficVolumeKey_start = makeTrafficVolumeKey(startZone, modeORvehType);
		TrafficVolumeGeneration.TrafficVolumeKey trafficVolumeKey_stop = makeTrafficVolumeKey(stopZone, modeORvehType);
		Object2DoubleMap<Integer> startVolume = trafficVolumePerTypeAndZone_start.get(trafficVolumeKey_start);
		Object2DoubleMap<Integer> stopVolume = trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey_stop);

		if (startVolume != null && startVolume.getDouble(purpose) == 0)
			reduceVolumeForOtherArea(trafficVolumePerTypeAndZone_start, modeORvehType, purpose, "Start", trafficVolumeKey_start.getZone());
		else if (startVolume != null)
			startVolume.mergeDouble(purpose, -1, Double::sum);
		if (stopVolume != null && stopVolume.getDouble(purpose) == 0)
			reduceVolumeForOtherArea(trafficVolumePerTypeAndZone_stop, modeORvehType, purpose, "Stop", trafficVolumeKey_stop.getZone());
		else if (stopVolume != null)
			stopVolume.mergeDouble(purpose, -1, Double::sum);
	}

	protected static String findZoneOfLink(Map<String, Map<Id<Link>, Link>> linksPerZone, Id<Link> linkId) {
		AtomicReference<String> resultingZone = new AtomicReference<>();

		linksPerZone.forEach( (zone, links) -> {
			if (links.containsKey(linkId)) {
				resultingZone.set(zone);
			}
		});
		if (resultingZone.get() == null) {
			return null;
		}
		return resultingZone. get();
	}

	/**
	 * Finds zone with demand and reduces this demand by 1.
	 *
	 * @param trafficVolumePerTypeAndZone traffic volumes
	 * @param modeORvehType               selected mode or vehicleType
	 * @param purpose                     selected purpose
	 * @param volumeType                  start or stop volume
	 * @param originalZone                zone with volume of 0, although a volume of an existing model exists
	 */
	private static void reduceVolumeForOtherArea(
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone, String modeORvehType,
		Integer purpose, String volumeType, String originalZone) {
		ArrayList<TrafficVolumeGeneration.TrafficVolumeKey> shuffledKeys = new ArrayList<>(
			trafficVolumePerTypeAndZone.keySet());
		Collections.shuffle(shuffledKeys, MatsimRandom.getRandom());
		for (TrafficVolumeGeneration.TrafficVolumeKey trafficVolumeKey : shuffledKeys) {
			if (trafficVolumeKey.getModeORvehType().equals(modeORvehType)
				&& trafficVolumePerTypeAndZone.get(trafficVolumeKey).getDouble(purpose) > 0) {
				trafficVolumePerTypeAndZone.get(trafficVolumeKey).mergeDouble(purpose, -1, Double::sum);
				log.warn(
					"{}-Volume of zone {} (mode '{}', purpose '{}') was reduced because the volume for the zone {}, where an existing model has a demand, has a generated demand of 0.",
					volumeType, trafficVolumeKey.getZone(), modeORvehType, purpose, originalZone);
				break;
			}
		}
	}

	/**
	 * Reads existing scenarios and add them to the scenario. If the scenario is
	 * part of the goodsTraffic or commercialPersonTraffic, the demand of the existing
	 * scenario reduces the demand of the small scale commercial traffic. The
	 * dispersedTraffic will be added additionally.
	 * For this method, the carriers should be located correctly and all needed information is taken from 'existingModels.csv'
	 *
	 * @param scenario       the scenario
	 * @param sampleScenario the sample size of the scenario
	 * @param linksPerZone   the links per zone
	 */
	@Override
	public void readExistingCarriersFromFolder(Scenario scenario, double sampleScenario,
											   Map<String, Map<Id<Link>, Link>> linksPerZone) throws Exception {
		Path existingModelsFolder = Path.of(scenario.getConfig().getContext().toURI()).getParent().resolve("existingModels");
		String locationOfExistingModels = existingModelsFolder.resolve("existingModels.csv").toString();
		CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
			.setSkipHeaderRecord(true).build().parse(IOUtils.getBufferedReader(locationOfExistingModels));
		for (CSVRecord record : parse) {
			String modelName = record.get("model");
			double sampleSizeExistingScenario = Double.parseDouble(record.get("sampleSize"));
			String modelTrafficType = record.get("smallScaleCommercialTrafficType");
			final Integer modelPurpose;
			if (!Objects.equals(record.get("purpose"), ""))
				modelPurpose = Integer.parseInt(record.get("purpose"));
			else
				modelPurpose = null;
			final String vehicleType;
			if (!Objects.equals(record.get("vehicleType"), ""))
				vehicleType = record.get("vehicleType");
			else
				vehicleType = null;
			final String modelMode = record.get("networkMode");

			Path scenarioLocation = existingModelsFolder.resolve(modelName);
			if (!Files.exists(scenarioLocation.resolve("output_carriers.xml.gz")))
				throw new Exception("For the existing model " + modelName
					+ " no carrierFile exists. The carrierFile should have the name 'output_carriers.xml.gz'");
			if (!Files.exists(scenarioLocation.resolve("vehicleTypes.xml.gz")))
				throw new Exception("For the existing model " + modelName
					+ " no vehicleTypesFile exists. The vehicleTypesFile should have the name 'vehicleTypes.xml.gz'");

			log.info("Integrating existing scenario: {}", modelName);

			CarrierVehicleTypes readVehicleTypes = new CarrierVehicleTypes();
			CarrierVehicleTypes usedVehicleTypes = new CarrierVehicleTypes();
			new CarrierVehicleTypeReader(readVehicleTypes)
				.readFile(scenarioLocation.resolve("vehicleTypes.xml.gz").toString());

			Carriers carriers = new Carriers();
			new CarrierPlanXmlReader(carriers, readVehicleTypes)
				.readFile(scenarioLocation.resolve("output_carriers.xml.gz").toString());

			if (sampleSizeExistingScenario < sampleScenario)
				throw new Exception("The sample size of the existing scenario " + modelName
					+ "is smaller than the sample size of the scenario. No up scaling for existing scenarios implemented.");

			double sampleFactor = sampleScenario / sampleSizeExistingScenario;

			int numberOfToursExistingScenario = 0;
			for (Carrier carrier : carriers.getCarriers().values()) {
				if (!carrier.getPlans().isEmpty())
					numberOfToursExistingScenario = numberOfToursExistingScenario
						+ carrier.getSelectedPlan().getScheduledTours().size();
			}
			int sampledNumberOfToursExistingScenario = (int) Math.round(numberOfToursExistingScenario * sampleFactor);
			List<Carrier> carrierToRemove = new ArrayList<>();
			int remainedTours = 0;
			double roundingError = 0.;

			log.info("The existing scenario {} is a {}% scenario and has {} tours", modelName, (int) (sampleSizeExistingScenario * 100),
				numberOfToursExistingScenario);
			log.info("The existing scenario {} will be sampled down to the scenario sample size of {}% which results in {} tours.", modelName,
				(int) (sampleScenario * 100), sampledNumberOfToursExistingScenario);

			int numberOfAnalyzedTours = 0;
			for (Carrier carrier : carriers.getCarriers().values()) {
				if (!carrier.getPlans().isEmpty()) {
					int numberOfOriginalTours = carrier.getSelectedPlan().getScheduledTours().size();
					numberOfAnalyzedTours += numberOfOriginalTours;
					int numberOfRemainingTours = (int) Math.round(numberOfOriginalTours * sampleFactor);
					roundingError = roundingError + numberOfRemainingTours - (numberOfOriginalTours * sampleFactor);
					int numberOfToursToRemove = numberOfOriginalTours - numberOfRemainingTours;
					List<ScheduledTour> toursToRemove = new ArrayList<>();

					if (roundingError <= -1 && numberOfToursToRemove > 0) {
						numberOfToursToRemove = numberOfToursToRemove - 1;
						numberOfRemainingTours = numberOfRemainingTours + 1;
						roundingError = roundingError + 1;
					}
					if (roundingError >= 1 && numberOfRemainingTours != numberOfToursToRemove) {
						numberOfToursToRemove = numberOfToursToRemove + 1;
						numberOfRemainingTours = numberOfRemainingTours - 1;
						roundingError = roundingError - 1;
					}
					remainedTours = remainedTours + numberOfRemainingTours;
					if (remainedTours > sampledNumberOfToursExistingScenario) {
						remainedTours = remainedTours - 1;
						numberOfRemainingTours = numberOfRemainingTours - 1;
						numberOfToursToRemove = numberOfToursToRemove + 1;
					}
					// last carrier with scheduled tours
					if (numberOfAnalyzedTours == numberOfToursExistingScenario
						&& remainedTours != sampledNumberOfToursExistingScenario) {
						numberOfRemainingTours = sampledNumberOfToursExistingScenario - remainedTours;
						numberOfToursToRemove = numberOfOriginalTours - numberOfRemainingTours;
						remainedTours = remainedTours + numberOfRemainingTours;
					}
					// remove carrier because no tours remaining
					if (numberOfOriginalTours == numberOfToursToRemove) {
						carrierToRemove.add(carrier);
						continue;
					}

					while (toursToRemove.size() < numberOfToursToRemove) {
						Object[] tours = carrier.getSelectedPlan().getScheduledTours().toArray();
						ScheduledTour tour = (ScheduledTour) tours[MatsimRandom.getRandom().nextInt(tours.length)];
						toursToRemove.add(tour);
						carrier.getSelectedPlan().getScheduledTours().remove(tour);
					}

					// remove services/shipments from removed tours
					if (!carrier.getServices().isEmpty()) {
						for (ScheduledTour removedTour : toursToRemove) {
							for (Tour.TourElement tourElement : removedTour.getTour().getTourElements()) {
								if (tourElement instanceof Tour.ServiceActivity service) {
									carrier.getServices().remove(service.getService().getId());
								}
							}
						}
					} else if (!carrier.getShipments().isEmpty()) {
						for (ScheduledTour removedTour : toursToRemove) {
							for (Tour.TourElement tourElement : removedTour.getTour().getTourElements()) {
								if (tourElement instanceof Tour.Pickup pickup) {
									carrier.getShipments().remove(pickup.getShipment().getId());
								}
							}
						}
					}
					// remove vehicles of removed tours and check if all vehicleTypes are still
					// needed
					if (carrier.getCarrierCapabilities().getFleetSize().equals(CarrierCapabilities.FleetSize.FINITE)) {
						for (ScheduledTour removedTour : toursToRemove) {
							carrier.getCarrierCapabilities().getCarrierVehicles()
								.remove(removedTour.getVehicle().getId());
						}
					} else if (carrier.getCarrierCapabilities().getFleetSize().equals(CarrierCapabilities.FleetSize.INFINITE)) {
						carrier.getCarrierCapabilities().getCarrierVehicles().clear();
						for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
							carrier.getCarrierCapabilities().getCarrierVehicles().put(tour.getVehicle().getId(),
								tour.getVehicle());
						}
					}
					List<VehicleType> vehicleTypesToRemove = new ArrayList<>();
					for (VehicleType existingVehicleType : carrier.getCarrierCapabilities().getVehicleTypes()) {
						boolean vehicleTypeNeeded = false;
						for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
							if (vehicle.getType().equals(existingVehicleType)) {
								vehicleTypeNeeded = true;
								usedVehicleTypes.getVehicleTypes().put(existingVehicleType.getId(),
									existingVehicleType);
							}
						}
						if (!vehicleTypeNeeded)
							vehicleTypesToRemove.add(existingVehicleType);
					}
					carrier.getCarrierCapabilities().getVehicleTypes().removeAll(vehicleTypesToRemove);
				}
				// carriers without solutions
				else {
					if (!carrier.getServices().isEmpty()) {
						int numberOfServicesToRemove = carrier.getServices().size()
							- (int) Math.round(carrier.getServices().size() * sampleFactor);
						for (int i = 0; i < numberOfServicesToRemove; i++) {
							Object[] services = carrier.getServices().keySet().toArray();
							carrier.getServices().remove(services[MatsimRandom.getRandom().nextInt(services.length)]);
						}
					}
					if (!carrier.getShipments().isEmpty()) {
						int numberOfShipmentsToRemove = carrier.getShipments().size()
							- (int) Math.round(carrier.getShipments().size() * sampleFactor);
						for (int i = 0; i < numberOfShipmentsToRemove; i++) {
							Object[] shipments = carrier.getShipments().keySet().toArray();
							carrier.getShipments().remove(shipments[MatsimRandom.getRandom().nextInt(shipments.length)]);
						}
					}
				}
			}
			carrierToRemove.forEach(carrier -> carriers.getCarriers().remove(carrier.getId()));
			CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().putAll(usedVehicleTypes.getVehicleTypes());

			carriers.getCarriers().values().forEach(carrier -> {
				Carrier newCarrier = CarriersUtils
					.createCarrier(Id.create(modelName + "_" + carrier.getId().toString(), Carrier.class));
				newCarrier.getAttributes().putAttribute("subpopulation", modelTrafficType);
				if (modelPurpose != null)
					newCarrier.getAttributes().putAttribute("purpose", modelPurpose);
				newCarrier.getAttributes().putAttribute("existingModel", modelName);
				newCarrier.getAttributes().putAttribute("networkMode", modelMode);
				if (vehicleType != null)
					newCarrier.getAttributes().putAttribute("vehicleType", vehicleType);
				newCarrier.setCarrierCapabilities(carrier.getCarrierCapabilities());

				if (!carrier.getServices().isEmpty())
					newCarrier.getServices().putAll(carrier.getServices());
				else if (!carrier.getShipments().isEmpty())
					newCarrier.getShipments().putAll(carrier.getShipments());
				if (carrier.getSelectedPlan() != null) {
					newCarrier.addPlan(carrier.getSelectedPlan());
					newCarrier.setSelectedPlan(carrier.getSelectedPlan());

					List<String> startAreas = new ArrayList<>();
					for (ScheduledTour tour : newCarrier.getSelectedPlan().getScheduledTours()) {
						String tourStartZone = findZoneOfLink(linksPerZone, tour.getTour().getStartLinkId());
						if (!startAreas.contains(tourStartZone))
							startAreas.add(tourStartZone);
					}
					newCarrier.getAttributes().putAttribute("tourStartArea",
						String.join(";", startAreas));

					CarriersUtils.setJspritIterations(newCarrier, 0);
					// recalculate score for selectedPlan
					VehicleRoutingProblem vrp = MatsimJspritFactory
						.createRoutingProblemBuilder(carrier, scenario.getNetwork()).build();
					VehicleRoutingProblemSolution solution = MatsimJspritFactory
						.createSolution(newCarrier.getSelectedPlan(), vrp);
					SolutionCostCalculator solutionCostsCalculator = getObjectiveFunction(vrp, Double.MAX_VALUE);
					double costs = solutionCostsCalculator.getCosts(solution) * (-1);
					carrier.getSelectedPlan().setScore(costs);
				} else {
					CarriersUtils.setJspritIterations(newCarrier, CarriersUtils.getJspritIterations(carrier));
					newCarrier.getCarrierCapabilities().setFleetSize(carrier.getCarrierCapabilities().getFleetSize());
				}
				CarriersUtils.addOrGetCarriers(scenario).getCarriers().put(newCarrier.getId(), newCarrier);
			});
		}
	}

	@Override
	public void reduceDemandBasedOnExistingCarriers(Scenario scenario, Map<String, Map<Id<Link>, Link>> linksPerZone,
													String smallScaleCommercialTrafficType,
													Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start,
													Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop) {
		for (Carrier carrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
			if (!carrier.getAttributes().getAsMap().containsKey("subpopulation")
				|| !carrier.getAttributes().getAttribute("subpopulation").equals(smallScaleCommercialTrafficType))
				continue;
			String modeORvehType;
			if (smallScaleCommercialTrafficType.equals("goodsTraffic"))
				modeORvehType = (String) carrier.getAttributes().getAttribute("vehicleType");
			else
				modeORvehType = "total";
			Integer purpose = (Integer) carrier.getAttributes().getAttribute("purpose");
			if (carrier.getSelectedPlan() != null) {
				for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
					String startZone = findZoneOfLink(linksPerZone, tour.getTour().getStartLinkId());
					for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
						if (tourElement instanceof Tour.ServiceActivity service) {
							String stopZone = findZoneOfLink(linksPerZone, service.getLocation());
							try {
								reduceVolumeForThisExistingJobElement(trafficVolumePerTypeAndZone_start,
									trafficVolumePerTypeAndZone_stop, modeORvehType, purpose, startZone, stopZone);
							} catch (IllegalArgumentException e) {
								log.warn(
									"For the tour {} of carrier {} a location of the service {} is not part of the zones. That's why the traffic volume was not reduces by this service. startZone = {}, stopZone = {}",
									tour.getTour().getId(), carrier.getId().toString(), service.getService().getId(), startZone, stopZone);
							}
						}
						if (tourElement instanceof Tour.Pickup pickup) {
							startZone = findZoneOfLink(linksPerZone, pickup.getShipment().getPickupLinkId());
							String stopZone = findZoneOfLink(linksPerZone, pickup.getShipment().getDeliveryLinkId());
							try {
								reduceVolumeForThisExistingJobElement(trafficVolumePerTypeAndZone_start,
									trafficVolumePerTypeAndZone_stop, modeORvehType, purpose, startZone, stopZone);
							} catch (IllegalArgumentException e) {
								log.warn(
									"For the tour {} of carrier {} a location of the shipment {} is not part of the zones. That's why the traffic volume was not reduces by this shipment.",
									tour.getTour().getId(), carrier.getId().toString(), pickup.getShipment().getId());
							}
						}
					}
				}
			} else {
				if (!carrier.getServices().isEmpty()) {
					List<String> possibleStartAreas = new ArrayList<>();
					for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
						possibleStartAreas
							.add(findZoneOfLink(linksPerZone, vehicle.getLinkId()));
					}
					for (CarrierService service : carrier.getServices().values()) {
						String startZone = (String) possibleStartAreas.toArray()[MatsimRandom.getRandom()
							.nextInt(possibleStartAreas.size())];
						String stopZone = findZoneOfLink(linksPerZone, service.getServiceLinkId());
						try {
							reduceVolumeForThisExistingJobElement(trafficVolumePerTypeAndZone_start,
								trafficVolumePerTypeAndZone_stop, modeORvehType, purpose, startZone, stopZone);
						} catch (IllegalArgumentException e) {
							log.warn(
								"For carrier {} a location of the service {} is not part of the zones. That's why the traffic volume was not reduces by this service.",
								carrier.getId().toString(), service.getId());
						}
					}
				} else if (!carrier.getShipments().isEmpty()) {
					for (CarrierShipment shipment : carrier.getShipments().values()) {
						String startZone = findZoneOfLink(linksPerZone, shipment.getPickupLinkId());
						String stopZone = findZoneOfLink(linksPerZone, shipment.getDeliveryLinkId());
						try {
							reduceVolumeForThisExistingJobElement(trafficVolumePerTypeAndZone_start,
								trafficVolumePerTypeAndZone_stop, modeORvehType, purpose, startZone, stopZone);
						} catch (IllegalArgumentException e) {
							log.warn(
								"For carrier {} a location of the shipment {} is not part of the zones. That's why the traffic volume was not reduces by this shipment.",
								carrier.getId().toString(), shipment.getId());
						}
					}
				}
			}
		}
	}

}
