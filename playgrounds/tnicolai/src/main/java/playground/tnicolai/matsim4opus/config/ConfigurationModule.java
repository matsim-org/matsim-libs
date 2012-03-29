package playground.tnicolai.matsim4opus.config;

import org.matsim.core.config.Module;
import org.matsim.core.scenario.ScenarioImpl;

public class ConfigurationModule {
	
	public static AccessibilityParameterConfigModule getAccessibilityParameterConfigModule(ScenarioImpl scenario){
		Module m = scenario.getConfig().getModule(AccessibilityParameterConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityParameterConfigModule) {
			return (AccessibilityParameterConfigModule) m;
		}
		return null;
	}

	public static MATSim4UrbaSimControlerConfigModule getMATSim4UrbaSimControlerConfigModule(ScenarioImpl scenario){
		Module m = scenario.getConfig().getModule(MATSim4UrbaSimControlerConfigModule.GROUP_NAME);
		if (m instanceof MATSim4UrbaSimControlerConfigModule) {
			return (MATSim4UrbaSimControlerConfigModule) m;
		}
		return null;
	}
	
	public static UrbanSimParameterConfigModule getUrbanSimParameterConfigModule(ScenarioImpl scenario){
		Module m = scenario.getConfig().getModule(UrbanSimParameterConfigModule.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModule) {
			return (UrbanSimParameterConfigModule) m;
		}
		return null;
	}
}
