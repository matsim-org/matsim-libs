package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.common.base.Joiner;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.conventions.vsp.SubpopulationDefaultNames;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.TimeDependentNetwork;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.PURPOSE;
import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.SUBPOPULATION;
import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.TOUR_START_AREA;

class DefaultUnhandledServicesSolution implements UnhandledServicesSolution {
	private static final Logger log = LogManager.getLogger(DefaultUnhandledServicesSolution.class);
	private static final Joiner JOIN = Joiner.on("\t");
	/**
	 * Minimum service duration in seconds used when a stagnating unhandled service is shortened.
	 */
	private static final double MIN_STAGNATION_SERVICE_DURATION = 60.;
	/**
	 * Extra time in seconds added to dedicated fallback vehicle windows so jsprit does not reject borderline timings.
	 */
	private static final double TIME_WINDOW_FALLBACK_SLACK = 30. * 60.;
	/**
	 * Maximum availability in seconds for newly created fallback vehicles.
	 */
	private static final double MAX_FALLBACK_VEHICLE_AVAILABILITY = 18. * 3600.;
	/**
	 * Number of attempts to sample a feasible fallback vehicle start time before using the latest feasible start.
	 */
	private static final int FALLBACK_START_SAMPLE_TRIES = 200;

	Random rnd;
	private final GenerateSmallScaleCommercialTrafficDemand generator;
	private final PreReplanningVehicleAddition preReplanningVehicleAddition;
	private final Predicate<VehicleType> vehicleTypeFallbackFilter;

	/**
	 * Creates the default repair strategy without specialized iteration hooks or vehicle-type filtering.
	 *
	 * @param generator demand generator that provides configuration, random distributions, and carrier attributes
	 */
	DefaultUnhandledServicesSolution(GenerateSmallScaleCommercialTrafficDemand generator) {
		this(generator, (_, _, _) -> 0, _ -> true);
	}

	/**
	 * Creates the repair strategy with optional specialized vehicle repairs and fallback vehicle-type filtering.
	 *
	 * @param generator demand generator that provides configuration, random distributions, and carrier attributes
	 * @param preReplanningVehicleAddition optional hook for adding specialized fallback vehicles before replanning
	 * @param vehicleTypeFallbackFilter filter deciding which vehicle types may be used for fallback vehicles
	 */
	DefaultUnhandledServicesSolution(GenerateSmallScaleCommercialTrafficDemand generator, PreReplanningVehicleAddition preReplanningVehicleAddition,
	                                 Predicate<VehicleType> vehicleTypeFallbackFilter) {
		rnd = MatsimRandom.getRandom();
		this.generator = generator;
		this.preReplanningVehicleAddition = Objects.requireNonNull(preReplanningVehicleAddition);
		this.vehicleTypeFallbackFilter = Objects.requireNonNull(vehicleTypeFallbackFilter);
	}

