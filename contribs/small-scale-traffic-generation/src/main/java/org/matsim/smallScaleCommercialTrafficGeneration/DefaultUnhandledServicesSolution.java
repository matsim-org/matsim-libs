package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.common.base.Joiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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

public class DefaultUnhandledServicesSolution implements UnhandledServicesSolution {
	private static final Logger log = LogManager.getLogger(DefaultUnhandledServicesSolution.class);
	private static final Joiner JOIN = Joiner.on("\t");


	// Generation data
	Random rnd;
	private final GenerateSmallScaleCommercialTrafficDemand generator;

	DefaultUnhandledServicesSolution(GenerateSmallScaleCommercialTrafficDemand generator) {
		rnd = MatsimRandom.getRandom();
		this.generator = generator;
	}

	/**
	 * Redraws the service-durations of all {@link CarrierService}s of the given {@link Carrier}.
	 */
	private void redrawAllServiceDurations(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes,
										   int additionalTravelBufferPerIterationInMinutes) {
		for (CarrierService service : carrier.getServices().values()) {
			double newServiceDuration = generator.getServiceTimePerStop(carrier, carrierAttributes, additionalTravelBufferPerIterationInMinutes);
			CarrierService.Builder builder = CarrierService.Builder.newInstance(service.getId(), service.getServiceLinkId(), 0)
				.setServiceDuration(newServiceDuration);
			CarrierService redrawnService = builder.setServiceStartingTimeWindow(service.getServiceStaringTimeWindow()).build();
			carrier.getServices().put(redrawnService.getId(), redrawnService);
		}
	}

