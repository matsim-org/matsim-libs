package org.matsim.dsim;

import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;

/**
 * Implementing classes provide information about the execution context of the simulation. Only two context variants are allowed:
 * <p>
 * 1. {@link LocalContext} describes the execution context for simulation that is executed on a single computer, and that uses the regular
 * {@link org.matsim.core.mobsim.qsim.QSim} as mobsim implementation. This is the execution context for most users at this point<br>
 * 2. {@link DistributedContext} describes the context for a simulation that is executed using the {@link DSim} implementation of the mobsim. This
 * variant of the mobsim can be executed on one or multiple compute nodes. Creating a {@link DistributedContext} requires to enable preview features
 * (at least of Java-21), since the distributed simulation uses the FFI-API, which is not finalized as of Java-21.
 * <p>
 * Implementation Note: This interface and its two implementations could normally be modelled with a single class. However, as described above, the
 * {@link DistributedContext} requires enabling preview features. To make sure that MATSim can be run without enabling those features, we chose to
 * implement this as an interface, so that we can provide a {@link LocalContext} variant which does not use any preview features. Once the FFI-API is
 * finalized and available as a standard feature in Java, this interface can be removed and both implementations can be represented by a single class.
 */
public sealed interface ExecutionContext permits DistributedContext, LocalContext {

    Topology getTopology();

    ComputeNode getComputeNode();

    /**
     * Whether the simulation is distributed across multiple nodes.
     */
    default boolean isDistributed() {
        return getTopology().isDistributed();
    }
}
