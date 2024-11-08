package org.matsim.dsim;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.LPProvider;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.SimulationNode;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsModule;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.executors.LPExecutor;
import org.matsim.dsim.executors.PoolExecutor;
import org.matsim.dsim.executors.SingleExecutor;
import org.matsim.dsim.simulation.SimProvider;
import org.matsim.dsim.simulation.TimeInterpretation;

import java.util.*;
import java.util.stream.IntStream;

@Log4j2
public class DistributedSimulationModule extends AbstractModule {

    private final Communicator comm;
    private final int threads;
    private final double oversubscribe;
    private final SerializationProvider serializer = new SerializationProvider();
    private final Topology topology;

    public DistributedSimulationModule(Communicator comm, int threads, double oversubscribe) {
        this.comm = comm;
        this.threads = threads;
        this.oversubscribe = oversubscribe;

        // TODO: Connecting logic is currently here to support standard matsim controller without modification
        log.info("Waiting for {} other nodes to connect...", comm.getSize() - 1);
        try {
            comm.connect();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to other nodes", e);
        }

        log.info("All nodes connected");

        // This may be relevant if we want to partition the network or other lps
        topology = createTopology(serializer);

        log.info("Topology has {} partitions on {} nodes. Node {} is has parts: {}",
                topology.getTotalPartitions(), topology.getNodesCount(), comm.getRank(), topology.getNode(comm.getRank()).getParts());
    }

    /**
     * Constructor for module, without communication.
     */
    public DistributedSimulationModule(int threads) {
        this(new NullCommunicator(), threads, 1);
    }

    @SneakyThrows
    @Override
    public void install() {

        SimulationNode node = topology.getNode(comm.getRank());

        bind(Communicator.class).toInstance(comm);
        bind(SimulationNode.class).toInstance(node);
        bind(Topology.class).toInstance(topology);
        bind(MessageBroker.class).in(Singleton.class);
        bind(DSim.class).in(Singleton.class);
        bind(SerializationProvider.class).toInstance(serializer);
        bind(TimeInterpretation.class).in(Singleton.class);

		// Binds mobsim related things
		install(new QSimComponentsModule());
		installQSimModule(new TransitEngineModule());
		installQSimModule(new PopulationModule());
		bindMobsim().toProvider(DSimProvider.class);
		bind(QSimCompatibility.class);

		bindEventsManager().to(DistributedEventsManager.class).in(Singleton.class);

		addControlerListenerBinding().to(DSimControllerListener.class).in(Singleton.class);

        // Optional single threaded execution
        if (threads > 1) {
            bind(LPExecutor.class).to(PoolExecutor.class).in(Singleton.class);
        } else {
            bind(LPExecutor.class).to(SingleExecutor.class).in(Singleton.class);
        }

        Multibinder<LPProvider> lps = Multibinder.newSetBinder(binder(), LPProvider.class);
		lps.addBinding().to(SimProvider.class);


		// From the qsim module
		bind( new TypeLiteral<Collection<AbstractQSimModule>>() {} ).to(new TypeLiteral<Set<AbstractQSimModule>>() {});

	}

    private Topology createTopology(SerializationProvider serializer) {

        SimulationNode node = SimulationNode.builder()
                .cores(threads)
                .rank(comm.getRank())
                .build();

        // Receive node information from all ranks
        List<SimulationNode> nodes = comm.allGather(node, 0, serializer);
        nodes.sort(Comparator.comparingInt(SimulationNode::getRank));

        Topology.TopologyBuilder topology = Topology.builder();
        List<SimulationNode> topoNodes = new ArrayList<>();


        int total = 0;
        for (SimulationNode value : nodes) {

            SimulationNode.NodeBuilder n = value.toBuilder();
            int parts = (int) (value.getCores() * oversubscribe);

            n.parts(IntStream.range(total, total + parts).collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll));
			n.distributed(nodes.size() > 1);

            total += parts;
            topoNodes.add(n.build());
        }

        // head nodes needs to build topology with all partition info
        return topology
                .nodes(topoNodes)
                .totalPartitions(total)
                .build();
    }
}
