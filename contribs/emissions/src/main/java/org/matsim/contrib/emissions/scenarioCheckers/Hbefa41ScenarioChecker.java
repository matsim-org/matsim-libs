package org.matsim.contrib.emissions.scenarioCheckers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.HbefaTechnology;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.scenario.checkers.ScenarioChecker;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Arrays;

public class Hbefa41ScenarioChecker implements ScenarioChecker {
	private static final Logger log = LogManager.getLogger(Hbefa41ScenarioChecker.class);

	@Override
	public void checkConsistencyBeforeRun(Scenario scenario) {
		Level lvl;

		switch (scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel()) {
			case ignore -> {
				log.info("NOT running hbefa 4.1 scenario consistency check because vsp defaults checking level is set to IGNORE. This can be changed in the vspExperimental configGroup.");
				return;
			}
			case info -> lvl = Level.INFO;
			case warn, abort -> lvl = Level.WARN;
			default -> throw new RuntimeException("not implemented");
		}
		log.info("running checkConsistency of scenario before run ...");

		boolean problem = false; // ini


		if (ConfigUtils.hasModule(scenario.getConfig(), EmissionsConfigGroup.class)) {
			problem = checkHbefaTechnology(scenario, lvl, problem);
		}

		if (problem && scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel() == VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort) {
			String str = "found a situation that leads to vsp-abort.  aborting ...";
			System.out.flush();
			log.fatal(str);
			throw new RuntimeException(str);
		}

	}

	@Override
	public void checkConsistencyAfterRun(Scenario scenario) {

	}

	private boolean checkHbefaTechnology(Scenario scenario, Level lvl, boolean problem) {
		boolean validTechnology = false;
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			if (!type.getEngineInformation().getAttributes().isEmpty()) {
//				in some older scenarios, the settings for EmissionsConcept and Technology were switched
//				this leads to wrong calculations of air pollution
				validTechnology = Arrays.stream(HbefaTechnology.values())
					.anyMatch(t ->
						t.id.equals(VehicleUtils.getHbefaTechnology(type.getEngineInformation())));

//				allow average as technology
				if (!validTechnology && VehicleUtils.getHbefaTechnology(type.getEngineInformation()).equals("average")) {
					validTechnology = true;
				}

				if (!validTechnology) {
					break;
				}
			}
		}

		if (validTechnology) {
			problem = false;
		} else {
			log.log(lvl, "You have configured vehicle types with invalid hbefa technology settings." +
				"For Hbefa4.1 valid technologies are: {}", (Object) HbefaTechnology.values());
			problem = true;
		}
		return problem;
	}
}