	/**
	 * Redraws the service-durations of these {@link CarrierService}s of the given {@link Carrier} which are not possible to handle with the current vehicle availabilities.
	 * Stagnating unhandled services can also be redrawn even if they already fit into a vehicle availability.
	 * This gives jsprit a changed input in cases where the time budget looks sufficient, but the same service remains unhandled in multiple replanning loops.
	 * The service durations are redrawn until they fit into the vehicle availability, including the effective buffer for the travel time.
	 *
	 * @param carrier carrier whose service durations may be replaced
	 * @param carrierAttributes attributes used for drawing replacement service durations
	 * @param unhandledServices services that remained unhandled after the previous jsprit run
	 * @param effectiveTravelBufferFactor travel-buffer factor used in the current replanning loop
	 * @param redrawFeasibleServices whether services that already fit into a vehicle window may still be shortened
	 * @return number of services whose duration was changed
	 */
	private int redrawUnhandledServiceDurations(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes, List<CarrierService> unhandledServices,
	                                            double effectiveTravelBufferFactor, boolean redrawFeasibleServices) {

		int changedServiceDurations = 0;
		double maxVehicleAvailability = maxVehicleAvailability(carrier.getCarrierCapabilities().getCarrierVehicles().values());
		double maxServiceDuration = maxVehicleAvailability / effectiveTravelBufferFactor;
		for (CarrierService service : unhandledServices) {
			log.debug("Carrier '{}': max vehicle availability (unused vehicles) is {} minutes. Service '{}' has a duration of {} minutes. Effective travel-buffer factor is {}.",
				carrier.getId(), maxVehicleAvailability / 60, service.getId(), service.getServiceDuration() / 60, effectiveTravelBufferFactor);
			double newServiceDuration = service.getServiceDuration();
			boolean serviceFitsCurrentAvailability = newServiceDuration <= maxServiceDuration;
			if (serviceFitsCurrentAvailability && !redrawFeasibleServices) {
				continue;
			}
			if (serviceFitsCurrentAvailability && service.getServiceDuration() <= MIN_STAGNATION_SERVICE_DURATION) {
				log.debug("Carrier '{}': Service '{}' is already at or below the minimum stagnation-redraw duration of {} minutes. Keeping {} minutes.",
					carrier.getId(), service.getId(), MIN_STAGNATION_SERVICE_DURATION / 60., service.getServiceDuration() / 60.);
				continue;
			}

			// If the service already fits but the carrier is stuck, prefer a shorter draw.
			// Otherwise the redraw might leave the insertion problem unchanged or even harder.
			int tries = 0;
			while ((newServiceDuration > maxServiceDuration
				|| Double.compare(newServiceDuration, service.getServiceDuration()) == 0
				|| (serviceFitsCurrentAvailability && newServiceDuration >= service.getServiceDuration())) && tries++ < 200) {
				newServiceDuration = generator.getServiceTimePerStop(carrierAttributes);
			}
			if (newServiceDuration > maxServiceDuration || (serviceFitsCurrentAvailability && newServiceDuration >= service.getServiceDuration())) {
				double feasibleServiceDuration = serviceFitsCurrentAvailability
					? Math.clamp(service.getServiceDuration() * 0.9, MIN_STAGNATION_SERVICE_DURATION, maxServiceDuration)
					: maxServiceDuration;
				log.warn(
					"Carrier '{}': Could not redraw service '{}' into the effective travel-buffer factor {} after {} tries. Clipping service duration from {} to {} minutes.",
					carrier.getId(), service.getId(), effectiveTravelBufferFactor, tries, service.getServiceDuration() / 60.,
					feasibleServiceDuration / 60.);
				newServiceDuration = feasibleServiceDuration;
			}
			if (Double.compare(newServiceDuration, service.getServiceDuration()) == 0) {
				log.debug("Carrier '{}': Service '{}' kept its duration of {} minutes after {} redraw tries.",
					carrier.getId(), service.getId(), service.getServiceDuration() / 60., tries);
				continue;
			}
			if (serviceFitsCurrentAvailability) {
				log.info(
					"Carrier '{}': Redrew feasible but still unhandled service '{}' from {} to {} minutes because the carrier stagnated in the replanning loop.",
					carrier.getId(), service.getId(), service.getServiceDuration() / 60., newServiceDuration / 60.);
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

	/**
	 * Repairs all carriers with unhandled jobs by iteratively adding vehicles, redrawing service durations, and replanning.
	 *
	 * @param scenario scenario containing the carriers and config used for replanning
	 * @param nonCompleteSolvedCarriers carriers that still have unhandled jobs after the previous jsprit run
	 */
	@Override
	public void tryToSolveAllCarriersCompletely(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers) {
		int startNumberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
		log.info("Starting with carrier-replanning loop.");

		for (Carrier carrier : nonCompleteSolvedCarriers) {
			generator.getCarrierId2carrierAttributes().computeIfAbsent(carrier.getId(), _ -> createCarrierAttributes(carrier));
		}
		Path outputPath = Path.of(scenario.getConfig().controller().getOutputDirectory(),
			"analysis/freight/Carriers_SolvingLoop_stats.tsv");

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputPath.toString())) {
			// Write header only if the file is newly created
			if (Files.size(outputPath) == 0) {
				String[] header = {"iteration", "carriersWithUnhandledJobsBeforeLoopIteration", "carriersSolvedInIteration",
					"carriersNotSolvedInIteration", "addedVehicles", "changedServiceDurations", "effectiveTravelBufferFactor", "calculationTimeInHH:MM:SS"};
				JOIN.appendTo(writer, header);
				writer.newLine();
			}

			HashMap <Id<Carrier>, UnHandledInformation> unhandledInformationPerCarrier = new HashMap<>();
			for (int i = 1; i <= generator.getMaxNumberOfLoopsForVRPSolving(); i++) {
				double start = System.currentTimeMillis();
				log.info("carrier-replanning loop iteration {}. Solving {} carriers (of {} carriers) with unhandled jobs", i, nonCompleteSolvedCarriers.size(), CarriersUtils.getCarriers(scenario).getCarriers().size());
				int numberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
				int addedVehicles = 0;
				int changedServiceDurations = 0;
				double effectiveTravelBufferFactor = effectiveTravelBufferFactor(i);
				Set<VehicleType> vehicleTypes = nonCompleteSolvedCarriers.stream()
					.flatMap(carrier -> carrier.getCarrierCapabilities().getVehicleTypes().stream())
					.collect(Collectors.toSet());
				NetworkBasedTransportCosts transportCosts = createNetworkBasedTransportCosts(scenario, vehicleTypes);
				Set<Id<Carrier>> carriersWithForcedSingleServiceRedraw = new HashSet<>();
				for (Carrier nonCompleteSolvedCarrier : nonCompleteSolvedCarriers) {
					GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes =
						generator.getCarrierId2carrierAttributes().get(nonCompleteSolvedCarrier.getId());

					// get handeled and unhandled services of the old plan
					Set<CarrierService> handledServices = nonCompleteSolvedCarrier.getSelectedPlan().getScheduledTours().stream().flatMap(
						tour -> tour.getTour().getTourElements().stream()).filter(te -> te instanceof Tour.ServiceActivity).map(
						te -> ((Tour.ServiceActivity) te).getService()).collect(Collectors.toSet());
					List<CarrierService> unhandledServices = nonCompleteSolvedCarrier.getServices().values().stream().filter(
						thisService -> !handledServices.contains(thisService)).toList();
					Set<Id<CarrierService>> unhandledServiceIds = unhandledServices.stream()
						.map(CarrierService::getId)
						.collect(Collectors.toSet());

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
					// Calculate the time deficit of the services compared to the available vehicle tour durations.
					double sumServiceDurationsWithoutTravelBuffer = nonCompleteSolvedCarrier.getServices().values().stream().mapToDouble(CarrierService::getServiceDuration).sum();
					double sumServiceDurationsWithBuffer = sumServiceDurationsWithoutTravelBuffer * effectiveTravelBufferFactor;
					double sumMaxTourDurationsVehicles = nonCompleteSolvedCarrier.getCarrierCapabilities().getCarrierVehicles().values().stream().mapToDouble(DefaultUnhandledServicesSolution::vehicleAvailability).sum();
					double timeDeficit = sumServiceDurationsWithBuffer - sumMaxTourDurationsVehicles;
					double timeDeficitWithoutTravelBuffer = sumServiceDurationsWithoutTravelBuffer - sumMaxTourDurationsVehicles;

					// calculate the maximum tour duration of the fleet to check if at least a single unhandled service could fit into the fleet availability
					double maxAnyVehicleAvailability = maxVehicleAvailability(
						nonCompleteSolvedCarrier.getCarrierCapabilities().getCarrierVehicles().values());
					double maxUnhandledServiceDurationWithEffectiveBuffer = unhandledServices.stream()
						.mapToDouble(service -> service.getServiceDuration() * effectiveTravelBufferFactor)
						.max().orElse(0);
					boolean anySingleJobInfeasible = maxUnhandledServiceDurationWithEffectiveBuffer > maxAnyVehicleAvailability;

					// check if the situation is stagnating or even getting worse (more unhandled services than in the last loop iteration) to decide if service durations should be changed in this iteration
					UnHandledInformation lastLoopInformation = unhandledInformationPerCarrier.get(nonCompleteSolvedCarrier.getId());
					boolean stagnatingOrWorse = lastLoopInformation != null && unhandledServices.size() >= lastLoopInformation.numberOfUnhandledServices();
					boolean unhandledServiceSetChanged = lastLoopInformation != null && !unhandledServiceIds.equals(lastLoopInformation.unhandledServiceIds());
					int previousStagnationVehicleFallbackSets =
						lastLoopInformation == null ? 0 : lastLoopInformation.stagnationVehicleFallbackSets();
					int previousStagnationVehicleFallbackSetLimit =
						lastLoopInformation == null ? 0 : lastLoopInformation.stagnationVehicleFallbackSetLimit();
					boolean forceRedrawAfterSingleServiceFallback = lastLoopInformation != null
						&& lastLoopInformation.numberOfUnhandledServices() == 1
						&& lastLoopInformation.singleServiceVehicleAdded();
					boolean forceFinalLoopRepair = i == generator.getMaxNumberOfLoopsForVRPSolving()
						&& !unhandledServices.isEmpty();
					if (forceRedrawAfterSingleServiceFallback) {
						carriersWithForcedSingleServiceRedraw.add(nonCompleteSolvedCarrier.getId());
					}

					// A real positive time deficit means the fleet needs more available vehicle time. The growing travel-buffer
					// factor is intentionally softer: if that factor alone creates the deficit, add at most one vehicle-type set
					// and only while there are fewer unused vehicles than still-open services. This prevents late loop iterations
					// from manufacturing many vehicles just to satisfy a conservative buffer estimate.
					boolean aggregateServiceTimeDeficit = timeDeficitWithoutTravelBuffer > 0;
					boolean rawTimeBudgetSufficient = !aggregateServiceTimeDeficit;

					// Redraw service durations if an unhandled service is individually too long for the available vehicle windows.
					// Also redraw when a carrier stagnates, although the raw aggregate time budget is sufficient. This intentionally
					// ignores the growing travel-buffer factor: otherwise the large late-loop buffer can hide stagnation and the loop
					// stops repairing carriers even though the actual service time would fit into the available vehicle windows.
					boolean redrawDueToStagnation = rawTimeBudgetSufficient && stagnatingOrWorse;
					boolean checkServiceDurationChange = anySingleJobInfeasible || redrawDueToStagnation
						|| forceRedrawAfterSingleServiceFallback || forceFinalLoopRepair;

					boolean addVehiclesForBufferOnlyDeficit = !aggregateServiceTimeDeficit && timeDeficit > 0
						&& unusedVehicles.size() < unhandledServices.size();

					// Carriers can also stagnate with unused vehicles even when the aggregate time budget is sufficient.
					// In those cases the remaining vehicles may be unusable for the last service because of depot or
					// time-window constraints. Prefer capped normal fallback vehicles over changing service durations,
					// but keep the number of fallback attempts bounded. If replanning changes which service is unhandled,
					// allow another bounded set of vehicle attempts for the newly exposed service.
					int stagnationVehicleFallbackSetLimit = previousStagnationVehicleFallbackSetLimit;
					int fallbackSetLimitIncrement = Math.max(unhandledServices.size(),
						lastLoopInformation == null ? unhandledServices.size() : lastLoopInformation.numberOfUnhandledServices());
					if (checkServiceDurationChange && stagnationVehicleFallbackSetLimit == 0) {
						stagnationVehicleFallbackSetLimit = fallbackSetLimitIncrement;
					} else if (checkServiceDurationChange && unhandledServiceSetChanged
						&& previousStagnationVehicleFallbackSets >= stagnationVehicleFallbackSetLimit) {
						stagnationVehicleFallbackSetLimit += fallbackSetLimitIncrement;
					}
					if ((forceRedrawAfterSingleServiceFallback || forceFinalLoopRepair)
						&& previousStagnationVehicleFallbackSets >= stagnationVehicleFallbackSetLimit) {
						stagnationVehicleFallbackSetLimit++;
					}
					// The network-based timing already includes the NetworkChangeEvents. Dedicated time-window fallback
					// vehicles receive a fixed 30-minute slack; the loop-growing factor remains limited to the aggregate fleet repair.
					List<CarrierService> servicesWithoutUnusedTimeWindow = findServicesWithoutUnusedVehicleTimeWindow(
						unhandledServices, unusedVehicles, transportCosts);
					boolean addTimeWindowFallbackBeforeRedraw = checkServiceDurationChange
						&& !forceRedrawAfterSingleServiceFallback
						&& previousStagnationVehicleFallbackSets < stagnationVehicleFallbackSetLimit
						&& !servicesWithoutUnusedTimeWindow.isEmpty();
					boolean checkAdditionalVehicles = !forceRedrawAfterSingleServiceFallback
						&& (aggregateServiceTimeDeficit || addVehiclesForBufferOnlyDeficit);
					int stagnationVehicleFallbackSets = previousStagnationVehicleFallbackSets + (addTimeWindowFallbackBeforeRedraw ? 1 : 0);
					double vehicleAdditionTargetDuration = aggregateServiceTimeDeficit
						? sumServiceDurationsWithoutTravelBuffer
						: sumServiceDurationsWithBuffer;
					int maxVehicleSetsToAdd = aggregateServiceTimeDeficit ? Integer.MAX_VALUE : 1;

					log.info(
						"Carrier '{}': deficits buffered/raw={} / {} min, bufferFactor={}, longestOpenServiceWithBuffer={} min, servicesWithoutUnusedVehicle={}, repairs(addVehicles={}, timeWindowFallback={}, redrawDurations={})",
						nonCompleteSolvedCarrier.getId(),
						timeDeficit / 60.0, timeDeficitWithoutTravelBuffer / 60.0,
						effectiveTravelBufferFactor, maxUnhandledServiceDurationWithEffectiveBuffer / 60.,
						servicesWithoutUnusedTimeWindow.size(),
						checkAdditionalVehicles, addTimeWindowFallbackBeforeRedraw, checkServiceDurationChange
					);
					int addedVehiclesForCarrier = 0;
					if (checkAdditionalVehicles)
						addedVehiclesForCarrier += addAdditionalVehicles(nonCompleteSolvedCarrier, carrierAttributes, unhandledServices, unusedVehicles,
							handledServices.size(), vehicleAdditionTargetDuration, sumMaxTourDurationsVehicles, effectiveTravelBufferFactor,
							maxVehicleSetsToAdd);
					if (addTimeWindowFallbackBeforeRedraw) {
						int addedTimeWindowFallbackVehicles = addTimeWindowFallbackVehicles(nonCompleteSolvedCarrier, carrierAttributes,
							servicesWithoutUnusedTimeWindow, transportCosts, effectiveTravelBufferFactor);
						addedVehiclesForCarrier += addedTimeWindowFallbackVehicles;
						if (addedTimeWindowFallbackVehicles == 0)
							stagnationVehicleFallbackSets = previousStagnationVehicleFallbackSets;
					}
					addedVehicles += addedVehiclesForCarrier;
					boolean singleServiceVehicleAddedThisLoop = unhandledServices.size() == 1
						&& addedVehiclesForCarrier > 0;

					// If this loop changed the fleet, let jsprit try the capped fallback vehicles before altering service
					// durations. If a carrier remains unresolved after adding a vehicle for one open service, redraw all jobs
					// that are open in the next loop without adding another vehicle; jsprit may have shifted the open set.
					// A subsequent loop may add a vehicle again, alternating both repair strategies while the carrier stagnates.
					// In the final loop, apply both repairs to all open services because no alternating follow-up remains.
					if (checkServiceDurationChange && (addedVehiclesForCarrier == 0 || forceRedrawAfterSingleServiceFallback
						|| forceFinalLoopRepair))
						changedServiceDurations += redrawUnhandledServiceDurations(nonCompleteSolvedCarrier, carrierAttributes, unhandledServices,
							effectiveTravelBufferFactor, redrawDueToStagnation || forceRedrawAfterSingleServiceFallback
								|| forceFinalLoopRepair);
					else if (checkServiceDurationChange) {
						log.info(
							"Carrier '{}': Deferred service-duration redraw because {} capped fallback vehicle(s) were added in this loop.",
							nonCompleteSolvedCarrier.getId(), addedVehiclesForCarrier);
					}
					unhandledInformationPerCarrier.put(nonCompleteSolvedCarrier.getId(),
						new UnHandledInformation(unhandledServices.size(), unusedVehicles.size(),
							stagnationVehicleFallbackSets, stagnationVehicleFallbackSetLimit, unhandledServiceIds,
							singleServiceVehicleAddedThisLoop));
				}
				// Specialized repair strategies, for example range-aware fallbacks, can inspect the updated fleet here.
				// The selected plans are still available, so implementations can distinguish used from currently free
				// vehicles. The old plans are cleared only afterwards to force jsprit to solve the adjusted carriers.
				List<Carrier> carriersForPreReplanningVehicleAddition = nonCompleteSolvedCarriers.stream()
					.filter(carrier -> !carriersWithForcedSingleServiceRedraw.contains(carrier.getId()))
					.toList();
				addedVehicles += preReplanningVehicleAddition.addVehiclesBeforeReplanning(scenario, carriersForPreReplanningVehicleAddition,
					effectiveTravelBufferFactor);
				nonCompleteSolvedCarriers.forEach(Carrier::clearPlans);
				try {
					CarriersUtils.runJsprit(scenario, CarriersUtils.CarrierSelectionForSolution.solveOnlyForCarrierWithoutPlans);
				} catch (ExecutionException | InterruptedException e) {
					throw new RuntimeException(e);
				}

				nonCompleteSolvedCarriers = CarriersUtils.createListOfCarrierWithUnhandledJobs(CarriersUtils.getCarriers(scenario));
				String timeForThisLoop = Time.writeTime((System.currentTimeMillis() - start) / 1000, Time.TIMEFORMAT_HHMMSS);

				// Write iteration results to file
				JOIN.appendTo(writer, new String[]{String.valueOf(i), String.valueOf(numberOfCarriersWithUnhandledJobs),
					String.valueOf(numberOfCarriersWithUnhandledJobs - nonCompleteSolvedCarriers.size()),
					String.valueOf(nonCompleteSolvedCarriers.size()),
					String.valueOf(addedVehicles), String.valueOf(changedServiceDurations),
					String.valueOf(effectiveTravelBufferFactor),
					timeForThisLoop});
				writer.newLine();
				writer.flush();  // Ensure it's written immediately

				log.info(
					"End of carrier-replanning loop iteration: {}. From the {} carriers with unhandled jobs ({} already solved), {} were solved in this iteration with an effective travel-buffer factor of {}.",
					i, startNumberOfCarriersWithUnhandledJobs, startNumberOfCarriersWithUnhandledJobs - numberOfCarriersWithUnhandledJobs,
					numberOfCarriersWithUnhandledJobs - nonCompleteSolvedCarriers.size(),
					effectiveTravelBufferFactor);

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
	 * Reconstructs carrier attributes when carriers were read from an existing file instead of being created by the generator.
	 */
	private GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes createCarrierAttributes(Carrier carrier) {
		// get the necessary attributes from a carrier which are not already saved in carrierAttributes (perhaps an existing carrier file was read)
		int purpose = carrier.getAttributes().getAttribute(PURPOSE) == null ? 0 : Integer.parseInt(
				carrier.getAttributes().getAttribute(PURPOSE).toString());
		String carrierId = carrier.getId().toString();
		String subpopulation = Objects.requireNonNull(carrier.getAttributes().getAttribute(SUBPOPULATION),
			"Carrier " + carrier.getId() + " has no subpopulation.").toString();
		GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment smallScaleCommercialTrafficSegment;
		String modeORvehType;
		if (subpopulation.equals(SubpopulationDefaultNames.SUBPOP_COM_PERSON)) {
			smallScaleCommercialTrafficSegment = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment.commercialPersonTraffic;
			modeORvehType = "total";
		} else if (subpopulation.equals(SubpopulationDefaultNames.SUBPOP_GOODS)) {
			smallScaleCommercialTrafficSegment = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment.goodsTraffic;
			String[] split = carrierId.split("vehTyp")[1].split("_"); //TODO make this via attributes
			modeORvehType = "vehTyp" + split[0];
		} else {
			throw new IllegalArgumentException("Carrier " + carrier.getId() + " has no valid subpopulation: " + subpopulation);
		}
		OdMatrixEntryInformationProvider.OdMatrixEntryInformation odMatrixEntry = generator.odMatrixEntryInformationProvider.getOdMatrixEntryInformation(
				purpose,
				modeORvehType, smallScaleCommercialTrafficSegment);
		String startZone = carrier.getAttributes().getAttribute(
				TOUR_START_AREA) == null ? "" : carrier.getAttributes().getAttribute(
				TOUR_START_AREA).toString();
		Object startCategoryAttribute = carrier.getAttributes().getAttribute("startCategory");
		SmallScaleCommercialTrafficUtils.ZoneAttribute selectedStartCategory = startCategoryAttribute == null
				? generator.getSelectedStartCategory(startZone, odMatrixEntry)
				: SmallScaleCommercialTrafficUtils.ZoneAttribute.fromLabel(startCategoryAttribute.toString())
				.orElseGet(() -> SmallScaleCommercialTrafficUtils.ZoneAttribute.valueOf(startCategoryAttribute.toString()));
		return new GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes(
				purpose, startZone, selectedStartCategory, modeORvehType,
				smallScaleCommercialTrafficSegment, null, odMatrixEntry);
	}

	/**
	 * Returns the longest available tour duration among the given vehicles.
	 *
	 * @param vehicles vehicles whose availability windows are compared
	 * @return largest vehicle availability in seconds, or {@code 0} if the collection is empty
	 */
	private static double maxVehicleAvailability(Collection<CarrierVehicle> vehicles) {
		return vehicles.stream().mapToDouble(DefaultUnhandledServicesSolution::vehicleAvailability).max().orElse(0);
	}

	/**
	 * Calculates the time span between a vehicle's earliest start and latest end.
	 *
	 * @param vehicle vehicle whose availability window is measured
	 * @return vehicle availability in seconds
	 */
	private static double vehicleAvailability(CarrierVehicle vehicle) {
		return vehicle.getLatestEndTime() - vehicle.getEarliestStartTime();
	}

	/**
	 * Builds the same time-dependent transport-cost view used by the jsprit solver.
	 *
	 * @param scenario scenario whose network and freight config define the cost view
	 * @param vehicleTypes vehicle types that can be used by the transport-cost calculation
	 * @return network-based transport costs for single-service feasibility checks
	 */
	private static NetworkBasedTransportCosts createNetworkBasedTransportCosts(Scenario scenario, Collection<VehicleType> vehicleTypes) {

		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork(), vehicleTypes);
		if (scenario.getNetwork() instanceof TimeDependentNetwork timeDependentNetwork
			&& !timeDependentNetwork.getNetworkChangeEvents().isEmpty()) {
			FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FreightCarriersConfigGroup.class);
			builder.setTimeSliceWidth(freightConfig.getTravelTimeSliceWidth());
		}
		return builder.build();
	}

	/**
	 * Reserves each currently unused vehicle for at most one unhandled service and returns the services that still need
	 * a dedicated time-window fallback vehicle.
	 *
	 * @param unhandledServices services that still need to be inserted
	 * @param unusedVehicles vehicles that were not used in the previous selected plan
	 * @param transportCosts network-based travel-time provider used for depot-service-depot checks
	 * @return services for which no currently unused vehicle could be reserved
	 */
	private List<CarrierService> findServicesWithoutUnusedVehicleTimeWindow(List<CarrierService> unhandledServices,
	                                                                        List<CarrierVehicle> unusedVehicles,
	                                                                        NetworkBasedTransportCosts transportCosts) {
		List<CarrierVehicle> availableVehicles = new ArrayList<>(unusedVehicles);
		List<CarrierService> servicesWithoutVehicle = new ArrayList<>();
		List<CarrierService> servicesByFewestOptions = unhandledServices.stream()
			.sorted(Comparator.comparingLong(service -> unusedVehicles.stream()
				.filter(vehicle -> canServeSingleServiceInTime(vehicle, service, transportCosts))
				.count()))
			.toList();
		for (CarrierService service : servicesByFewestOptions) {
			Optional<CarrierVehicle> reservedVehicle = availableVehicles.stream()
				.filter(vehicle -> canServeSingleServiceInTime(vehicle, service, transportCosts))
				.min(Comparator.comparingDouble(DefaultUnhandledServicesSolution::vehicleAvailability));
			if (reservedVehicle.isPresent())
				availableVehicles.remove(reservedVehicle.get());
			else
				servicesWithoutVehicle.add(service);
		}
		return servicesWithoutVehicle;
	}

	/**
	 * Adds one dedicated normal, non-Recharge fallback vehicle for every service that could not reserve a currently
	 * unused vehicle. The new vehicle window is service-timed and capped, so it does not inherit a long reference-vehicle
	 * availability. It still ignores distance/range.
	 *
	 * @param carrier carrier that receives the fallback vehicles
	 * @param carrierAttributes attributes used to sample fallback start times
	 * @param servicesWithoutUnusedTimeWindow services that need a dedicated fallback vehicle
	 * @param transportCosts network-based travel-time provider used for depot-service-depot checks
	 * @param effectiveTravelBufferFactor current loop travel-buffer factor, logged for diagnostics
	 * @return number of fallback vehicles added to the carrier
	 */
	private int addTimeWindowFallbackVehicles(Carrier carrier,
	                                          GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes,
	                                          List<CarrierService> servicesWithoutUnusedTimeWindow,
	                                          NetworkBasedTransportCosts transportCosts,
	                                          double effectiveTravelBufferFactor) {
		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
		List<CarrierVehicle> referenceVehicles = carrierCapabilities.getCarrierVehicles().values().stream()
			.filter(vehicle -> vehicleTypeFallbackFilter.test(vehicle.getType()))
			.toList();
		if (referenceVehicles.isEmpty()) {
			log.warn("Carrier '{}' has no vehicle that can be used as a normal time-window fallback template for {} services.",
				carrier.getId(), servicesWithoutUnusedTimeWindow.size());
			return 0;
		}

		int addedVehicles = 0;
		int runningIndex = carrierCapabilities.getCarrierVehicles().size();
		for (CarrierService service : servicesWithoutUnusedTimeWindow) {
			// Prefer a fallback vehicle that starts at one of the existing carrier depots.
			List<ServiceTourFeasibility> depotFeasibilities = referenceVehicles.stream()
				.map(vehicle -> calculateFallbackServiceTimeWindow(vehicle, service, carrierAttributes, transportCosts))
				.flatMap(Optional::stream)
				.toList();
			Optional<ServiceTourFeasibility> fallbackFeasibility = depotFeasibilities.stream()
				.filter(feasibilityValue -> feasibilityValue.requiredTourDuration() + TIME_WINDOW_FALLBACK_SLACK
					<= MAX_FALLBACK_VEHICLE_AVAILABILITY)
				.min(Comparator.<ServiceTourFeasibility>comparingDouble(
						ServiceTourFeasibility::requiredTourDuration)
					.thenComparingDouble(ServiceTourFeasibility::requiredLatestEndExtension));

			if (fallbackFeasibility.isEmpty()) {
				if (depotFeasibilities.isEmpty()) {
					log.warn(
						"Carrier '{}': No existing fallback depot can reach service '{}' within its time window. No time-window fallback vehicle is added.",
						carrier.getId(), service.getId());
				} else {
					double shortestCappedAvailability = depotFeasibilities.stream()
						.mapToDouble(feasibilityValue -> feasibilityValue.requiredTourDuration() + TIME_WINDOW_FALLBACK_SLACK)
						.min().orElse(Double.NaN);
					log.warn(
						"Carrier '{}': No normal time-window fallback vehicle for service '{}' fits into the maximum fallback availability of {} hours. Shortest required availability including slack is {} minutes.",
						carrier.getId(), service.getId(), MAX_FALLBACK_VEHICLE_AVAILABILITY / 3600.,
						shortestCappedAvailability / 60.);
				}
				continue;
			}

			ServiceTourFeasibility selectedFeasibility = fallbackFeasibility.get();
			CarrierVehicle referenceVehicle = selectedFeasibility.vehicle();
			Id<Vehicle> vehicleId;
			do {
				runningIndex++;
				vehicleId = Id.create(carrier.getId().toString() + "_" + runningIndex, Vehicle.class);
			} while (carrierCapabilities.getCarrierVehicles().containsKey(vehicleId));
			// Keep a small slack when this repair extends a vehicle window. Network-based travel times and jsprit insertion
			// checks are both double based; a fallback that ends exactly at the calculated return time can still be rejected
			// because of sub-second differences or route-cost recalculation.
			double latestEndTime = selectedFeasibility.requiredLatestEndTime() + TIME_WINDOW_FALLBACK_SLACK;
			CarrierVehicle newVehicle = CarrierVehicle.Builder.newInstance(vehicleId, referenceVehicle.getLinkId(), referenceVehicle.getType())
				.setEarliestStart(selectedFeasibility.departureTime())
				.setLatestEnd(latestEndTime)
				.build();
			referenceVehicle.getAttributes().getAsMap().forEach((key, value) -> newVehicle.getAttributes().putAttribute(key, value));
			carrierCapabilities.getCarrierVehicles().put(newVehicle.getId(), newVehicle);
			addedVehicles++;
			log.info(
				"Carrier '{}': Added normal time-window fallback vehicle '{}' of type '{}' at depot '{}' for service '{}'. Window {}-{} (availability {} minutes, max {} hours); raw depot-service-depot duration is {} minutes, required availability including slack is {} minutes (current loop factor {}) using distribution-checked feasible start{}.",
				carrier.getId(), newVehicle.getId(), newVehicle.getType().getId(), newVehicle.getLinkId(), service.getId(),
				Time.writeTime(newVehicle.getEarliestStartTime()), Time.writeTime(newVehicle.getLatestEndTime()),
				(newVehicle.getLatestEndTime() - newVehicle.getEarliestStartTime()) / 60.,
				MAX_FALLBACK_VEHICLE_AVAILABILITY / 3600.,
				selectedFeasibility.requiredTourDuration() / 60.,
				(selectedFeasibility.requiredTourDuration() + TIME_WINDOW_FALLBACK_SLACK) / 60.,
				effectiveTravelBufferFactor,
				" including " + TIME_WINDOW_FALLBACK_SLACK / 60. + " minutes slack");
		}
		return addedVehicles;
	}

	/**
	 * Checks whether one vehicle can serve one service and return before its latest end time.
	 *
	 * @param vehicle candidate vehicle
	 * @param service service to check
	 * @param transportCosts network-based travel-time provider
	 * @return {@code true} if the vehicle can serve the service within its availability window
	 */
	private boolean canServeSingleServiceInTime(CarrierVehicle vehicle, CarrierService service, NetworkBasedTransportCosts transportCosts) {
		return calculateServiceTourFeasibility(vehicle, service, transportCosts, Math.max(0., vehicle.getEarliestStartTime()))
			.map(feasibility -> feasibility.requiredLatestEndTime() <= vehicle.getLatestEndTime())
			.orElse(false);
	}

	/**
	 * Calculates a service-timed fallback window. The fallback vehicle starts, according to the tour start distribution,
	 * if the sampled start can still reach the service before its latest start. If the sampled starts are too late, the
	 * start is clamped to the latest feasible departure.
	 *
	 * @param vehicle reference vehicle that provides depot and type information
	 * @param service service that should receive a dedicated fallback vehicle
	 * @param carrierAttributes attributes used to sample realistic fallback start times
	 * @param transportCosts network-based travel-time provider
	 * @return tour feasibility for the fallback vehicle, or {@link Optional#empty()} if no feasible schedule exists
	 */
	private Optional<ServiceTourFeasibility> calculateFallbackServiceTimeWindow(CarrierVehicle vehicle, CarrierService service,
	                                                                           GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes,
	                                                                           NetworkBasedTransportCosts transportCosts) {
		double latestServiceStartTime = service.getServiceStaringTimeWindow().getEnd();
		if (!Double.isFinite(latestServiceStartTime)) {
			return calculateServiceTourFeasibility(vehicle, service, transportCosts, Math.max(0., vehicle.getEarliestStartTime()));
		}

		double minDepartureTime = 0.;
		OptionalDouble latestFeasibleDepartureTime = calculateLatestFeasibleDepartureTime(vehicle, service, transportCosts, minDepartureTime,
			latestServiceStartTime);
		if (latestFeasibleDepartureTime.isEmpty()) {
			return Optional.empty();
		}

		double departureTime = sampleFeasibleFallbackDepartureTime(vehicle, service, carrierAttributes, transportCosts,
			minDepartureTime, latestFeasibleDepartureTime.getAsDouble(), latestServiceStartTime);
		return calculateServiceTourFeasibility(vehicle, service, transportCosts, departureTime);
	}

	/**
	 * Samples a fallback vehicle departure time from the carrier's tour-start distribution and keeps the first draw that
	 * can still reach the service window.
	 *
	 * @param vehicle reference vehicle that provides depot information
	 * @param service service whose latest start must be reached
	 * @param carrierAttributes attributes used to select the tour-start distribution
	 * @param transportCosts network-based travel-time provider
	 * @param minDepartureTime earliest allowed fallback departure time
	 * @param latestFeasibleDepartureTime latest departure time that can still reach the service
	 * @param latestServiceStartTime latest allowed service start time
	 * @return sampled feasible departure time, or the latest feasible departure time if sampling fails
	 */
	private double sampleFeasibleFallbackDepartureTime(CarrierVehicle vehicle, CarrierService service,
	                                                  GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes,
	                                                  NetworkBasedTransportCosts transportCosts, double minDepartureTime,
	                                                  double latestFeasibleDepartureTime, double latestServiceStartTime) {
		EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration> tourStartDistribution =
			generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficSegment());
		for (int tries = 0; tries < FALLBACK_START_SAMPLE_TRIES; tries++) {
			GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration sampledTour = tourStartDistribution.sample();
			double sampledDepartureTime = Math.max(minDepartureTime, sampledTour.getVehicleStartTime(this.rnd));
			if (sampledDepartureTime <= latestFeasibleDepartureTime
				&& canReachServiceStart(vehicle, service, transportCosts, sampledDepartureTime, latestServiceStartTime)) {
				return sampledDepartureTime;
			}
		}

		log.info(
			"Carrier service '{}': Could not sample a feasible fallback vehicle start after {} tries. Using latest feasible start {} instead.",
			service.getId(), FALLBACK_START_SAMPLE_TRIES, Time.writeTime(latestFeasibleDepartureTime));
		return latestFeasibleDepartureTime;
	}

	/**
	 * Calculates the latest departure time from a vehicle depot that can still reach the service before its latest start.
	 *
	 * @param vehicle vehicle that provides the depot and type information
	 * @param service service whose latest start time is checked
	 * @param transportCosts network-based travel-time provider
	 * @param minDepartureTime earliest departure time used as the lower bound
	 * @param latestServiceStartTime latest allowed service start time
	 * @return latest feasible departure time, or {@link OptionalDouble#empty()} if the service cannot be reached
	 */
	private OptionalDouble calculateLatestFeasibleDepartureTime(CarrierVehicle vehicle, CarrierService service,
	                                                           NetworkBasedTransportCosts transportCosts, double minDepartureTime,
	                                                           double latestServiceStartTime) {
		Location depot = Location.newInstance(vehicle.getLinkId().toString());
		Location serviceLocation = Location.newInstance(service.getServiceLinkId().toString());
		com.graphhopper.jsprit.core.problem.vehicle.Vehicle jspritVehicle = createJspritVehicle(vehicle);
		double outboundTravelTime = transportCosts.getTransportTime(depot, serviceLocation, minDepartureTime, null, jspritVehicle);
		double latestArrivalTime = latestServiceStartTime - TIME_WINDOW_FALLBACK_SLACK;
		if (minDepartureTime + outboundTravelTime > latestArrivalTime) {
			latestArrivalTime = latestServiceStartTime;
		}
		if (minDepartureTime + outboundTravelTime > latestArrivalTime) {
			return OptionalDouble.empty();
		}

		double departureTime = Math.max(minDepartureTime, latestArrivalTime - outboundTravelTime);
		outboundTravelTime = transportCosts.getTransportTime(depot, serviceLocation, departureTime, null, jspritVehicle);
		if (departureTime + outboundTravelTime > latestArrivalTime) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(departureTime);
	}

	/**
	 * Checks whether the vehicle can reach the service location from its depot before a given latest service start time.
	 *
	 * @param vehicle vehicle that provides the depot and type information
	 * @param service service whose location must be reached
	 * @param transportCosts network-based travel-time provider
	 * @param departureTime departure time from the vehicle depot
	 * @param latestServiceStartTime latest allowed service start time
	 * @return {@code true} if the depot-service trip arrives in time
	 */
	private boolean canReachServiceStart(CarrierVehicle vehicle, CarrierService service, NetworkBasedTransportCosts transportCosts,
	                                     double departureTime, double latestServiceStartTime) {
		Location depot = Location.newInstance(vehicle.getLinkId().toString());
		Location serviceLocation = Location.newInstance(service.getServiceLinkId().toString());
		com.graphhopper.jsprit.core.problem.vehicle.Vehicle jspritVehicle = createJspritVehicle(vehicle);
		double outboundTravelTime = transportCosts.getTransportTime(depot, serviceLocation, departureTime, null, jspritVehicle);
		return departureTime + outboundTravelTime <= latestServiceStartTime;
	}

	/**
	 * Calculates depot-service-depot feasibility for a fixed departure time.
	 *
	 * @param vehicle vehicle that provides depot and type information
	 * @param service service to be served
	 * @param transportCosts network-based travel-time provider
	 * @param vehicleDepartureTime departure time from the depot
	 * @return tour feasibility, or {@link Optional#empty()} if the service cannot be started within its time window
	 */
	private Optional<ServiceTourFeasibility> calculateServiceTourFeasibility(CarrierVehicle vehicle, CarrierService service,
	                                                                        NetworkBasedTransportCosts transportCosts,
	                                                                        double vehicleDepartureTime) {
		Location depot = Location.newInstance(vehicle.getLinkId().toString());
		Location serviceLocation = Location.newInstance(service.getServiceLinkId().toString());
		com.graphhopper.jsprit.core.problem.vehicle.Vehicle jspritVehicle = createJspritVehicle(vehicle);
		double outboundTravelTime = transportCosts.getTransportTime(depot, serviceLocation, vehicleDepartureTime, null, jspritVehicle);
		double serviceStartTime = Math.max(vehicleDepartureTime + outboundTravelTime, service.getServiceStaringTimeWindow().getStart());
		if (serviceStartTime > service.getServiceStaringTimeWindow().getEnd()) {
			return Optional.empty();
		}
		double serviceEndTime = serviceStartTime + service.getServiceDuration();
		double returnTravelTime = transportCosts.getTransportTime(serviceLocation, depot, serviceEndTime, null, jspritVehicle);
		double requiredVehicleLatestEndTime = serviceEndTime + returnTravelTime;
		return Optional.of(new ServiceTourFeasibility(vehicle, vehicleDepartureTime, requiredVehicleLatestEndTime,
			requiredVehicleLatestEndTime - vehicleDepartureTime));
	}

	/**
	 * Creates the jsprit vehicle representation needed by network-based transport cost calculations.
	 */
	private static com.graphhopper.jsprit.core.problem.vehicle.Vehicle createJspritVehicle(CarrierVehicle vehicle) {

		VehicleTypeImpl jspritType = VehicleTypeImpl.Builder.newInstance(vehicle.getType().getId().toString())
			.setMaxVelocity(vehicle.getType().getMaximumVelocity())
			.build();
		return VehicleImpl.Builder.newInstance(vehicle.getId().toString())
			.setStartLocation(Location.newInstance(vehicle.getLinkId().toString()))
			.setType(jspritType)
			.build();
	}

	/**
	 * Adds additional vehicles to the carrier until the sum of the maximum tour durations of the vehicles is higher than the required target duration.
	 * {@code maxVehicleSetsToAdd} bounds soft fallbacks, especially deficits caused only by the growing travel-buffer factor.
	 *
	 * @return    number of added vehicles
	 */
	private int addAdditionalVehicles(Carrier nonCompleteSolvedCarrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes,
									  List<CarrierService> nonHandledJobs, List<CarrierVehicle> unusedVehicle, int handledServices,
									  double sumServiceDurationsJobs, double sumMaxTourDurationsVehicles, double effectiveTravelBufferFactor,
									  int maxVehicleSetsToAdd) {
		CarrierCapabilities carrierCapabilities = nonCompleteSolvedCarrier.getCarrierCapabilities();
		int planedJobs = nonCompleteSolvedCarrier.getServices().size();
		int addedVehicles = 0;

		int runningIndex = carrierCapabilities.getCarrierVehicles().size();
		List<VehicleType> vehicleTypesToAdd = carrierCapabilities.getVehicleTypes().stream()
			.filter(vehicleTypeFallbackFilter)
			.toList();
		if (vehicleTypesToAdd.isEmpty()) {
			log.warn("Carrier '{}' has no vehicle types after applying the fallback filter. Cannot add vehicles to handle {} non-handled jobs.",
				nonCompleteSolvedCarrier.getId(), nonHandledJobs.size());
			return 0;
		}

		log.info("Carrier: {}, Number of non-used vehicles: {}. Number of non-handled jobs: {}. Number of handled services: {}.",
			nonCompleteSolvedCarrier.getId().toString(), unusedVehicle.size(), nonHandledJobs.size(), handledServices);

		double maxSingleUnhandledServiceDuration = nonHandledJobs.stream()
			.mapToDouble(CarrierService::getServiceDuration)
			.max().orElse(0);
		double requiredSingleServiceAvailability = maxSingleUnhandledServiceDuration * effectiveTravelBufferFactor;
		double targetSingleVehicleAvailability = Math.min(requiredSingleServiceAvailability, MAX_FALLBACK_VEHICLE_AVAILABILITY);
		if (requiredSingleServiceAvailability > MAX_FALLBACK_VEHICLE_AVAILABILITY) {
			log.info(
				"Carrier '{}': Longest unhandled service would require {} hours with buffer factor {}, but new fallback vehicles are capped at {} hours. Service-duration redraw remains the backup if the capped fleet cannot solve it.",
				nonCompleteSolvedCarrier.getId(), requiredSingleServiceAvailability / 3600., effectiveTravelBufferFactor,
				MAX_FALLBACK_VEHICLE_AVAILABILITY / 3600.);
		}
		int addedVehicleSets = 0;
		while (sumMaxTourDurationsVehicles < sumServiceDurationsJobs && addedVehicleSets < maxVehicleSetsToAdd) {
			addedVehicleSets++;
			GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration t = null;
			int tries = 0;
			int tourDuration = 0;
			// Samples tour duration until long enough or tries expire
			while (tourDuration < targetSingleVehicleAvailability && tries++ < 200) {
				t = generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficSegment() ).sample();
				tourDuration = t.getVehicleTourDuration(this.rnd);
			}

			// Sets minimum tour duration if sampling fails
			if (tourDuration < targetSingleVehicleAvailability) {
				tourDuration = (int) Math.ceil(targetSingleVehicleAvailability);
				t = generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficSegment() ).sample();
			}
			tourDuration = (int) Math.min(tourDuration, MAX_FALLBACK_VEHICLE_AVAILABILITY);
			assert t != null;
			int vehicleStartTime = t.getVehicleStartTime(this.rnd);
			int vehicleEndTime = vehicleStartTime + tourDuration;
			Id<Link> linkId = generator.findPossibleLink(carrierAttributes.startZone(),
				carrierAttributes.selectedStartCategory(), null);
			for (VehicleType thisVehicleType : vehicleTypesToAdd) { //TODO Flottenzusammensetzung anpassen. Momentan pro Depot alle Fahrzeugtypen 1x erzeugen
				runningIndex++;
				sumMaxTourDurationsVehicles += tourDuration;

				Id<Vehicle> vehcileId = Id.create(nonCompleteSolvedCarrier.getId().toString() + "_" + runningIndex, Vehicle.class);
				CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(vehcileId, linkId,
					thisVehicleType).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
				carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
				addedVehicles++;
				log.info("Added new vehicle '{}' to carrier '{}' with capped availability of {} minutes (max {} hours).",
					newCarrierVehicle.getId(), nonCompleteSolvedCarrier.getId(), tourDuration / 60.,
					MAX_FALLBACK_VEHICLE_AVAILABILITY / 3600.);
			}
		}
		if (sumMaxTourDurationsVehicles < sumServiceDurationsJobs && maxVehicleSetsToAdd != Integer.MAX_VALUE) {
			log.info(
				"Carrier '{}': Stopped adding vehicles after {} vehicle-type set(s) because this fallback is capped. Remaining time deficit is {} minutes.",
				nonCompleteSolvedCarrier.getId(), addedVehicleSets, (sumServiceDurationsJobs - sumMaxTourDurationsVehicles) / 60.);
		}

