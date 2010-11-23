package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class TaskConfiguration {
	
	private Config config;
	
	private String defaultArg;

	Config getConfig() {
		return config;
	}

	String getDefaultArg() {
		return defaultArg;
	}

	public TaskConfiguration(Config config, String defaultArg) {
		super();
		this.config = config;
		this.defaultArg = defaultArg;
	}

}
