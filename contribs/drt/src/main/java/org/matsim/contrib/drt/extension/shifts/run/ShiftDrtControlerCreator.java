package org.matsim.contrib.drt.extension.shifts.run;

import com.google.common.collect.ImmutableSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class ShiftDrtControlerCreator {

	/**
	 * Creates a controller in one step. Assumes a single DRT service.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) {
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		ShiftDrtConfigGroup shiftDrtConfigGroup = ConfigUtils.addOrGetModule(config, ShiftDrtConfigGroup.class);

		Controler controler = DrtControlerCreator.createControler(config, otfvis);

		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingModule(new ShiftDrtModeModule(drtCfg, shiftDrtConfigGroup));
			controler.addOverridingQSimModule(new DrtModeQSimModule(drtCfg, new ShiftDrtModeOptimizerQSimModule(drtCfg, shiftDrtConfigGroup)));
			controler.addOverridingModule(new DrtModeAnalysisModule(drtCfg, ImmutableSet.of(DrtDriveTask.TYPE,
					DrtStopTask.TYPE, ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE,
					ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)));
			controler.addOverridingQSimModule(new ShiftDvrpFleetQsimModule(drtCfg.getMode()));
		}

		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		return controler;
	}
}
