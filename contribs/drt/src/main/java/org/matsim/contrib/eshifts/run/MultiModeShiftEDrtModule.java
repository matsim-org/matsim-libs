package org.matsim.contrib.eshifts.run;

import com.google.inject.Inject;
import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.routing.MultiModeDrtMainModeIdentifier;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeQSimModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.shifts.analysis.*;
import org.matsim.contrib.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.shifts.run.ShiftDrtModeModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.MainModeIdentifier;

/**
 * @author nkuehnel
 */
public class MultiModeShiftEDrtModule extends AbstractModule {
    @Inject
    private MultiModeDrtConfigGroup multiModeDrtCfg;
    @Inject
    private ShiftDrtConfigGroup shiftConfigGroup;

    public MultiModeShiftEDrtModule() {
    }

    @Override
    public void install() {
        for (DrtConfigGroup drtCfg : this.multiModeDrtCfg.getModalElements()) {
            this.install(new ShiftDrtModeModule(drtCfg));
            installQSimModule(new DrtModeQSimModule(drtCfg, new ShiftEDrtModeOptimizerQSimModule(drtCfg, shiftConfigGroup)));
            this.install(new DrtModeAnalysisModule(drtCfg));
        }

        bind(ShiftDurationXY.class);
        bind(BreakCorridorXY.class);
        addEventHandlerBinding().to(ShiftDurationXY.class);
        addEventHandlerBinding().to(BreakCorridorXY.class);
        addControlerListenerBinding().to(ShiftAnalysisControlerListener.class);

        bind(ShiftHistogram.class);
        addControlerListenerBinding().to(ShiftHistogramListener.class);
        addEventHandlerBinding().to(ShiftHistogram.class);

        installQSimModule(new AbstractQSimModule() {
            @Override
            protected void configureQSim() {
                addQSimComponentBinding("SHIFT_COMPONENT").toProvider(
                        IndividualCapacityTimeProfileCollectorProvider.class);
            }
        });

        bind(MainModeIdentifier.class).toInstance(new MultiModeDrtMainModeIdentifier(this.multiModeDrtCfg));
    }
}



