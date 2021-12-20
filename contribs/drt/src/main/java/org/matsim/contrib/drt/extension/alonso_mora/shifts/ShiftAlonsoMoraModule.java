package org.matsim.contrib.drt.extension.alonso_mora.shifts;

import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraOptimizer;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicleFactory;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.DefaultAlonsoMoraFunction.Constraint;
import org.matsim.contrib.drt.extension.alonso_mora.scheduling.DefaultAlonsoMoraScheduler.OperationalVoter;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.TravelTimeEstimator;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.shifts.optimizer.ShiftDrtOptimizer;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater.StayTaskEndTimeCalculator;

public class ShiftAlonsoMoraModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtConfig;
	private final ShiftDrtConfigGroup shiftConfig;

	public ShiftAlonsoMoraModule(DrtConfigGroup drtConfig, ShiftDrtConfigGroup shiftConfig) {
		super(drtConfig.getMode());
		this.drtConfig = drtConfig;
		this.shiftConfig = shiftConfig;
	}

	@Override
	protected void configureQSim() {
		bindModal(AlonsoMoraVehicleFactory.class).toProvider(modalProvider(getter -> {
			return v -> new ShiftAlonsoMoraVehicle(v);
		}));

		// TODO: This can become a general binding in DRT
		bindModal(StayTaskEndTimeCalculator.class).toProvider(modalProvider(getter -> {
			return new ShiftDrtStayTaskEndTimeCalculator(shiftConfig, new EDrtStayTaskEndTimeCalculator(drtConfig));
		}));

		bindModal(OperationalVoter.class).toProvider(modalProvider(getter -> {
			return new ShiftOperationalVoter();
		}));

		bindModal(Constraint.class).toProvider(modalProvider(getter -> {
			TravelTimeEstimator travelTimeEstimator = getter.getModal(TravelTimeEstimator.class);
			return new AlonsoMoraShiftConstraint(travelTimeEstimator);
		}));

		// Wrap the Shift opimizer around the AlonsoMoraOptimizer instead of
		// DefaultDrtOptimizer
		addModalComponent(DrtOptimizer.class,
				modalProvider((getter) -> new ShiftDrtOptimizer(getter.getModal(AlonsoMoraOptimizer.class),
						getter.getModal(DrtShiftDispatcher.class), getter.getModal(ScheduleTimingUpdater.class))));
	}
}
