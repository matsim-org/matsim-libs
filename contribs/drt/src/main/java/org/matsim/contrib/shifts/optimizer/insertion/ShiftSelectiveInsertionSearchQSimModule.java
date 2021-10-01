package org.matsim.contrib.shifts.optimizer.insertion;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DetourPathCalculator;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.SingleInsertionDetourPathCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

public class ShiftSelectiveInsertionSearchQSimModule extends AbstractDvrpModeQSimModule {
    private final DrtConfigGroup drtCfg;

    public ShiftSelectiveInsertionSearchQSimModule(DrtConfigGroup drtCfg) {
        super(drtCfg.getMode());
        this.drtCfg = drtCfg;
    }

    @Override
    protected void configureQSim() {
        bindModal(new TypeLiteral<DrtInsertionSearch<OneToManyPathSearch.PathData>>() {
        }).toProvider(modalProvider(getter -> {
            var costCalculator = getter.getModal(CostCalculationStrategy.class);
            var timer = getter.get(MobsimTimer.class);
            var provider = ShiftSelectiveInsertionProviders.create(drtCfg, timer, costCalculator,
                    getter.getModal(DvrpTravelTimeMatrix.class),
                    getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool());
			return ShiftDrtInsertionSearches.createShiftDrtInsertionSearch(provider,
					getter.getModal(DetourPathCalculator.class), costCalculator, drtCfg, timer);
        })).asEagerSingleton();

        addModalComponent(SingleInsertionDetourPathCalculator.class, new ModalProviders.AbstractProvider<>(getMode()) {
            @Inject
            @Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
            private TravelTime travelTime;

            @Override
            public SingleInsertionDetourPathCalculator get() {
                Network network = getModalInstance(Network.class);
                TravelDisutility travelDisutility = getModalInstance(
                        TravelDisutilityFactory.class).createTravelDisutility(travelTime);
                return new SingleInsertionDetourPathCalculator(network, travelTime, travelDisutility, drtCfg);
            }
        });
        bindModal(DetourPathCalculator.class).to(modalKey(SingleInsertionDetourPathCalculator.class));
    }
}
