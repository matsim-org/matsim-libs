package org.matsim.contrib.drt.extension.shifts.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.io.DrtShiftsReader;
import org.matsim.contrib.drt.extension.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesUtils;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftUtils;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShifts;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author nkuehnel, fzwick
 */
public class ShiftDrtControlerCreator {
	/**
	 * Creates a standard scenario and adds a DRT route factory to the route factories.
	 *
	 * @param config
	 * @return
	 */
	public static Scenario createScenarioWithDrtRouteFactory(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		return scenario;
	}

	/**
	 * Creates a controller in one step. Assumes a single DRT service.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) {
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

		Scenario scenario = createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		ShiftDrtConfigGroup shiftDrtConfigGroup = ConfigUtils.addOrGetModule(config, ShiftDrtConfigGroup.class);
		if(shiftDrtConfigGroup.getOperationFacilityInputFile() != null) {
			final OperationFacilities operationFacilities = OperationFacilitiesUtils.getOrCreateShifts(scenario);
			new OperationFacilitiesReader(operationFacilities).readFile(shiftDrtConfigGroup.getOperationFacilityInputFile());
		}

		if(shiftDrtConfigGroup.getShiftInputFile() != null) {
			final DrtShifts shifts = DrtShiftUtils.getOrCreateShifts(scenario);
			new DrtShiftsReader(shifts).readFile(shiftDrtConfigGroup.getShiftInputFile());
		}

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeShiftDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingQSimModule(new ShiftDvrpFleetQsimModule(drtCfg.getMode()));
		}

		controler.configureQSimComponents(DvrpQSimComponents.activateModes(List.of("SHIFT_COMPONENT"),
				multiModeDrtConfig.modes().collect(toList())));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}
}
