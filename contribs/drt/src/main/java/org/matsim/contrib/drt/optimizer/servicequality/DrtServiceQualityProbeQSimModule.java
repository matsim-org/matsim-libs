package org.matsim.contrib.drt.optimizer.servicequality;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.DvrpLoadFromTrip;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.api.core.v01.network.Network;

/** Installs the optional service-quality probe independently of request insertion. */
public class DrtServiceQualityProbeQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtConfigGroup;

	public DrtServiceQualityProbeQSimModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.getMode());
		this.drtConfigGroup = drtConfigGroup;
	}

	@Override
	protected void configureQSim() {
		if (drtConfigGroup.getDrtServiceQualityProbeParams().isEmpty()
			|| !drtConfigGroup.getDrtServiceQualityProbeParams().get().isEnabled()) {
			return;
		}

		bindModal(DrtServiceQualityProbe.class).toProvider(modalProvider(getter -> {
			DrtServiceQualityProbeParams params = drtConfigGroup.getDrtServiceQualityProbeParams().get();
			return new DrtServiceQualityProbe(
				getter.get(MatsimServices.class),
				drtConfigGroup.getMode(),
				getter.getModal(DrtStopNetwork.class),
				getter.getModal(Network.class),
				getter.getModal(TravelTime.class),
				getter.get(LeastCostPathCalculatorFactory.class),
				getter.getModal(TravelDisutilityFactory.class),
				getter.getModal(DrtRouteConstraintsCalculator.class),
				getter.getModal(DvrpLoadFromTrip.class),
				getter.getModal(RequestFleetFilter.class),
				() -> getter.getModal(DrtInsertionSearch.class),
				getter.getModal(Fleet.class),
				getter.getModal(VehicleEntry.EntryFactory.class),
				params);
		}));
		addModalQSimComponentBinding().to(modalKey(DrtServiceQualityProbe.class));
	}
}
