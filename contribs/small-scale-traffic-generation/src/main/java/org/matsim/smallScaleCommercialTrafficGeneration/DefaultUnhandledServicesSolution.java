package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.common.base.Joiner;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.conventions.vsp.SubpopulationDefaultNames;
import org.matsim.core.gbl.MatsimRandom;
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

public class DefaultUnhandledServicesSolution implements UnhandledServicesSolution {
	private static final Logger log = LogManager.getLogger(DefaultUnhandledServicesSolution.class);
	private static final Joiner JOIN = Joiner.on("\t");
	private static final double MIN_STAGNATION_SERVICE_DURATION = 60.;
	private static final double TIME_WINDOW_FALLBACK_SLACK = 60.;

	Random rnd;
	private final GenerateSmallScaleCommercialTrafficDemand generator;
	private final IterationVehicleRepair iterationVehicleRepair;
	private final Predicate<VehicleType> vehicleTypeFallbackFilter;

	/**
	 * Creates the default repair strategy without specialized iteration hooks or vehicle-type filtering.
	 */
	DefaultUnhandledServicesSolution(GenerateSmallScaleCommercialTrafficDemand generator) {
		this(generator, (_, _, _) -> 0, _ -> true);
	}

	/**
	 * Creates the repair strategy with optional specialized vehicle repairs and fallback vehicle-type filtering.
	 */
	DefaultUnhandledServicesSolution(GenerateSmallScaleCommercialTrafficDemand generator, IterationVehicleRepair iterationVehicleRepair,
	                                 Predicate<VehicleType> vehicleTypeFallbackFilter) {
		rnd = MatsimRandom.getRandom();
		this.generator = generator;
		this.iterationVehicleRepair = Objects.requireNonNull(iterationVehicleRepair);
		this.vehicleTypeFallbackFilter = Objects.requireNonNull(vehicleTypeFallbackFilter);
	}

