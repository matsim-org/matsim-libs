package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.common.base.Joiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DefaultUnhandledServicesSolution implements UnhandledServicesSolution {
	private static final Logger log = LogManager.getLogger(DefaultUnhandledServicesSolution.class);
	private static final Joiner JOIN = Joiner.on("\t");

	Random rnd;
	private final GenerateSmallScaleCommercialTrafficDemand generator;

	DefaultUnhandledServicesSolution(GenerateSmallScaleCommercialTrafficDemand generator) {
		rnd = MatsimRandom.getRandom();
		this.generator = generator;
	}

	/**
	 * Redraws the service-durations of these {@link CarrierService}s of the given {@link Carrier} which are not possible to handle with the current vehicle availabilities.
	 * The service durations will be redrawn until they fit into the vehicle availability, including the additional buffer for the travel time.
	 */
	private int redrawUnhandledServiceDurations(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes, List<CarrierService> unhandledServices) {

		int changedServiceDurations = 0;
		for (CarrierService service : unhandledServices) {
			double maxVehicleAvailability = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().mapToDouble(
				vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()).max().orElse(0);
			log.debug("Carrier '{}': max vehicle availability (unused vehicles) is {} minutes. Service '{}' has a duration of {} minutes. (Travel buffer is tour-level, not checked per service.)",
				carrier.getId(), maxVehicleAvailability / 60, service.getId(), service.getServiceDuration() / 60);
			double newServiceDuration = service.getServiceDuration();
			if (newServiceDuration * generator.getFactorForTravelBufferCalculation() <= maxVehicleAvailability) {
				continue;
			}
			while (newServiceDuration * generator.getFactorForTravelBufferCalculation() > maxVehicleAvailability) {
				newServiceDuration = generator.getServiceTimePerStop(carrierAttributes);
			}
				CarrierService.Builder builder = CarrierService.Builder.newInstance(service.getId(), service.getServiceLinkId(), 0)
					.setServiceDuration(newServiceDuration);
				CarrierService redrawnService = builder.setServiceStartingTimeWindow(service.getServiceStaringTimeWindow()).build();
				service.getAttributes().getAsMap().forEach((s, o) -> redrawnService.getAttributes().putAttribute(s, o));
				carrier.getServices().put(redrawnService.getId(), redrawnService);
				changedServiceDurations++;
			}
		return changedServiceDurations;
	}

	@Override
	public void tryToSolveAllCarriersCompletely(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers) {
		int startNumberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
		log.info("Starting with carrier-replanning loop.");

		for (Carrier carrier : nonCompleteSolvedCarriers) {
			// get the necessary attributes from a carrier which are not already saved in carrierAttributes (perhaps an existing carrier file was read)
			if (generator.getCarrierId2carrierAttributes().get(carrier.getId()) == null) {
				int purpose = carrier.getAttributes().getAttribute("purpose") == null ? 0 : Integer.parseInt(
					carrier.getAttributes().getAttribute("purpose").toString());
				String carrierId = carrier.getId().toString();
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType;
				String modeORvehType;
				if (carrier.getAttributes().getAttribute("subpopulation").toString().contains("commercialPersonTraffic")) {
					smallScaleCommercialTrafficType = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic;
					modeORvehType = "total";
				} else if (carrier.getAttributes().getAttribute("subpopulation").toString().contains("goodsTraffic")) {
					smallScaleCommercialTrafficType = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic;
					String[] split = carrierId.split("_");
					modeORvehType = split[split.length - 1];
				} else {
					log.warn("Carrier {} has no valid subpopulation. Skipping.", carrier.getId());
					continue;
				}
				VehicleSelection.OdMatrixEntryInformation odMatrixEntry = generator.vehicleSelection.getOdMatrixEntryInformation(purpose,
					modeORvehType, smallScaleCommercialTrafficType);
				String startZone = carrier.getAttributes().getAttribute("tourStartArea") == null ? "" : carrier.getAttributes().getAttribute(
					"tourStartArea").toString();
				SmallScaleCommercialTrafficUtils.StructuralAttribute selectedStartCategory = generator.getSelectedStartCategory(startZone, odMatrixEntry);
				GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes = new GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes(
					purpose, startZone, selectedStartCategory, modeORvehType,
					smallScaleCommercialTrafficType, null, odMatrixEntry);
				generator.getCarrierId2carrierAttributes().putIfAbsent(carrier.getId(), carrierAttributes);
			}
		}
		Path outputPath = Path.of(scenario.getConfig().controller().getOutputDirectory(),
			"analysis/freight/Carriers_SolvingLoop_stats.tsv");

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputPath.toString())) {
			// Write header only if the file is newly created
			if (Files.size(outputPath) == 0) {
				String[] header = {"iteration", "carriersWithUnhandledJobsBeforeLoopIteration", "carriersSolvedInIteration",
					"carriersNotSolvedInIteration", "addedVehicles", "changedServiceDurations", "additionalTravelBufferInMinutes"};
				JOIN.appendTo(writer, header);
				writer.newLine();
			}

			HashMap <Id<Carrier>, UnHandledInformation> unhandledInformationPerCarrier = new HashMap<>();
			for (int i = 1; i <= generator.getMaxNumberOfLoopsForVRPSolving(); i++) {
				log.info("carrier-replanning loop iteration {}. Solving {} carriers (of {} carriers) with unhandled jobs", i, nonCompleteSolvedCarriers.size(), CarriersUtils.getCarriers(scenario).getCarriers().size());
				int numberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
				int addedVehicles = 0;
				int changedServiceDurations = 0;
				int additionalTravelBufferInThisIterationInMinutes = (i) * generator.getAdditionalTravelBufferPerIterationInMinutes();
				for (Carrier nonCompleteSolvedCarrier : nonCompleteSolvedCarriers) {
					GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes =
						generator.getCarrierId2carrierAttributes().get(nonCompleteSolvedCarrier.getId());

					// get handeled and unhandled services of the old plan
					Set<CarrierService> handledServices = nonCompleteSolvedCarrier.getSelectedPlan().getScheduledTours().stream().flatMap(
						tour -> tour.getTour().getTourElements().stream()).filter(te -> te instanceof Tour.ServiceActivity).map(
						te -> ((Tour.ServiceActivity) te).getService()).collect(Collectors.toSet());
					List<CarrierService> unhandledServices = nonCompleteSolvedCarrier.getServices().values().stream().filter(
						thisService -> !handledServices.contains(thisService)).toList();

					// get used and unused vehicles of the old plan
					Set<Id<Vehicle>> usedVehicleIds = nonCompleteSolvedCarrier.getSelectedPlan().getScheduledTours().stream()
						.map(st -> st.getVehicle().getId())
						.collect(Collectors.toSet());
					List<CarrierVehicle> unusedVehicles = nonCompleteSolvedCarrier.getCarrierCapabilities().getCarrierVehicles().values().stream()
						.filter(v -> !usedVehicleIds.contains(v.getId()))
						.toList();

					log.info("Carrier '{}': {} unhandled services, {} handled services, {} unused vehicles, {} used vehicles.",
						nonCompleteSolvedCarrier.getId(), unhandledServices.size(), handledServices.size(), unusedVehicles.size(),
						usedVehicleIds.size());
					// Calculate time deficit (including additional buffer) of the unhandled services compared to the available vehicle tour durations
					double sumServiceDurationsWithBuffer = nonCompleteSolvedCarrier.getServices().values().stream().mapToDouble(
						CarrierService::getServiceDuration).sum() + Math.max(1,
						nonCompleteSolvedCarrier.getSelectedPlan().getScheduledTours().size()) * additionalTravelBufferInThisIterationInMinutes * 60;
					double sumMaxTourDurationsVehicles = nonCompleteSolvedCarrier.getCarrierCapabilities().getCarrierVehicles().values().stream().mapToDouble(
						vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()).sum();
					double timeDeficit = sumServiceDurationsWithBuffer - sumMaxTourDurationsVehicles;

					// calculate the maximum tour duration of the fleet to check if at least a single unhandled service could fit into the fleet availability
					double maxAnyVehicleAvailability = nonCompleteSolvedCarrier.getCarrierCapabilities()
						.getCarrierVehicles().values().stream()
						.mapToDouble(v -> v.getLatestEndTime() - v.getEarliestStartTime())
						.max().orElse(0);
					double maxUnhandledServiceDuration = unhandledServices.stream()
						.mapToDouble(CarrierService::getServiceDuration)
						.max().orElse(0);
					boolean anySingleJobInfeasible = maxUnhandledServiceDuration > maxAnyVehicleAvailability;

					// check if the situation is stagnating or even getting worse (more unhandled services than in the last loop iteration) to decide if service durations should be changed in this iteration
					UnHandledInformation lastLoopInformation = unhandledInformationPerCarrier.get(nonCompleteSolvedCarrier.getId());
					boolean stagnatingOrWorse = lastLoopInformation != null && unhandledServices.size() >= lastLoopInformation.numberOfUnhandledServices();

					// check if additional vehicles should be added: only if there is a time deficit, otherwise changing service durations would be enough to solve the carrier
					boolean checkAdditionalVehicles = (timeDeficit > 0);

					// check if service durations should be changed: if there is no time deficit, but at least a single job is infeasible with the current vehicle availabilities,
					// or if the situation is stagnating or getting worse (even with an additional buffer) to push the solution towards a better direction
					boolean checkServiceDurationChange = anySingleJobInfeasible || (timeDeficit <= 0 && stagnatingOrWorse);

					unhandledInformationPerCarrier.put(nonCompleteSolvedCarrier.getId(),
						new UnHandledInformation(unhandledServices.size(), unusedVehicles.size()));

					log.info(
						"Carrier '{}': timeDeficit={} min (service+buffer={} / vehicles={}), anySingleJobInfeasible={}, checkAddVehicles={}, checkRedrawServiceDur={}",
						nonCompleteSolvedCarrier.getId(),
						timeDeficit / 60.0, sumServiceDurationsWithBuffer / 60.0, sumMaxTourDurationsVehicles / 60.0,
						anySingleJobInfeasible, checkAdditionalVehicles, checkServiceDurationChange
					);
					// Add additional vehicles
					if (checkAdditionalVehicles)
						addedVehicles += addAdditionalVehicles(nonCompleteSolvedCarrier, carrierAttributes, unhandledServices, unusedVehicles,
							handledServices.size(), sumServiceDurationsWithBuffer, sumMaxTourDurationsVehicles);

					// change service durations of the services that could not be handelt within the vehicle availabilities
					if (checkServiceDurationChange)
						changedServiceDurations += redrawUnhandledServiceDurations(nonCompleteSolvedCarrier, carrierAttributes, unhandledServices);
					// Delete old plan of the carrier
					nonCompleteSolvedCarrier.clearPlans();
				}
				try {
					CarriersUtils.runJsprit(scenario, CarriersUtils.CarrierSelectionForSolution.solveOnlyForCarrierWithoutPlans);
				} catch (ExecutionException | InterruptedException e) {
					throw new RuntimeException(e);
				}

				nonCompleteSolvedCarriers = CarriersUtils.createListOfCarrierWithUnhandledJobs(CarriersUtils.getCarriers(scenario));

				// Write iteration results to file
				JOIN.appendTo(writer, new String[]{String.valueOf(i), String.valueOf(numberOfCarriersWithUnhandledJobs),
					String.valueOf(numberOfCarriersWithUnhandledJobs - nonCompleteSolvedCarriers.size()),
					String.valueOf(nonCompleteSolvedCarriers.size()),
					String.valueOf(addedVehicles), String.valueOf(changedServiceDurations),
					String.valueOf((i) * generator.getAdditionalTravelBufferPerIterationInMinutes())});
				writer.newLine();
				writer.flush();  // Ensure it's written immediately

				log.info(
					"End of carrier-replanning loop iteration: {}. From the {} carriers with unhandled jobs ({} already solved), {} were solved in this iteration with an additionalBuffer of {} minutes.",
					i, startNumberOfCarriersWithUnhandledJobs, startNumberOfCarriersWithUnhandledJobs - numberOfCarriersWithUnhandledJobs,
					numberOfCarriersWithUnhandledJobs - nonCompleteSolvedCarriers.size(),
					(i + 1) * generator.getAdditionalTravelBufferPerIterationInMinutes());

				if (i != 1) {
					try {
						Files.deleteIfExists(Path.of(
							scenario.getConfig().controller().getOutputDirectory(),
							scenario.getConfig().controller().getRunId() + ".output_carriers_notCompletelySolved_it_" + (i - 1) + ".xml.gz"));
					} catch (IOException e) {
						log.warn("Could not delete file: {}/{}.output_carriers_notCompletelySolved_it_{}.xml.gz",
							scenario.getConfig().controller().getOutputDirectory(), scenario.getConfig().controller().getRunId(), i);
					}
				}

				if (nonCompleteSolvedCarriers.isEmpty()) {
					Files.deleteIfExists(Path.of(
						scenario.getConfig().controller().getOutputDirectory(),
						scenario.getConfig().controller().getRunId() + ".output_carriers_notCompletelySolved_it_" + (i) + ".xml.gz"));
					break;
				}
				else
					CarriersUtils.writeCarriers(CarriersUtils.getCarriers(scenario),
						scenario.getConfig().controller().getOutputDirectory() + "/" + scenario.getConfig().controller().getRunId() + ".output_carriers_notCompletelySolved_it_" + i + ".xml.gz"
					);
			}

			if (!nonCompleteSolvedCarriers.isEmpty()) {
				log.warn("Not all services were handled!");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adds additional vehicles to the carrier until the sum of the maximum tour durations of the vehicles is higher than the sum of the service durations of the jobs (including the additional buffer).
	 *
	 * @return    number of added vehicles
	 */
	private int addAdditionalVehicles(Carrier nonCompleteSolvedCarrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes,
									  List<CarrierService> nonHandledJobs, List<CarrierVehicle> unusedVehicle, int handledServices,
									  double sumServiceDurationsJobs, double sumMaxTourDurationsVehicles) {
		CarrierCapabilities carrierCapabilities = nonCompleteSolvedCarrier.getCarrierCapabilities();
		int planedJobs = nonCompleteSolvedCarrier.getServices().size();
		int addedVehicles = 0;

		int runningIndex = carrierCapabilities.getCarrierVehicles().size();

		log.info("Carrier: {}, Number of non-used vehicles: {}. Number of non-handled jobs: {}. Number of handled services: {}.",
			nonCompleteSolvedCarrier.getId().toString(), unusedVehicle.size(), nonHandledJobs.size(), handledServices);

		double maxSingleUnhandledServiceDuration = nonHandledJobs.stream()
			.mapToDouble(CarrierService::getServiceDuration)
			.max().orElse(0);
		while (sumMaxTourDurationsVehicles < sumServiceDurationsJobs) {
			GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration t = null;
			int tries = 0;
			int tourDuration = 0;
			// Samples tour duration until long enough or tries expire
			while (tourDuration < maxSingleUnhandledServiceDuration * generator.getFactorForTravelBufferCalculation() && tries++ < 200) {
				t = generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficType()).sample();
				tourDuration = t.getVehicleTourDuration(this.rnd);
			}

			// Sets minimum tour duration if sampling fails
			if (tourDuration < maxSingleUnhandledServiceDuration * generator.getFactorForTravelBufferCalculation()) {
				tourDuration = (int) Math.ceil(maxSingleUnhandledServiceDuration);
				t = generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficType()).sample();
			}
			assert t != null;
			int vehicleStartTime = t.getVehicleStartTime(this.rnd);
			int vehicleEndTime = vehicleStartTime + tourDuration;
			Id<Link> linkId = generator.findPossibleLink(carrierAttributes.startZone(),
				carrierAttributes.selectedStartCategory(), null);
			for (VehicleType thisVehicleType : carrierCapabilities.getVehicleTypes()) { //TODO Flottenzusammensetzung anpassen. Momentan pro Depot alle Fahrzeugtypen 1x erzeugen
				runningIndex++;
				sumMaxTourDurationsVehicles += tourDuration;

				Id<Vehicle> vehcileId = Id.create(nonCompleteSolvedCarrier.getId().toString() + "_" + runningIndex, Vehicle.class);
				CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(vehcileId, linkId,
					thisVehicleType).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
				carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
				addedVehicles++;
				log.info("Added new vehicle '{}' to carrier '{}'", newCarrierVehicle.getId(), nonCompleteSolvedCarrier.getId());
			}
		}

		log.info("Added {} vehicles to carrier '{}' to handle {} non-handled jobs ({} planned, {} handled).",
			addedVehicles, nonCompleteSolvedCarrier.getId(), nonHandledJobs.size(), planedJobs, handledServices);
		nonCompleteSolvedCarrier.setCarrierCapabilities(carrierCapabilities);
		return addedVehicles;
	}

	record UnHandledInformation(int numberOfUnhandledServices, int numberOfUnusedVehicles) {}
}
