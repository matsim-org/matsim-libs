package org.matsim.dsim;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Config group for distributed simulation.
 */
public class DSimConfigGroup extends ReflectiveConfigGroup {

    public enum Partitioning {none, bisect, metis}

    public final static String CONFIG_MODULE_NAME = "dsim";

    @Parameter
    @Comment("Partitioning strategy for the network")
    public Partitioning partitioning = Partitioning.bisect;

    public DSimConfigGroup() {
        super(CONFIG_MODULE_NAME);
    }
}
