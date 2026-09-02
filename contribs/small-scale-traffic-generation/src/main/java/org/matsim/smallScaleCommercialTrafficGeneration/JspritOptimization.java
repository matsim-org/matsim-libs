package org.matsim.smallScaleCommercialTrafficGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.lang.String.join;
import static org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.*;
import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.TOUR_START_AREA;

class JspritOptimization{
	private static final Logger log = LogManager.getLogger( JspritOptimization.class );
	private final int nJspritIterations;
	private final Map<Id<Carrier>, CarrierAttributes> carrierId2carrierAttributes;
	private final int maxNumberOfLoopsForVRPSolving;
	private final Map<String, Map<Id<Link>, Link>> linksPerZone;
	private final UnhandledServicesSolution unhandledServicesSolution;

	JspritOptimization( int nJspritIterations, Map<Id<Carrier>, CarrierAttributes> carrierId2carrierAttributes,
	                    int maxNumberOfLoopsForVRPSolving, Map<String, Map<Id<Link>, Link>> linksPerZone,
	                    UnhandledServicesSolution unhandledServicesSolution ) {
		// the sequence of constructor arguments is random.  Possibly replace by builder so that the sequence no longer matters.
		this.nJspritIterations = nJspritIterations;
		this.carrierId2carrierAttributes = carrierId2carrierAttributes;
		this.maxNumberOfLoopsForVRPSolving = maxNumberOfLoopsForVRPSolving;
		this.linksPerZone = linksPerZone;
		this.unhandledServicesSolution = unhandledServicesSolution;
	}

