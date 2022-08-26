package org.matsim.contrib.drt.extension.eshifts.run;

import org.matsim.contrib.drt.extension.edrt.run.EDrtControlerCreator;
import org.matsim.contrib.drt.extension.eshifts.charging.ShiftOperatingVehicleProvider;
import org.matsim.contrib.drt.extension.eshifts.fleet.EvShiftDvrpFleetQSimModule;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDrtModeModule;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDrtModeOptimizerQSimModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeQSimModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class EvShiftDrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);

		Controler controler = EDrtControlerCreator.createControler(config, otfvis);

		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingModule(new ShiftDrtModeModule(drtCfg));
			controler.addOverridingQSimModule(new DrtModeQSimModule(drtCfg, new ShiftDrtModeOptimizerQSimModule(drtCfg)));
			controler.addOverridingQSimModule(new ShiftEDrtModeOptimizerQSimModule(drtCfg));
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
