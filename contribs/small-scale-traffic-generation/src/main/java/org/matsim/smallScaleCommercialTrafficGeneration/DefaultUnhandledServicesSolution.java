package org.matsim.smallScaleCommercialTrafficGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.CarriersUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class DefaultUnhandledServicesSolution implements UnhandledServicesSolution {
	private static final Logger log = LogManager.getLogger(DefaultUnhandledServicesSolution.class);

	// Generation data
	Random rnd;
	private final GenerateSmallScaleCommercialTrafficDemand generator;

	DefaultUnhandledServicesSolution(GenerateSmallScaleCommercialTrafficDemand generator){
		rnd = MatsimRandom.getRandom();
		this.generator = generator;
	}

	public List<Carrier> createListOfCarrierWithUnhandledJobs(Scenario scenario){
		List<Carrier> carriersWithUnhandledJobs = new LinkedList<>();
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			if (!CarriersUtils.allJobsHandledBySelectedPlan(carrier))
				carriersWithUnhandledJobs.add(carrier);
		}

		return carriersWithUnhandledJobs;
	}

	public int getServiceTimePerStop(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes, int additionalTravelBufferPerIterationInMinutes) {
		GenerateSmallScaleCommercialTrafficDemand.ServiceDurationPerCategoryKey key = null;
		if (carrierAttributes.smallScaleCommercialTrafficType().equals(
			GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()))
			key = GenerateSmallScaleCommercialTrafficDemand.makeServiceDurationPerCategoryKey(carrierAttributes.selectedStartCategory(), null, carrierAttributes.smallScaleCommercialTrafficType());
		else if (carrierAttributes.smallScaleCommercialTrafficType().equals(
			GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			key = GenerateSmallScaleCommercialTrafficDemand.makeServiceDurationPerCategoryKey(carrierAttributes.selectedStartCategory(), carrierAttributes.modeORvehType(), carrierAttributes.smallScaleCommercialTrafficType());
		}

		//possible new Version by Ricardo
		double maxVehicleAvailability = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().mapToDouble(vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()).max().orElse(0);
		int usedTravelTimeBuffer = additionalTravelBufferPerIterationInMinutes * 60; // buffer for the driving time; for unsolved carriers the buffer will be increased over time
		for (int j = 0; j < 200; j++) {
			GenerateSmallScaleCommercialTrafficDemand.DurationsBounds serviceDurationBounds = generator.getServiceDurationTimeSelector().get(key).sample();

			for (int i = 0; i < 10; i++) {
				int serviceDurationLowerBound = serviceDurationBounds.minDuration();
				int serviceDurationUpperBound = serviceDurationBounds.maxDuration();
				int possibleValue = rnd.nextInt(serviceDurationLowerBound * 60, serviceDurationUpperBound * 60);
				// checks if the service duration will not exceed the vehicle availability including the buffer
				if (possibleValue + usedTravelTimeBuffer <= maxVehicleAvailability)
					return possibleValue;
			}
			if (j > 100){
				CarrierVehicle carrierVehicleToChange = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().sorted(Comparator.comparingDouble(vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime())).toList().getFirst();
				log.info("Changing vehicle availability for carrier {}. Old maxVehicleAvailability: {}", carrier.getId(), maxVehicleAvailability);
				int tourDuration = 0;
				int vehicleStartTime = 0;
				int vehicleEndTime = 0;
				while (tourDuration < maxVehicleAvailability) {
					GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration t = generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficType()).sample();
					vehicleStartTime = t.getVehicleStartTime();
					tourDuration = t.getVehicleTourDuration();
					vehicleEndTime = vehicleStartTime + tourDuration;
				}
				CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(carrierVehicleToChange.getId(), carrierVehicleToChange.getLinkId(),
					carrierVehicleToChange.getType()).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
				carrier.getCarrierCapabilities().getCarrierVehicles().remove(carrierVehicleToChange.getId());
				carrier.getCarrierCapabilities().getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
				maxVehicleAvailability = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().mapToDouble(vehicle -> vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()).max().orElse(0);
				log.info("New maxVehicleAvailability: {}", maxVehicleAvailability);
			}
		}

		throw new RuntimeException("No possible service duration found for employee category '" + carrierAttributes.selectedStartCategory() + "' and mode '"
			+ carrierAttributes.modeORvehType() + "' in traffic type '" + carrierAttributes.smallScaleCommercialTrafficType() + "'");
	}

	/**
	 * Redraws the service-durations of all {@link CarrierService}s of the given {@link Carrier}.
	 */
	private void redrawAllServiceDurations(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes, int additionalTravelBufferPerIterationInMinutes) {
		for (CarrierService service : carrier.getServices().values()) {
			double newServiceDuration = getServiceTimePerStop(carrier, carrierAttributes, additionalTravelBufferPerIterationInMinutes);
			CarrierService redrawnService = CarrierService.Builder.newInstance(service.getId(), service.getLocationLinkId())
				.setServiceDuration(newServiceDuration).setServiceStartTimeWindow(service.getServiceStartTimeWindow()).build();
			carrier.getServices().put(redrawnService.getId(), redrawnService);
		}
	}

	@Override
	public void tryToSolveAllCarriersCompletely(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers) {
		int startNumberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
		log.info("Starting with carrier-replanning loop.");
		for (int i = 0; i < generator.getMaxReplanningIterations(); i++) {
			log.info("carrier-replanning loop iteration: {}", i);
			int numberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
			for (Carrier nonCompleteSolvedCarrier : nonCompleteSolvedCarriers) {
				//Delete old plan of carrier
				nonCompleteSolvedCarrier.clearPlans();
				nonCompleteSolvedCarrier.setSelectedPlan(null);
				GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes = generator.getCarrierId2carrierAttributes().get(nonCompleteSolvedCarrier.getId());

				// Generate new services. The new service batch should have a smaller sum of serviceDurations than before (or otherwise it will not change anything)
				redrawAllServiceDurations(nonCompleteSolvedCarrier, carrierAttributes, (i + 1) * generator.getAdditionalTravelBufferPerIterationInMinutes());
				log.info("Carrier should be changed...");
			}
			try {
				CarriersUtils.runJsprit(scenario, CarriersUtils.CarrierSelectionForSolution.solveOnlyForCarrierWithoutPlans);
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(e);
			}


			nonCompleteSolvedCarriers = createListOfCarrierWithUnhandledJobs(scenario);
			log.info(
				"End of carrier-replanning loop iteration: {}. From the {} carriers with unhandled jobs ({} already solved), {} were solved in this iteration with an additionalBuffer of {} minutes.",
				i, startNumberOfCarriersWithUnhandledJobs, startNumberOfCarriersWithUnhandledJobs - numberOfCarriersWithUnhandledJobs,
				numberOfCarriersWithUnhandledJobs - nonCompleteSolvedCarriers.size(), (i + 1) * generator.getAdditionalTravelBufferPerIterationInMinutes());
			if (nonCompleteSolvedCarriers.isEmpty()) break;
		}

		// Final check
		if (!nonCompleteSolvedCarriers.isEmpty()) {
			log.warn("Not all services were handled!");
		}
	}

}