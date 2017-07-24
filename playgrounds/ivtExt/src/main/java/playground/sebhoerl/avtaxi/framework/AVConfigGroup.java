package playground.sebhoerl.avtaxi.framework;

import org.matsim.core.config.ReflectiveConfigGroup;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.config.AVConfigReader;
import playground.sebhoerl.avtaxi.config.AVTimingParameters;

public class AVConfigGroup extends ReflectiveConfigGroup {
	final static String AV = "av";
	final static String CONFIG = "config";
	final static String PARALLEL_ROUTERS = "parallelRouters";

	private String configPath;
	private long parallelRouters = 4;
	
	public AVConfigGroup() {
		super(AV);
	}

	@StringGetter(CONFIG)
	public String getConfigPath() {
		return configPath;
	}

	@StringSetter(CONFIG)
	public void setConfigPath(String path) {
		configPath = path;
	}

	@StringGetter(PARALLEL_ROUTERS)
	public double getParallelRouters() {
		return parallelRouters;
	}

	@StringSetter(PARALLEL_ROUTERS)
	public void setParallelRouters(long parallelRouters) {
		this.parallelRouters = parallelRouters;
	}
}
