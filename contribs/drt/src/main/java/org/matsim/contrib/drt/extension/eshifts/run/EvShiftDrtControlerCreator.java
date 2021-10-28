package org.matsim.contrib.drt.extension.eshifts.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.extension.eshifts.charging.ShiftOperatingVehicleProvider;
import org.matsim.contrib.drt.extension.eshifts.fleet.EvShiftDvrpFleetQSimModule;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDrtQSimModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class EvShiftDrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new MultiModeShiftEDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new EvModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));


		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingQSimModule(new EvShiftDvrpFleetQSimModule(drtCfg.getMode()));
			controler.addOverridingQSimModule(new ShiftDrtQSimModule(drtCfg.getMode()));
		}

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind(AuxDischargingHandler.VehicleProvider.class).to(ShiftOperatingVehicleProvider.class);
			}
		});

		controler.configureQSimComponents(DvrpQSimComponents.activateModes(List.of(EvModule.EV_COMPONENT),
				multiModeDrtConfig.modes().collect(toList())));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}
}