	@Override
	public void tryToSolveAllCarriersCompletely(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers) {
		int startNumberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
		log.info("Starting with carrier-replanning loop.");

		// get the needed attributes from the carrier without this data (perhaps an existing carrier file was read)
		for (Carrier carrier : nonCompleteSolvedCarriers) {
			if (generator.getCarrierId2carrierAttributes().get(carrier.getId()) == null) {
				int purpose = carrier.getAttributes().getAttribute("purpose") == null ? 0 : Integer.parseInt(
					carrier.getAttributes().getAttribute("purpose").toString());
				String carierId = carrier.getId().toString();
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType;
				String modeORvehType;
				if (carrier.getAttributes().getAttribute("subpopulation").toString().contains("commercialPersonTraffic")) {
					smallScaleCommercialTrafficType = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic;
					modeORvehType = "total";
				} else if (carrier.getAttributes().getAttribute("subpopulation").toString().contains("goodsTraffic")) {
					smallScaleCommercialTrafficType = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic;
					String[] split = carierId.split("_");
					modeORvehType = split[split.length - 1];
				} else {
					log.warn("Carrier {} has no valid subpopulation. Skipping.", carrier.getId());
					continue;
				}

				VehicleSelection.OdMatrixEntryInformation odMatrixEntry = generator.vehicleSelection.getOdMatrixEntryInformation(purpose,
					modeORvehType, smallScaleCommercialTrafficType);
				String startZone = carrier.getAttributes().getAttribute("tourStartArea") == null ? "" : carrier.getAttributes().getAttribute(
					"tourStartArea").toString();
				String selectedStartCategory = generator.getSelectedStartCategory(startZone, odMatrixEntry);
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
					"carriersNotSolvedInIteration", "additionalBufferInMinutes"};
				JOIN.appendTo(writer, header);
				writer.newLine();
			}

			for (int i = 1; i <= generator.getMaxReplanningIterations(); i++) {
				log.info("carrier-replanning loop iteration: {}", i);
				int numberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();

				for (Carrier nonCompleteSolvedCarrier : nonCompleteSolvedCarriers) {
					// Delete old plan of carrier
					nonCompleteSolvedCarrier.clearPlans();
					GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes =
						generator.getCarrierId2carrierAttributes().get(nonCompleteSolvedCarrier.getId());

					addAdditionalVehicles(nonCompleteSolvedCarrier, carrierAttributes);

					// Generate new services
//					redrawAllServiceDurations(nonCompleteSolvedCarrier, carrierAttributes,
//						(i) * generator.getAdditionalTravelBufferPerIterationInMinutes());
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

				if (nonCompleteSolvedCarriers.isEmpty()) break;
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

	private void addAdditionalVehicles(Carrier nonCompleteSolvedCarrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes) {
		CarrierCapabilities carrierCapabilities = nonCompleteSolvedCarrier.getCarrierCapabilities();
		log.info("Number of vehicles before adding new vehicles for carrier '{}': {}", nonCompleteSolvedCarrier.getId(),
			carrierCapabilities.getCarrierVehicles().size());

		int planedJobs = nonCompleteSolvedCarrier.getServices().size();
		int handledJobs;
		if (nonCompleteSolvedCarrier.getSelectedPlan() == null) {
			handledJobs = 0;
		} else
			handledJobs	= nonCompleteSolvedCarrier.getSelectedPlan().getScheduledTours().stream().mapToInt(
			tour -> (int) tour.getTour().getTourElements().stream().filter(element -> element instanceof Tour.ServiceActivity).count()).sum();
		int nonHandledJobs = planedJobs - handledJobs;
		int vehiclesToAdd = Math.max(1,	(int) Math.ceil(nonHandledJobs * generator.getFactorForNumberOfVehicles()));
		log.info("Adding {} vehicles to carrier '{}' to handle {} non-handled jobs ({} planned, {} handled).",
			vehiclesToAdd, nonCompleteSolvedCarrier.getId(), nonHandledJobs, planedJobs, handledJobs);
		int runningIndex = carrierCapabilities.getCarrierVehicles().size();

		for (int v = 0; v < vehiclesToAdd; v++) {
			GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration t = generator.getTourDistribution().get(
				carrierAttributes.smallScaleCommercialTrafficType()).sample();
			int vehicleStartTime = t.getVehicleStartTime(this.rnd);
			int tourDuration = t.getVehicleTourDuration(this.rnd);
			int vehicleEndTime = vehicleStartTime + tourDuration;
			String depot = carrierAttributes.vehicleDepots().get(this.rnd.nextInt(carrierAttributes.vehicleDepots().size()));
			for (VehicleType thisVehicleType : carrierCapabilities.getVehicleTypes()) { //TODO Flottenzusammensetzung anpassen. Momentan pro Depot alle Fahrzeugtypen 1x erzeugen
				runningIndex++;
				Id<Vehicle> vehcileId = Id.create(nonCompleteSolvedCarrier.getId().toString() + "_" + runningIndex, Vehicle.class);
				CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(vehcileId, Id.createLinkId(depot),
					thisVehicleType).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
				carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
				log.info("Added new vehicle '{}' to carrier '{}'", newCarrierVehicle.getId(), nonCompleteSolvedCarrier.getId());
			}
		}
		nonCompleteSolvedCarrier.setCarrierCapabilities(carrierCapabilities);
		log.info("Number of vehicles after adding new vehicles for carrier '{}': {}", nonCompleteSolvedCarrier.getId(),
			carrierCapabilities.getCarrierVehicles().size());
	}

	/**
	 * Change the service duration for a given carrier, because the service could not be handled in the last solution.
	 *
	 * @param carrier                                     The carrier for which we generate the serviceTime
	 * @param carrierAttributes                           attributes of the carrier to generate the service time for.
	 * @param key                                         key for the service duration
	 * @param additionalTravelBufferPerIterationInMinutes additional buffer for the travel time
	 * @return new service duration
	 */
	@Override
	public int changeServiceTimePerStop(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes,
										GenerateSmallScaleCommercialTrafficDemand.ServiceDurationPerCategoryKey key,
										int additionalTravelBufferPerIterationInMinutes) {

		double maxVehicleAvailability = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().mapToDouble(
			vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()).max().orElse(0);
		int usedTravelTimeBufferInSeconds = additionalTravelBufferPerIterationInMinutes * 60; // buffer for the driving time; for unsolved carriers the buffer will be increased over time
		CarrierVehicle newCarrierVehicle = null;
		for (int j = 0; j < 200; j++) {
			if (generator.getServiceDurationTimeSelector().get(key) == null) {
				System.out.println("key: " + key);
				System.out.println(generator.getServiceDurationTimeSelector().keySet());
				throw new RuntimeException(
					"No service duration found for employee category '" + carrierAttributes.selectedStartCategory() + "' and mode '"
						+ carrierAttributes.modeORvehType() + "' in traffic type '" + carrierAttributes.smallScaleCommercialTrafficType() + "'");
			}
			GenerateSmallScaleCommercialTrafficDemand.DurationsBounds serviceDurationBounds = generator.getServiceDurationTimeSelector().get(
				key).sample();

			for (int i = 0; i < 10; i++) {
				int serviceDurationLowerBound = serviceDurationBounds.minDuration();
				int serviceDurationUpperBound = serviceDurationBounds.maxDuration();
				int possibleValue = rnd.nextInt(serviceDurationLowerBound * 60, serviceDurationUpperBound * 60);
				// checks if the service duration will not exceed the vehicle availability including the buffer
				if (possibleValue + usedTravelTimeBufferInSeconds <= maxVehicleAvailability) {
					if (newCarrierVehicle != null)
						log.info("New maxVehicleAvailability of vehicle '{}' of carrier '{}': {}", newCarrierVehicle.getId(), carrier.getId(),
							maxVehicleAvailability);
					else
						log.info("Changed service duration for carrier '{}' to fit vehicle duration with usedTravelTimeBufferInMinutes {}.",
							carrier.getId(), additionalTravelBufferPerIterationInMinutes);
					return possibleValue;
				}
			}
			if (j > 100) {
				CarrierVehicle carrierVehicleToChange = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().sorted(
					Comparator.comparingDouble(vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime())).toList().getFirst();
				int tourDuration = 0;
				int vehicleStartTime = 0;
				int vehicleEndTime = 0;
				while (tourDuration < maxVehicleAvailability) {
					GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration t = generator.getTourDistribution().get(
						carrierAttributes.smallScaleCommercialTrafficType()).sample();
					vehicleStartTime = t.getVehicleStartTime(rnd);
					tourDuration = t.getVehicleTourDuration(rnd);
					vehicleEndTime = vehicleStartTime + tourDuration;
				}
				newCarrierVehicle = CarrierVehicle.Builder.newInstance(carrierVehicleToChange.getId(), carrierVehicleToChange.getLinkId(),
					carrierVehicleToChange.getType()).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
				carrier.getCarrierCapabilities().getCarrierVehicles().remove(carrierVehicleToChange.getId());
				carrier.getCarrierCapabilities().getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
				maxVehicleAvailability = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().mapToDouble(
					vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()).max().orElse(0);
			}
		}

		throw new RuntimeException(
			"No possible service duration found for employee category '" + carrierAttributes.selectedStartCategory() + "' and mode '"
				+ carrierAttributes.modeORvehType() + "' in traffic type '" + carrierAttributes.smallScaleCommercialTrafficType() + "'");
	}
}