		log.info("Added {} vehicles to carrier '{}' to handle {} non-handled jobs ({} planned, {} handled).",
			addedVehicles, nonCompleteSolvedCarrier.getId(), nonHandledJobs.size(), planedJobs, handledServices);
		nonCompleteSolvedCarrier.setCarrierCapabilities(carrierCapabilities);
		return addedVehicles;
	}

	/**
	 * Increases the travel-buffer factor by the same excess over 1.0 in each replanning loop.
	 * For example, a base factor of 1.1 becomes 1.1, 1.2, 1.3, ... across iterations.
	 *
	 * @param loopIteration one-based carrier-replanning loop iteration
	 * @return travel-buffer factor to use in the given loop iteration
	 */
	private double effectiveTravelBufferFactor(int loopIteration) {
		double baseFactor = generator.getFactorForTravelBufferCalculation();
		return baseFactor + Math.max(0, loopIteration - 1) * Math.max(0., baseFactor - 1.);
	}

	/**
	 * Depot-service-depot feasibility for assigning one vehicle to one service.
	 *
	 * @param vehicle vehicle used for the feasibility check
	 * @param departureTime departure time from the vehicle depot
	 * @param requiredLatestEndTime latest vehicle end time required without additional buffering
	 * @param requiredTourDuration raw depot-service-depot duration without additional buffering
	 */
	private record ServiceTourFeasibility(CarrierVehicle vehicle, double departureTime, double requiredLatestEndTime,
	                                      double requiredTourDuration) {
		/**
		 * Returns the vehicle-window extension needed to fit the raw latest end time.
		 *
		 * @return additional latest-end extension in seconds
		 */
		private double requiredLatestEndExtension() {
			return Math.max(0., requiredLatestEndTime - vehicle.getLatestEndTime());
		}

	}

	/**
	 * Per-carrier state from the previous replanning loop used to detect stagnation and bound fallback attempts.
	 *
	 * @param numberOfUnhandledServices number of unhandled services in the previous loop
	 * @param numberOfUnusedVehicles number of unused vehicles in the previous selected plan
	 * @param stagnationVehicleFallbackSets number of fallback vehicle-type sets already attempted for stagnation
	 * @param stagnationVehicleFallbackSetLimit current cap for stagnation fallback vehicle-type sets
	 * @param unhandledServiceIds ids of services that remained unhandled in the previous loop
	 * @param singleServiceVehicleAdded whether the previous loop added a vehicle for exactly one open service
	 */
	record UnHandledInformation(int numberOfUnhandledServices, int numberOfUnusedVehicles,
	                            int stagnationVehicleFallbackSets, int stagnationVehicleFallbackSetLimit,
	                            Set<Id<CarrierService>> unhandledServiceIds,
	                            boolean singleServiceVehicleAdded) {}

	/**
	 * Extension point for adding specialized vehicles between the default fleet repair and the jsprit rerun.
	 */
	@FunctionalInterface
	interface PreReplanningVehicleAddition {
		/**
		 * Adds specialized vehicles after the default loop changed the fleet but before old plans are cleared.
		 * The effective travel-buffer factor is the same factor that DefaultUnhandledServicesSolution uses for its
		 * single-service time-window checks in the current loop iteration.
		 * Returning the number of added vehicles keeps the loop statistics complete.
		 *
		 * @param scenario scenario containing the carriers and network used for replanning
		 * @param nonCompleteSolvedCarriers carriers still needing repair in this loop
		 * @param effectiveTravelBufferFactor travel-buffer factor used by the default repair in this loop
		 * @return number of specialized vehicles added
		 */
		int addVehiclesBeforeReplanning(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers, double effectiveTravelBufferFactor);
	}
}
