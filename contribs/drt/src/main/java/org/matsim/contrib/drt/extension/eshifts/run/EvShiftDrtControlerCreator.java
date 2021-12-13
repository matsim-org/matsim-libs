package org.matsim.contrib.drt.extension.eshifts.run;

import com.google.common.collect.ImmutableSet;
import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.extension.edrt.run.EDrtControlerCreator;
import org.matsim.contrib.drt.extension.eshifts.charging.ShiftOperatingVehicleProvider;
import org.matsim.contrib.drt.extension.eshifts.fleet.EvShiftDvrpFleetQSimModule;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDrtModeModule;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDrtModeOptimizerQSimModule;
import org.matsim.contrib.drt.extension.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeQSimModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class EvShiftDrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		ShiftDrtConfigGroup shiftDrtConfigGroup = ConfigUtils.addOrGetModule(config, ShiftDrtConfigGroup.class);

		Controler controler = EDrtControlerCreator.createControler(config, otfvis);

		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingModule(new ShiftDrtModeModule(drtCfg, shiftDrtConfigGroup));
			controler.addOverridingQSimModule(new DrtModeQSimModule(drtCfg, new ShiftDrtModeOptimizerQSimModule(drtCfg, shiftDrtConfigGroup)));
			controler.addOverridingQSimModule(new DrtModeQSimModule(drtCfg, new ShiftEDrtModeOptimizerQSimModule(drtCfg, shiftDrtConfigGroup)));
			controler.addOverridingModule(new DrtModeAnalysisModule(drtCfg, ImmutableSet.of(DrtDriveTask.TYPE,
					DrtStopTask.TYPE, ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE,
					ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)));
			controler.addOverridingQSimModule(new EvShiftDvrpFleetQSimModule(drtCfg.getMode()));
		}

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind(AuxDischargingHandler.VehicleProvider.class).to(ShiftOperatingVehicleProvider.class);
			}
		});

		return controler;
	}
}
