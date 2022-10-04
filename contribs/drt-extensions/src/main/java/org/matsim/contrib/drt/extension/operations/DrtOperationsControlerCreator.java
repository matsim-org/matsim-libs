package org.matsim.contrib.drt.extension.operations;

import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesQSimModule;
import org.matsim.contrib.drt.extension.operations.shifts.run.ShiftDrtModeModule;
import org.matsim.contrib.drt.extension.operations.shifts.run.ShiftDrtModeOptimizerQSimModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.DrtModeQSimModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class DrtOperationsControlerCreator {

	/**
	 * Creates a controller in one step.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) {
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);

		Controler controler = DrtControlerCreator.createControler(config, otfvis);

		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingModule(new ShiftDrtModeModule(drtCfg));
			controler.addOverridingQSimModule(new DrtModeQSimModule(drtCfg, new ShiftDrtModeOptimizerQSimModule(drtCfg)));
			controler.addOverridingQSimModule(new OperationFacilitiesQSimModule(drtCfg));
		}

		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		return controler;
	}
}
