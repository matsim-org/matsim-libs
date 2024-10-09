package org.matsim.contrib.drt.sharingmetrics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.MatsimServices;

/**
 * @author nkuehnel / MOIA
 */
public class SharingMetricsModule extends AbstractDvrpModeModule {

    private final DrtConfigGroup drtConfigGroup;

    public SharingMetricsModule(DrtConfigGroup drtConfigGroup) {
        super(drtConfigGroup.getMode());
        this.drtConfigGroup = drtConfigGroup;
    }

    @Override
    public void install() {
        bindModal(SharingMetricsTracker.class).toProvider(modalProvider(getter ->
                new SharingMetricsTracker())).asEagerSingleton();
        addEventHandlerBinding().to(modalKey(SharingMetricsTracker.class));
        bindModal(SharingMetricsControlerListener.class).toProvider(modalProvider(getter ->
                new SharingMetricsControlerListener(getConfig(), drtConfigGroup,
                        getter.getModal(SharingMetricsTracker.class),
                        getter.get(MatsimServices.class))
        ));
        addControlerListenerBinding().to(modalKey(SharingMetricsControlerListener.class));
    }
}
