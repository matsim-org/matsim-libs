package org.matsim.contrib.signals.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Class to convert an arbitrary signal control file of MATSim into an all day green signal control.
 * 
 * @author tthunig
 * 
 */
public final class CreateAllDayGreenFromSignalControl {

	public static void main(String[] args) {
		
		if (args == null || args.length < 2) {
			throw new UnsupportedOperationException("Please call this class with two arguments: "
					+ "1. the path to the input signal control file; "
					+ "2. the path to the output signal control file (showing all day green).");
		}
		
		String signalControlInputFile = args[0];
		String signalControlOutputFile = args[1];
		
		Scenario allDayGreenScenario = convertSignalControl(signalControlInputFile);
		
		writeSignalControl(signalControlOutputFile, allDayGreenScenario);
	}

	private static Scenario convertSignalControl(String pathToSignalControl) {
		
		Config config = ConfigUtils.createConfig();
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setUseSignalSystems(true);
		signalsConfigGroup.setSignalControlFile(pathToSignalControl);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);		
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		SignalsData signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		// iterate over all signal systems and get their signal control data
		for (SignalSystemControllerData signalSystemControl : signals.getSignalControlData().
				getSignalSystemControllerDataBySystemId().values()) {
			// iterate over all signal plans (there is normally only one per signal system)
			for (SignalPlanData signalPlan : signalSystemControl.getSignalPlanData().values()){
				// iterate over all signal groups of the signal system
				for (SignalGroupSettingsData signalGroupSetting : signalPlan.
						getSignalGroupSettingsDataByGroupId().values()){
					// set them to green for the whole cycle (all day green)
					signalGroupSetting.setOnset(0);
					signalGroupSetting.setDropping(signalPlan.getCycleTime());
				}
			}
		}
		
		return scenario;
	}

	private static void writeSignalControl(String outputSignalControl, Scenario scenario) {
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		writer.setSignalControlOutputFilename(outputSignalControl);
		writer.writeSignalsData(scenario);
	}

}
