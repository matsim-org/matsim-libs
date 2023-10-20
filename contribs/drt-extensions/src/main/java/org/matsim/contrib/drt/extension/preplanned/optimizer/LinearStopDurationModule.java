
package org.matsim.contrib.drt.extension.preplanned.optimizer;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.CumulativeStopTimeCalculator;
import org.matsim.contrib.drt.stops.DefaultPassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

public class LinearStopDurationModule extends AbstractDvrpModeModule {
    private final DrtConfigGroup drtConfigGroup;

    public LinearStopDurationModule(DrtConfigGroup drtConfigGroup) {
        super(drtConfigGroup.getMode());
        this.drtConfigGroup = drtConfigGroup;
    }

    @Override
    public void install() {
		StopTimeCalculator stopTimeCalculator = new CumulativeStopTimeCalculator(new DefaultPassengerStopDurationProvider(drtConfigGroup.stopDuration));
		bindModal(StopTimeCalculator.class).toInstance(stopTimeCalculator);
    }
}

