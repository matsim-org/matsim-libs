package org.matsim.dsim;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Config group for distributed simulation.
 */
public class DSimConfigGroup extends ReflectiveConfigGroup {

    public enum Partitioning {none, bisect, metis}

    public final static String CONFIG_MODULE_NAME = "dsim";

	/**
	 * Create a new config group with the given number of threads.
	 */
	public static DSimConfigGroup ofThreads(int threads) {
		DSimConfigGroup config = new DSimConfigGroup();
		config.threads = threads;
		return config;
	}

    @Parameter
    @Comment("Partitioning strategy for the network")
    public Partitioning partitioning = Partitioning.bisect;

	@Parameter
	@Comment("Number of threads to use for execution. If <= 0, the number of available processors is used.")
	public int threads = 0;

    public DSimConfigGroup() {
        super(CONFIG_MODULE_NAME);
    }
}
