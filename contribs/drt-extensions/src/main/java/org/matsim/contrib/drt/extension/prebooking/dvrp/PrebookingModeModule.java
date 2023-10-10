package org.matsim.contrib.drt.extension.prebooking.dvrp;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.stops.MinimumStopDurationAdapter;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.PrebookingStopTimeCalculator;
import org.matsim.contrib.drt.stops.StaticPassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

/**
 * This class sets up all required bindings for DRT with prebooking. It should
 * be added using addOverridingModule to the controler *after* the standard DRT
 * has been set up.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PrebookingModeModule extends AbstractDvrpModeModule {
	private final boolean isElectric;

	public PrebookingModeModule(String mode, boolean isElectric) {
		super(mode);

		this.isElectric = isElectric;
	}

	@Override
	public void install() {
		DrtConfigGroup drtConfig = MultiModeDrtConfigGroup.get(getConfig()).getModalElements() //
				.stream().filter(e -> e.getMode().equals(getMode())).findFirst().get();

		// default binding for the passenger stop duration: pickup takes the amount
		// indicated in the configuration, dropoff happens immediately
		bindModal(PassengerStopDurationProvider.class).toProvider(modalProvider(getter -> {
			return StaticPassengerStopDurationProvider.of(drtConfig.stopDuration, 0.0);
		}));

		// (vehicle) stop time is at least the stopDuration indicated in the drt
		// configuration, or longer, depending on when the requests are scheduled to
		// enter the vehicle
		bindModal(StopTimeCalculator.class).toProvider(modalProvider(getter -> {
			PassengerStopDurationProvider provider = getter.getModal(PassengerStopDurationProvider.class);
			return new MinimumStopDurationAdapter(new PrebookingStopTimeCalculator(provider), drtConfig.stopDuration);
		}));

		// install QSim bindings / overrides
		installOverridingQSimModule(new PrebookingModeQSimModule(getMode(), isElectric));
	}
}
