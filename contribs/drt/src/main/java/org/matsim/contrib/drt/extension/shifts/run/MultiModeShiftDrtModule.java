package org.matsim.contrib.drt.extension.shifts.run;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.routing.MultiModeDrtMainModeIdentifier;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeQSimModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.extension.shifts.analysis.*;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.MainModeIdentifier;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class MultiModeShiftDrtModule extends AbstractModule {
    @Inject
    private MultiModeDrtConfigGroup multiModeDrtCfg;
    @Inject
    private ShiftDrtConfigGroup shiftCfg;

    public MultiModeShiftDrtModule() {
    }

    @Override
    public void install() {
        for (DrtConfigGroup drtCfg : this.multiModeDrtCfg.getModalElements()) {
            install(new ShiftDrtModeModule(drtCfg, shiftCfg));
            installQSimModule(new DrtModeQSimModule(drtCfg, new ShiftDrtModeOptimizerQSimModule(drtCfg, shiftCfg)));
            install(new DrtModeAnalysisModule(drtCfg, ImmutableSet.of(DrtDriveTask.TYPE,
					DrtStopTask.TYPE, ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE,
					ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)));
        }

        bind(MainModeIdentifier.class).toInstance(new MultiModeDrtMainModeIdentifier(this.multiModeDrtCfg));
    }
}
