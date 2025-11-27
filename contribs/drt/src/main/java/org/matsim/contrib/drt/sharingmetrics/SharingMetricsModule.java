package org.matsim.contrib.drt.sharingmetrics;

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
        bindModal(SharingMetricsControllerListener.class).toProvider(modalProvider(getter ->
                new SharingMetricsControllerListener(getConfig(), drtConfigGroup,
                        getter.getModal(SharingMetricsTracker.class),
                        getter.get(MatsimServices.class))
        ));
        addControllerListenerBinding().to(modalKey(SharingMetricsControllerListener.class));
    }
}
