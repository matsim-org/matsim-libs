package playground.dgrether.koehlerstrehlersignal.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.utils.DgScenarioUtils;

/**
 * Class to convert an arbitrary signal control file of MATSim into an all day green signal control.
 * 
 * @author tthunig
 * 
 */
public class TtCreateAllDayGreenFromSignalControl {

	public static void main(String[] args) {
		
		String signalControlInputFile = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/parallel_scenario/AB/signalControlBC.xml";
		String signalControlOutputFile = DgPaths.SHAREDSVN + 
				"projects/cottbus/data/scenarios/parallel_scenario/AB/signalControlGreen.xml";
		
		convertAndWriteSignalControl(signalControlOutputFile, signalControlInputFile);
	}

	private static void convertAndWriteSignalControl(
			String outputSignalControl, String signalControlFilename) {
		
		Scenario readSignalControl = DgScenarioUtils.loadScenario(null, null, null, null,
				null, signalControlFilename);
		SignalsData signals = (SignalsData) readSignalControl
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		
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
		
		// write all day green signal control
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		writer.setSignalControlOutputFilename(outputSignalControl);
		writer.writeSignalsData(signals);
	}

}