	/**
	 * Solves the generated carrier-plans and puts them into the Scenario.
	 * If a carrier has unhandled services, a carrier-replanning loop deletes the old plans and generates new plans.
	 * The new plans will then be solved and checked again.
	 * This is repeated until the carrier-plans are solved or the {@code maxNumberOfLoopsForVRPSolving} are reached.
	 *
	 * @param originalScenario                          complete Scenario
	 */
	void solveVRP( Scenario originalScenario ) {

		CarriersUtils.addRoadPricingForVRPToEnsureSolutionsBasedOnNetworkModes(originalScenario );

		boolean splitCarrier = true;

		// comparison of results for hannover: increasing to maxServicesPerCarrier = 300 results in:
		// computationTime *4; numberOfVehicles -2%; Jsprit score: -0,4%, drivenDistanc: -4,4%
		// RE: result is to set this to maxServicesPerCarrier = 100;
		// yy increasing from where?  kai, sep'26
		int maxServicesPerCarrier = 100;

		Carriers carriers = CarriersUtils.addOrGetCarriers(originalScenario );
		Map<Id<Carrier>, List<Id<Carrier>>> carrierId2subCarrierIds = new HashMap<>();

		int numberOfSplitCarriers = 0;

		if (splitCarrier) {
			Map<Id<Carrier>, Carrier> subCarriersToAdd = new HashMap<>();
			List<Id<Carrier>> keyListCarrierToRemove = new ArrayList<>();
			for (Carrier carrier : carriers.getCarriers().values()) {
				if (CarriersUtils.getJspritIterations(carrier) == 0 || CarriersUtils.allJobsHandledBySelectedPlan(carrier)){
					continue;
				}
				int countedServices = 0;
				int countedVehicles = 0;
				if (carrier.getServices().size() > maxServicesPerCarrier) {
					numberOfSplitCarriers++;
					int numberOfNewCarriers = (int) Math.ceil((double) carrier.getServices().size() / (double) maxServicesPerCarrier);
					int numberOfServicesPerNewCarrier = (int) Math.floor((double) carrier.getServices().size() / numberOfNewCarriers);

					int totalVehicles = carrier.getCarrierCapabilities().getCarrierVehicles().size();
					int numberOfVehiclesPerNewCarrier = totalVehicles / numberOfNewCarriers;
					int numberOfVehiclesRemainder = totalVehicles % numberOfNewCarriers;

					List<Id<Vehicle>> vehiclesForNewCarrier = new ArrayList<>( carrier.getCarrierCapabilities().getCarrierVehicles().keySet() );
					vehiclesForNewCarrier.sort( Comparator.comparing(Id::toString ) );

					List<Id<CarrierService>> servicesForNewCarrier = new ArrayList<>( carrier.getServices().keySet() );
					servicesForNewCarrier.sort( Comparator.comparing(Id::toString) );

					for (int jj = 0; jj < numberOfNewCarriers; jj++) {

						int numberOfServicesForNewCarrier = numberOfServicesPerNewCarrier;
						if (jj + 1 == numberOfNewCarriers){
							numberOfServicesForNewCarrier = carrier.getServices().size() - countedServices;
						}
						int numberOfVehiclesForNewCarrier = numberOfVehiclesPerNewCarrier;
						if (jj < numberOfVehiclesRemainder){
							numberOfVehiclesForNewCarrier++;
							// (the first couple of split carriers get one vehicle more in order to allocate the remainder)
						}
						Carrier newCarrier = CarriersUtils.createCarrier( Id.create(carrier.getId().toString() + "_part_" + (jj + 1), Carrier.class));
						CarrierCapabilities newCarrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(carrier.getCarrierCapabilities().getFleetSize()).build();
						newCarrierCapabilities.getVehicleTypes().addAll(carrier.getCarrierCapabilities().getVehicleTypes());
						newCarrierCapabilities.getCarrierVehicles().putAll(carrier.getCarrierCapabilities().getCarrierVehicles());
						newCarrier.setCarrierCapabilities(newCarrierCapabilities);
						newCarrier.getServices().putAll(carrier.getServices());
						CarriersUtils.setJspritIterations(newCarrier, CarriersUtils.getJspritIterations(carrier));
						carrier.getAttributes().getAsMap().keySet().forEach(attribute -> newCarrier.getAttributes()
							.putAttribute(attribute, carrier.getAttributes().getAttribute(attribute)));

						carrierId2subCarrierIds.putIfAbsent(carrier.getId(), new LinkedList<>());
						carrierId2subCarrierIds.get(carrier.getId()).add(newCarrier.getId());

						int fromIndexVehicles = Math.min(countedVehicles, vehiclesForNewCarrier.size());
						int fromIndexServices = Math.min(countedServices, servicesForNewCarrier.size());

						int toIndexVehicles = fromIndexVehicles + numberOfVehiclesForNewCarrier;
						int toIndexServices = fromIndexServices + numberOfServicesForNewCarrier;

						// just to be sure that the index is not out of bounds
						toIndexVehicles = Math.min(toIndexVehicles, vehiclesForNewCarrier.size());
						toIndexServices = Math.min(toIndexServices, servicesForNewCarrier.size());
						// (should not happen?  --> rather use Gbl.assert or similar?)

						if (fromIndexVehicles == toIndexVehicles && fromIndexServices == toIndexServices) {
							throw new IllegalStateException("No remaining vehicles/services but still splitting: " + carrier.getId());
						}

						List<Id<Vehicle>> subListVehicles = vehiclesForNewCarrier.subList(fromIndexVehicles, toIndexVehicles);
						List<Id<CarrierService>> subListServices = servicesForNewCarrier.subList(fromIndexServices, toIndexServices);

						newCarrier.getCarrierCapabilities().getCarrierVehicles().keySet().retainAll(subListVehicles);
						newCarrier.getServices().keySet().retainAll(subListServices);

						countedVehicles += newCarrier.getCarrierCapabilities().getCarrierVehicles().size();
						countedServices += newCarrier.getServices().size();

						subCarriersToAdd.put(newCarrier.getId(), newCarrier);
					}
					keyListCarrierToRemove.add(carrier.getId());
					if (countedVehicles != carrier.getCarrierCapabilities().getCarrierVehicles().size())
						throw new RuntimeException("Split parts of the carrier " + carrier.getId().toString()
							+ " has a different number of vehicles than the original carrier");
					if (countedServices != carrier.getServices().size())
						throw new RuntimeException("Split parts of the carrier " + carrier.getId().toString()
							+ " has a different number of services than the original carrier");

				}
			}
			log.info("Splitting carriers: {}/{}, because the maximum number of services per carriers is set to {}.",
				numberOfSplitCarriers, carriers.getCarriers().size(), maxServicesPerCarrier );
			// add created parts of new carriers and delete the old ones
			carriers.getCarriers().putAll(subCarriersToAdd);
			for (Id<Carrier> id : keyListCarrierToRemove) {
				carriers.getCarriers().remove(id);
			}
			log.info("New number of carriers to solve: {}.", carriers.getCarriers().size() );

		}

		// Map the values to the new subcarriers
		for (Id<Carrier> oldCarrierId : carrierId2subCarrierIds.keySet()) {
			for (Id<Carrier> newCarrierId : carrierId2subCarrierIds.get(oldCarrierId)) {
				if ( this.carrierId2carrierAttributes.putIfAbsent(newCarrierId, this.carrierId2carrierAttributes.get(oldCarrierId ) ) != null){
					throw new RuntimeException( "CarrierAttributes already exist for the carrier " + newCarrierId.toString() );
				}
			}
		}

		log.info("Start solving {} carriers. Carriers with 0 Jsprit iterations will be skipped and carriers with existing plans will be skipped.", carriers.getCarriers().size() );
		try{
			CarriersUtils.runJsprit(originalScenario, CarriersUtils.CarrierSelectionForSolution.solveOnlyForCarrierWithoutPlans);
		} catch( ExecutionException | InterruptedException e ){
			throw new RuntimeException( e );
		}

		List<Carrier> nonCompleteSolvedCarriers = CarriersUtils.createListOfCarrierWithUnhandledJobs(CarriersUtils.getCarriers(originalScenario));
		// (we have run jsprit with possibly not enough vehicles.  Jsprit in consequence will not handle all jobs.)

		if (!nonCompleteSolvedCarriers.isEmpty() && this.maxNumberOfLoopsForVRPSolving > 0) {
			CarriersUtils.writeCarriers(CarriersUtils.getCarriers(originalScenario),
				originalScenario.getConfig().controller().getOutputDirectory() + "/" + originalScenario.getConfig().controller().getRunId() + ".output_carriers_notCompletelySolved.xml.gz");

			this.unhandledServicesSolution.tryToSolveAllCarriersCompletely(originalScenario, nonCompleteSolvedCarriers );
			// (the above is a replacable implementation to address the situation when there are not enough vehicles)

		}

		CarriersUtils.getCarriers(originalScenario).getCarriers().values().forEach(carrier -> {
			if ( this.linksPerZone != null && !carrier.getAttributes().getAsMap().containsKey( TOUR_START_AREA )) {
				List<String> startAreas = new ArrayList<>();
				for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
					String tourStartZone = SmallScaleCommercialTrafficUtils
						.findZoneOfLink(tour.getTour().getStartLinkId(), this.linksPerZone );
					if (!startAreas.contains(tourStartZone))
						startAreas.add(tourStartZone);
				}
				carrier.getAttributes().putAttribute( TOUR_START_AREA,
						join( ";", startAreas ) );
			}
		});
	}
}
