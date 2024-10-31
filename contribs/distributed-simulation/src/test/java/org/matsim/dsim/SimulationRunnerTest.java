package org.matsim.dsim;

import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.Node;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;
import java.util.Set;

class SimulationRunnerTest {

    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    @Disabled("This test is disabled, because the binding does not yet work. I think this is because the scoring is not yet implemented.")
    public void localThreeLinkScenrio() {
        var config = ConfigUtils.createConfig();
        config.qsim().setMainModes(Set.of("default"));
        config.routing().setNetworkModes(Set.of("default"));
        config.controller().setOutputDirectory(utils.getOutputDirectory());
        var scenario = ScenarioUtils.createMutableScenario(config);
        scenario.setNetwork(ThreeLinkTestFixture.createNetwork(2));
        scenario.getPopulation().addPerson(ThreeLinkTestFixture.createPerson(scenario.getPopulation().getFactory()));
        var defaultType = VehicleUtils.createVehicleType(Id.create("default", VehicleType.class));
        scenario.getVehicles().addVehicleType(defaultType);
        scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(Id.createVehicleId("test-person_default"), defaultType));

        //var broker = mock(MessageBroker.class);
        //var eventsManager = TestUtils.mockExpectingEventsManager(ThreeLinkTestFixture.expectedEvents());
        var node = Node.builder()
                .parts(IntList.of(0, 1))
                .cores(2)
                .rank(0)
                .build();

        var topology = Topology.builder()
                .nodes(List.of(node))
                .totalPartitions(2)
                .build();

        var injector = Injector.createMinimalMatsimInjector(config, scenario, new AbstractModule() {
            @Override
            public void install() {
                //bind(TimeInterpretation.class).in(Singleton.class);
                //bind(MessageBroker.class).toInstance(broker);
                //bind(SimProvider.class);
                //bind(SimulationRunner.class).in(Singleton.class);
                //bind(LPExecutor.class).to(PoolExecutor.class);
                //bind(SerializationProvider.class).in(Singleton.class);
                bind(Node.class).toInstance(node);
                bind(Communicator.class).to(NullCommunicator.class);
                bind(IOHandler.class).in(Singleton.class);
                bind(Topology.class).toInstance(topology);

                //Multibinder<LPProvider> lpBinder = Multibinder.newSetBinder(binder(), LPProvider.class);
                //lpBinder.addBinding().to(SimProvider.class);
            }
        }, new DistributedSimulationModule(2));

        var runner = injector.getInstance(DSim.class);
        runner.run();
    }


}