	/**
	 * Redraws the service-durations of these {@link CarrierService}s of the given {@link Carrier} which are not possible to handle with the current vehicle availabilities.
	 * Stagnating unhandled services can also be redrawn even if they already fit into a vehicle availability.
	 * This gives jsprit a changed input in cases where the time budget looks sufficient, but the same service remains unhandled in multiple replanning loops.
	 * The service durations are redrawn until they fit into the vehicle availability, including the effective buffer for the travel time.
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
			boolean preferShorterServiceDuration = serviceFitsCurrentAvailability && redrawFeasibleServices;
			int tries = 0;
			while ((newServiceDuration > maxServiceDuration
				|| Double.compare(newServiceDuration, service.getServiceDuration()) == 0
				|| (preferShorterServiceDuration && newServiceDuration >= service.getServiceDuration())) && tries++ < 200) {
				newServiceDuration = generator.getServiceTimePerStop(carrierAttributes);
			}
			if (newServiceDuration > maxServiceDuration || (preferShorterServiceDuration && newServiceDuration >= service.getServiceDuration())) {
				double feasibleServiceDuration = preferShorterServiceDuration
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
	 */
	@Override
	public void tryToSolveAllCarriersCompletely(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers) {
		int startNumberOfCarriersWithUnhandledJobs = nonCompleteSolvedCarriers.size();
		log.info("Starting with carrier-replanning loop.");

		for (Carrier carrier : nonCompleteSolvedCarriers) {
			generator.getCarrierId2carrierAttributes().computeIfAbsent(carrier.getId(), ignored -> createCarrierAttributes(carrier));
		}
		Path outputPath = Path.of(scenario.getConfig().controller().getOutputDirectory(),
			"analysis/freight/Carriers_SolvingLoop_stats.tsv");

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputPath.toString())) {
			// Write header only if the file is newly created
			if (Files.size(outputPath) == 0) {
				String[] header = {"iteration", "carriersWithUnhandledJobsBeforeLoopIteration", "carriersSolvedInIteration",
					"carriersNotSolvedInIteration", "addedVehicles", "changedServiceDurations", "additionalTravelBufferPerScheduledTourInMinutes", "calculationTimeInHH:MM:SS"};
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
				int additionalTravelBufferPerTourInThisIterationInMinutes = i * generator.getAdditionalTravelBufferPerTourAndIterationInMinutes();
				double effectiveTravelBufferFactor = effectiveTravelBufferFactor(i);
				Set<VehicleType> vehicleTypes = nonCompleteSolvedCarriers.stream()
					.flatMap(carrier -> carrier.getCarrierCapabilities().getVehicleTypes().stream())
					.collect(Collectors.toSet());
				NetworkBasedTransportCosts transportCosts = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork(), vehicleTypes).build();
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
					int scheduledTourCount = Math.max(1, nonCompleteSolvedCarrier.getSelectedPlan().getScheduledTours().size());
					double sumServiceDurationsWithoutIterationBuffer = sumServiceDurations(nonCompleteSolvedCarrier.getServices().values());
					double sumServiceDurationsWithBuffer = sumServiceDurationsWithoutIterationBuffer
						+ scheduledTourCount * additionalTravelBufferPerTourInThisIterationInMinutes * 60;
					double sumMaxTourDurationsVehicles = sumVehicleAvailabilities(
						nonCompleteSolvedCarrier.getCarrierCapabilities().getCarrierVehicles().values());
					double timeDeficit = sumServiceDurationsWithBuffer - sumMaxTourDurationsVehicles;
					double timeDeficitWithoutIterationBuffer = sumServiceDurationsWithoutIterationBuffer - sumMaxTourDurationsVehicles;

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
					int previousStagnationVehicleFallbackSets =
						lastLoopInformation == null ? 0 : lastLoopInformation.stagnationVehicleFallbackSets();
					int previousStagnationVehicleFallbackSetLimit =
						lastLoopInformation == null ? 0 : lastLoopInformation.stagnationVehicleFallbackSetLimit();

					// A real positive time deficit means the fleet needs more available vehicle time. The growing iteration
					// buffer is intentionally softer: if that buffer alone creates the deficit, add at most one vehicle-type
					// set and only while there are fewer unused vehicles than still-open services. This prevents late loop
					// iterations from manufacturing many vehicles just to satisfy a conservative buffer estimate.
					boolean aggregateServiceTimeDeficit = timeDeficitWithoutIterationBuffer > 0;
					boolean rawTimeBudgetSufficient = !aggregateServiceTimeDeficit;

					// Redraw service durations if an unhandled service is individually too long for the available vehicle windows.
					// Also redraw when a carrier stagnates although the raw aggregate time budget is sufficient. This intentionally
					// ignores the growing iteration buffer: otherwise the large late-loop buffer can hide stagnation and the loop
					// stops repairing carriers even though the actual service time would fit into the available vehicle windows.
					boolean redrawDueToStagnation = rawTimeBudgetSufficient && stagnatingOrWorse;
					boolean checkServiceDurationChange = anySingleJobInfeasible || redrawDueToStagnation;

					boolean addVehiclesForBufferOnlyDeficit = !aggregateServiceTimeDeficit && timeDeficit > 0
						&& unusedVehicles.size() < unhandledServices.size();

					// Carriers can also stagnate with unused vehicles even when the aggregate time budget is sufficient.
					// In those cases the remaining vehicles may be unusable for the last service because of depot or
					// time-window constraints. The stagnation fallback is deliberately conservative: it adds only normal
					// vehicles, only for services without any unused time-feasible vehicle, and only after the unused-vehicle
					// count stayed unchanged for one loop. Range and Recharge decisions stay in specialized repair hooks.
					boolean unusedVehicleCountStagnated =
						lastLoopInformation != null && lastLoopInformation.numberOfUnusedVehicles() == unusedVehicles.size();
					int stagnationVehicleFallbackSetLimit = previousStagnationVehicleFallbackSetLimit;
					if (redrawDueToStagnation && stagnationVehicleFallbackSetLimit == 0) {
						stagnationVehicleFallbackSetLimit = Math.max(unhandledServices.size(), lastLoopInformation.numberOfUnhandledServices());
					}
					// Time-window fallback vehicles are capped. Therefore they must already be sized for the largest
					// effective buffer this loop can reach; otherwise a vehicle that was valid in an early iteration can
					// become invalid again later, while the cap prevents adding the truly robust replacement vehicle.
					double timeWindowFallbackTravelBufferFactor = maximumEffectiveTravelBufferFactor();
					List<CarrierService> servicesWithoutUnusedTimeWindow = findServicesWithoutUnusedVehicleTimeWindow(
						unhandledServices, unusedVehicles, transportCosts, timeWindowFallbackTravelBufferFactor);
					boolean addVehiclesDueToStagnation = redrawDueToStagnation
						&& unusedVehicleCountStagnated
						&& previousStagnationVehicleFallbackSets < stagnationVehicleFallbackSetLimit
						&& !servicesWithoutUnusedTimeWindow.isEmpty();
					boolean checkAdditionalVehicles = aggregateServiceTimeDeficit || addVehiclesForBufferOnlyDeficit;
					boolean checkAnyVehicleAddition = checkAdditionalVehicles || addVehiclesDueToStagnation;
					int stagnationVehicleFallbackSets = previousStagnationVehicleFallbackSets + (addVehiclesDueToStagnation ? 1 : 0);
					double vehicleAdditionTargetDuration = aggregateServiceTimeDeficit
						? sumServiceDurationsWithoutIterationBuffer
						: sumServiceDurationsWithBuffer;
					int maxVehicleSetsToAdd = aggregateServiceTimeDeficit ? Integer.MAX_VALUE : 1;

					unhandledInformationPerCarrier.put(nonCompleteSolvedCarrier.getId(),
						new UnHandledInformation(unhandledServices.size(), unusedVehicles.size(),
							stagnationVehicleFallbackSets, stagnationVehicleFallbackSetLimit));

					log.info(
						"Carrier '{}': timeDeficit={} min (service+buffer={} / vehicles={}), timeDeficitWithoutIterationBuffer={} min, rawTimeBudgetSufficient={}, scheduledTours={}, additionalBufferPerTour={} min, effectiveTravelBufferFactor={}, timeWindowFallbackTravelBufferFactor={}, maxUnhandledServiceDurationWithEffectiveBuffer={} min, anySingleJobInfeasible={}, checkAddVehicles={}, addVehiclesForBufferOnlyDeficit={}, addVehiclesDueToStagnation={}, stagnationVehicleFallbackSets={}/{}, unusedVehicleCountStagnated={}, servicesWithoutUnusedTimeWindow={}, checkRedrawServiceDur={}, redrawDueToStagnation={}",
						nonCompleteSolvedCarrier.getId(),
						timeDeficit / 60.0, sumServiceDurationsWithBuffer / 60.0, sumMaxTourDurationsVehicles / 60.0,
						timeDeficitWithoutIterationBuffer / 60.0, rawTimeBudgetSufficient, scheduledTourCount, additionalTravelBufferPerTourInThisIterationInMinutes,
						effectiveTravelBufferFactor, timeWindowFallbackTravelBufferFactor, maxUnhandledServiceDurationWithEffectiveBuffer / 60.,
						anySingleJobInfeasible, checkAnyVehicleAddition, addVehiclesForBufferOnlyDeficit, addVehiclesDueToStagnation,
						stagnationVehicleFallbackSets, stagnationVehicleFallbackSetLimit, unusedVehicleCountStagnated,
						servicesWithoutUnusedTimeWindow.size(), checkServiceDurationChange, redrawDueToStagnation
					);
					// Add additional vehicles
					if (checkAdditionalVehicles)
						addedVehicles += addAdditionalVehicles(nonCompleteSolvedCarrier, carrierAttributes, unhandledServices, unusedVehicles,
							handledServices.size(), vehicleAdditionTargetDuration, sumMaxTourDurationsVehicles, effectiveTravelBufferFactor,
							maxVehicleSetsToAdd);
					if (addVehiclesDueToStagnation)
						addedVehicles += addTimeWindowFallbackVehicles(nonCompleteSolvedCarrier, servicesWithoutUnusedTimeWindow,
							unusedVehicles, transportCosts, effectiveTravelBufferFactor, timeWindowFallbackTravelBufferFactor);

					// change service durations of the services that could not be handelt within the vehicle availabilities
					if (checkServiceDurationChange)
						changedServiceDurations += redrawUnhandledServiceDurations(nonCompleteSolvedCarrier, carrierAttributes, unhandledServices,
							effectiveTravelBufferFactor, redrawDueToStagnation);
				}
				// Specialized repair strategies, for example range-aware fallbacks, can inspect the updated fleet here.
				// The selected plans are still available, so implementations can distinguish used from currently free
				// vehicles. The old plans are cleared only afterwards to force jsprit to solve the adjusted carriers.
				addedVehicles += iterationVehicleRepair.addVehiclesBeforeReplanning(scenario, nonCompleteSolvedCarriers,
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
					String.valueOf(additionalTravelBufferPerTourInThisIterationInMinutes),
					timeForThisLoop});
				writer.newLine();
				writer.flush();  // Ensure it's written immediately

				log.info(
					"End of carrier-replanning loop iteration: {}. From the {} carriers with unhandled jobs ({} already solved), {} were solved in this iteration with an additionalBuffer of {} minutes per scheduled tour.",
					i, startNumberOfCarriersWithUnhandledJobs, startNumberOfCarriersWithUnhandledJobs - numberOfCarriersWithUnhandledJobs,
					numberOfCarriersWithUnhandledJobs - nonCompleteSolvedCarriers.size(),
					additionalTravelBufferPerTourInThisIterationInMinutes);

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
		int purpose = carrier.getAttributes().getAttribute("purpose") == null ? 0 : Integer.parseInt(
			carrier.getAttributes().getAttribute("purpose").toString());
		String carrierId = carrier.getId().toString();
		GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType;
		String modeORvehType;
		if (carrier.getAttributes().getAttribute("subpopulation").toString().contains(SubpopulationDefaultNames.SUBPOP_COM_PERSON)) {
			smallScaleCommercialTrafficType = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic;
			modeORvehType = "total";
		} else if (carrier.getAttributes().getAttribute("subpopulation").toString().contains(SubpopulationDefaultNames.SUBPOP_GOODS)) {
			smallScaleCommercialTrafficType = GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic;
			String[] split = carrierId.split("vehTyp")[1].split("_"); //TODO make this via attributes
			modeORvehType = "vehTyp" + split[0];
		} else {
			log.warn("Carrier {} has no valid subpopulation. Skipping.", carrier.getId());
			return null;
		}
		OdMatrixEntryInformationProvider.OdMatrixEntryInformation odMatrixEntry =
			generator.odMatrixEntryInformationProvider.getOdMatrixEntryInformation(purpose, modeORvehType, smallScaleCommercialTrafficType);
		String startZone = carrier.getAttributes().getAttribute("tourStartArea") == null ? "" : carrier.getAttributes().getAttribute(
			"tourStartArea").toString();
		Object startCategoryAttribute = carrier.getAttributes().getAttribute("startCategory");
		SmallScaleCommercialTrafficUtils.StructuralAttribute selectedStartCategory = startCategoryAttribute == null
			? generator.getSelectedStartCategory(startZone, odMatrixEntry)
			: SmallScaleCommercialTrafficUtils.StructuralAttribute.fromLabel(startCategoryAttribute.toString())
			.orElseGet(() -> SmallScaleCommercialTrafficUtils.StructuralAttribute.valueOf(startCategoryAttribute.toString()));
		return new GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes(
			purpose, startZone, selectedStartCategory, modeORvehType,
			smallScaleCommercialTrafficType, null, odMatrixEntry);
	}

	/**
	 * Sums the raw durations of the given services.
	 */
	private static double sumServiceDurations(Collection<CarrierService> services) {
		return services.stream().mapToDouble(CarrierService::getServiceDuration).sum();
	}

	/**
	 * Sums the available tour duration of the given vehicles.
	 */
	private static double sumVehicleAvailabilities(Collection<CarrierVehicle> vehicles) {
		return vehicles.stream().mapToDouble(DefaultUnhandledServicesSolution::vehicleAvailability).sum();
	}

	/**
	 * Returns the longest available tour duration among the given vehicles.
	 */
	private static double maxVehicleAvailability(Collection<CarrierVehicle> vehicles) {
		return vehicles.stream().mapToDouble(DefaultUnhandledServicesSolution::vehicleAvailability).max().orElse(0);
	}

	/**
	 * Calculates the time span between a vehicle's earliest start and latest end.
	 */
	private static double vehicleAvailability(CarrierVehicle vehicle) {
		return vehicle.getLatestEndTime() - vehicle.getEarliestStartTime();
	}

	/**
	 * Finds unhandled services that cannot be served by any currently unused vehicle within the buffered time window.
	 */
	private List<CarrierService> findServicesWithoutUnusedVehicleTimeWindow(List<CarrierService> unhandledServices,
	                                                                        List<CarrierVehicle> unusedVehicles,
	                                                                        NetworkBasedTransportCosts transportCosts,
	                                                                        double effectiveTravelBufferFactor) {
		return unhandledServices.stream()
			.filter(service -> unusedVehicles.stream()
				.noneMatch(vehicle -> canServeSingleServiceInTime(vehicle, service, transportCosts, effectiveTravelBufferFactor)))
			.toList();
	}

	/**
	 * Adds normal, non-Recharge fallback vehicles only for stagnating services that cannot fit into any currently
	 * unused vehicle time window. Because this fallback is capped, the new vehicles are sized with the maximum effective
	 * travel-buffer factor the loop can reach, not just the current iteration factor. Otherwise an early fallback can be
	 * valid for the current iteration, become invalid in a later iteration, and still block another fallback through the
	 * cap. It still ignores distance/range.
	 */
	private int addTimeWindowFallbackVehicles(Carrier carrier, List<CarrierService> servicesWithoutUnusedTimeWindow,
	                                          List<CarrierVehicle> unusedVehicles, NetworkBasedTransportCosts transportCosts,
	                                          double effectiveTravelBufferFactor, double timeWindowFallbackTravelBufferFactor) {
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
		List<CarrierVehicle> availableVehicles = new ArrayList<>(unusedVehicles);
		for (CarrierService service : servicesWithoutUnusedTimeWindow) {
			if (availableVehicles.stream().anyMatch(vehicle -> canServeSingleServiceInTime(vehicle, service, transportCosts, timeWindowFallbackTravelBufferFactor))) {
				continue;
			}

			Optional<ServiceTimeWindowCandidate> candidate = referenceVehicles.stream()
				.map(vehicle -> calculateServiceTimeWindow(vehicle, service, transportCosts))
				.flatMap(Optional::stream)
				.min(Comparator.<ServiceTimeWindowCandidate>comparingDouble(
						candidateValue -> candidateValue.requiredLatestEndExtension(timeWindowFallbackTravelBufferFactor))
					.thenComparingDouble(ServiceTimeWindowCandidate::requiredTourDuration));
			if (candidate.isEmpty()) {
				log.warn("Carrier '{}': Could not find a normal vehicle template whose depot can reach service '{}' within the service time window.",
					carrier.getId(), service.getId());
				continue;
			}

			ServiceTimeWindowCandidate selectedCandidate = candidate.get();
			CarrierVehicle referenceVehicle = selectedCandidate.vehicle();
			Id<Vehicle> vehicleId;
			do {
				runningIndex++;
				vehicleId = Id.create(carrier.getId().toString() + "_" + runningIndex, Vehicle.class);
			} while (carrierCapabilities.getCarrierVehicles().containsKey(vehicleId));
			// Keep a small slack when this repair extends a vehicle window. Network-based travel times and jsprit insertion
			// checks are both double based; a fallback that ends exactly at the calculated return time can still be rejected
			// because of sub-second differences or route-cost recalculation.
			double bufferedRequiredLatestEndTime = selectedCandidate.bufferedRequiredLatestEndTime(timeWindowFallbackTravelBufferFactor);
			boolean latestEndWasExtended = bufferedRequiredLatestEndTime >= referenceVehicle.getLatestEndTime();
			double latestEndTime = latestEndWasExtended
				? bufferedRequiredLatestEndTime + TIME_WINDOW_FALLBACK_SLACK
				: referenceVehicle.getLatestEndTime();
			CarrierVehicle newVehicle = CarrierVehicle.Builder.newInstance(vehicleId, referenceVehicle.getLinkId(), referenceVehicle.getType())
				.setEarliestStart(selectedCandidate.departureTime())
				.setLatestEnd(latestEndTime)
				.build();
			referenceVehicle.getAttributes().getAsMap().forEach((key, value) -> newVehicle.getAttributes().putAttribute(key, value));
			carrierCapabilities.getCarrierVehicles().put(newVehicle.getId(), newVehicle);
			availableVehicles.add(newVehicle);
			addedVehicles++;
			log.info(
				"Carrier '{}': Added normal time-window fallback vehicle '{}' of type '{}' at depot '{}' for service '{}'. Window {}-{}; raw depot-service-depot duration is {} minutes, buffered duration with fallback factor {} is {} minutes (current loop factor {}){}.",
				carrier.getId(), newVehicle.getId(), newVehicle.getType().getId(), newVehicle.getLinkId(), service.getId(),
				Time.writeTime(newVehicle.getEarliestStartTime()), Time.writeTime(newVehicle.getLatestEndTime()),
				selectedCandidate.requiredTourDuration() / 60., timeWindowFallbackTravelBufferFactor,
				selectedCandidate.bufferedRequiredTourDuration(timeWindowFallbackTravelBufferFactor) / 60.,
				effectiveTravelBufferFactor,
				latestEndWasExtended ? " including " + TIME_WINDOW_FALLBACK_SLACK + " seconds slack" : "");
		}
		return addedVehicles;
	}

	/**
	 * Checks whether one vehicle can serve one service and return before its latest end time.
	 */
	private boolean canServeSingleServiceInTime(CarrierVehicle vehicle, CarrierService service, NetworkBasedTransportCosts transportCosts,
	                                            double effectiveTravelBufferFactor) {
		return calculateServiceTimeWindow(vehicle, service, transportCosts)
			.map(candidate -> candidate.bufferedRequiredLatestEndTime(effectiveTravelBufferFactor) <= vehicle.getLatestEndTime())
			.orElse(false);
	}

	/**
	 * Calculates the earliest feasible depot-service-depot timing for one vehicle-service pair.
	 */
	private Optional<ServiceTimeWindowCandidate> calculateServiceTimeWindow(CarrierVehicle vehicle, CarrierService service,
	                                                                       NetworkBasedTransportCosts transportCosts) {
		double departureTime = Math.max(0., vehicle.getEarliestStartTime());
		Location depot = Location.newInstance(vehicle.getLinkId().toString());
		Location serviceLocation = Location.newInstance(service.getServiceLinkId().toString());
		com.graphhopper.jsprit.core.problem.vehicle.Vehicle jspritVehicle = createJspritVehicle(vehicle);

		double outboundTravelTime = transportCosts.getTransportTime(depot, serviceLocation, departureTime, null, jspritVehicle);
		double serviceStartTime = Math.max(departureTime + outboundTravelTime, service.getServiceStaringTimeWindow().getStart());
		if (serviceStartTime > service.getServiceStaringTimeWindow().getEnd()) {
			return Optional.empty();
		}
		double serviceEndTime = serviceStartTime + service.getServiceDuration();
		double returnTravelTime = transportCosts.getTransportTime(serviceLocation, depot, serviceEndTime, null, jspritVehicle);
		double requiredLatestEndTime = serviceEndTime + returnTravelTime;
		return Optional.of(new ServiceTimeWindowCandidate(vehicle, departureTime, requiredLatestEndTime,
			requiredLatestEndTime - departureTime));
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
	 * Adds additional vehicles to the carrier until the sum of the maximum tour durations of the vehicles is higher than the sum of the service durations of the jobs (including the additional buffer).
	 * {@code maxVehicleSetsToAdd} bounds soft fallbacks, especially deficits caused only by the growing iteration buffer.
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
		int addedVehicleSets = 0;
		while (sumMaxTourDurationsVehicles < sumServiceDurationsJobs && addedVehicleSets < maxVehicleSetsToAdd) {
			addedVehicleSets++;
			GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration t = null;
			int tries = 0;
			int tourDuration = 0;
			// Samples tour duration until long enough or tries expire
			while (tourDuration < maxSingleUnhandledServiceDuration * effectiveTravelBufferFactor && tries++ < 200) {
				t = generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficType()).sample();
				tourDuration = t.getVehicleTourDuration(this.rnd);
			}

			// Sets minimum tour duration if sampling fails
			if (tourDuration < maxSingleUnhandledServiceDuration * effectiveTravelBufferFactor) {
				tourDuration = (int) Math.ceil(maxSingleUnhandledServiceDuration * effectiveTravelBufferFactor);
				t = generator.getTourDistribution().get(carrierAttributes.smallScaleCommercialTrafficType()).sample();
			}
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
				log.info("Added new vehicle '{}' to carrier '{}'", newCarrierVehicle.getId(), nonCompleteSolvedCarrier.getId());
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
	 */
	private double effectiveTravelBufferFactor(int loopIteration) {
		double baseFactor = generator.getFactorForTravelBufferCalculation();
		return baseFactor + Math.max(0, loopIteration - 1) * Math.max(0., baseFactor - 1.);
	}

	/**
	 * Returns the effective travel-buffer factor used in the last configured replanning loop.
	 */
	private double maximumEffectiveTravelBufferFactor() {
		return effectiveTravelBufferFactor(generator.getMaxNumberOfLoopsForVRPSolving());
	}

	private record ServiceTimeWindowCandidate(CarrierVehicle vehicle, double departureTime, double requiredLatestEndTime,
	                                          double requiredTourDuration) {
		/**
		 * Scales the raw depot-service-depot duration by the effective travel-buffer factor.
		 */
		private double bufferedRequiredTourDuration(double effectiveTravelBufferFactor) {
			return requiredTourDuration * Math.max(1., effectiveTravelBufferFactor);
		}

		/**
		 * Returns the latest end time required after applying the effective travel-buffer factor.
		 */
		private double bufferedRequiredLatestEndTime(double effectiveTravelBufferFactor) {
			return departureTime + bufferedRequiredTourDuration(effectiveTravelBufferFactor);
		}

		/**
		 * Returns the vehicle-window extension needed to fit the buffered latest end time.
		 */
		private double requiredLatestEndExtension(double effectiveTravelBufferFactor) {
			return Math.max(0., bufferedRequiredLatestEndTime(effectiveTravelBufferFactor) - vehicle.getLatestEndTime());
		}
	}

	record UnHandledInformation(int numberOfUnhandledServices, int numberOfUnusedVehicles,
	                            int stagnationVehicleFallbackSets, int stagnationVehicleFallbackSetLimit) {}

	@FunctionalInterface
	interface IterationVehicleRepair {
		/**
		 * Adds specialized vehicles after the default loop changed the fleet but before old plans are cleared.
		 * The effective travel-buffer factor is the same factor that DefaultUnhandledServicesSolution uses for its
		 * single-service time-window checks in the current loop iteration.
		 * Returning the number of added vehicles keeps the loop statistics complete.
		 */
		int addVehiclesBeforeReplanning(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers, double effectiveTravelBufferFactor);
	}
}
