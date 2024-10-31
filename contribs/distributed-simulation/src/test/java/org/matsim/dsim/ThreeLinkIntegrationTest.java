package org.matsim.dsim;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
public class ThreeLinkIntegrationTest {

    @RegisterExtension
    MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void oneAgentOneThreads() throws URISyntaxException {

        var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
        var config = ConfigUtils.loadConfig(configPath, new DSimConfigGroup());
        var outputDir = utils.getOutputDirectory();

        // remove the partition information from the network
        var netInputPath = Paths.get(config.getContext().toURI()).getParent().resolve(config.network().getInputFile());
        var net = NetworkUtils.readNetwork(netInputPath.toString());
        for (var link : net.getLinks().values()) {
            link.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
        }
        for (var node : net.getNodes().values()) {
            node.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
        }
        var netPath = Paths.get(outputDir + "single-partition-network.xml").toAbsolutePath().toString();
        NetworkUtils.writeNetwork(net, netPath);

        config.controller().setOutputDirectory(utils.getOutputDirectory() + "output");
        config.network().setInputFile(netPath);
        var controller = new DistributedController(new NullCommunicator(), config, 1, 1);
        controller.run();

        var expectedEventsPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links.expected-events-1-plan.xml";
        var actualEventsPath = utils.getOutputDirectory() + "output/three-links.output_events.xml.gz";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath, actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
    }

    /**
     * This tests the propagation of a vehicle through the network. It was helpful to debug the timing of message
     * passing, including when processes should synchronize and when simulation times should be updated.
     */
    @Test
    void oneAgentThreeThreads() {

        var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
        var config = ConfigUtils.loadConfig(configPath, new DSimConfigGroup());
        config.controller().setOutputDirectory(utils.getOutputDirectory());
        var controller = new DistributedController(new NullCommunicator(), config, 3, 1);
        controller.run();

        var expectedEventsPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links.expected-events-1-plan.xml";
        var actualEventsPath = utils.getOutputDirectory() + "three-links.output_events.xml.gz";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath, actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
    }

    /**
     * This tests mainly tests, that storage capacities block following agents. The scenario contains two agents, one
     * with a slow vehicle and one with a fast vehicle. The one with the slow vehicle blocks the last link for 100s.
     * This test was helpful for debugging, the consuming and releasing logic of links. Also, this tests that storage
     * capacities are propagated between partitions
     */
    @Test
    void twoAgentsThreeThreads() {

        var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
        var config = ConfigUtils.loadConfig(configPath, new DSimConfigGroup());
        config.controller().setOutputDirectory(utils.getOutputDirectory());
        config.plans().setInputFile("three-links-plans-2.xml");
        var controller = new DistributedController(new NullCommunicator(), config, 3, 1);
        controller.run();

        var expectedEventsPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links.expected-events-2-plans.xml";
        var actualEventsPath = utils.getOutputDirectory() + "three-links.output_events.xml.gz";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath, actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
    }

    @Test
    void oneAgentThreeNodes() throws InterruptedException, ExecutionException, TimeoutException {
        var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
        var outputDirectory = utils.getOutputDirectory(); // this also creats the directory

        // start three instances each containing one partition
        var size = 3;
        var comms = LocalCommunicator.create(size);
        var pool = Executors.newFixedThreadPool(size);
        var futures = comms.stream()
                .map(comm -> pool.submit(() -> {
                    Config config = ConfigUtils.loadConfig(configPath, new DSimConfigGroup());
                    config.controller().setOutputDirectory(outputDirectory);
                    config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
                    DistributedController c = new DistributedController(comm, config, 1, 1);
                    c.run();
                    try {
                        comm.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        for (var f : futures) {
            f.get(2, TimeUnit.MINUTES);
        }

        var expectedEventsPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links.expected-events-1-plan.xml";
        var actualEventsPath = utils.getOutputDirectory() + "three-links.output_events.xml.gz";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath, actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
    }

    @Test
    void storageCapacityThreeNodes() throws URISyntaxException {

        var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
        var config = ConfigUtils.loadConfig(configPath, new DSimConfigGroup());
        config.plans().setInputFile("three-links-plans-storage-cap.xml");
        var outputDir = utils.getOutputDirectory();

        // remove the partition information from the network
        var netInputPath = Paths.get(config.getContext().toURI()).getParent().resolve(config.network().getInputFile());
        var net = NetworkUtils.readNetwork(netInputPath.toString());
        for (var link : net.getLinks().values()) {
            link.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
        }
        for (var node : net.getNodes().values()) {
            node.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
        }
        var netPath = Paths.get(outputDir + "single-partition-network.xml").toAbsolutePath().toString();
        NetworkUtils.writeNetwork(net, netPath);

        config.controller().setOutputDirectory(utils.getOutputDirectory() + "output");
        config.network().setInputFile(netPath);
        var controller = new DistributedController(new NullCommunicator(), config, 1, 1);
        controller.run();
        /*
        // start three instances each containing one partition
        var size = 3;
        var comms = LocalCommunicator.create(size);
        var pool = Executors.newFixedThreadPool(size);
        var futures = comms.stream()
                .map(comm -> pool.submit(() -> {
                    Config config = ConfigUtils.loadConfig(configPath, new DSimConfigGroup());
                    config.controller().setOutputDirectory(outputDirectory + "output");
                    config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
                    config.plans().setInputFile("three-links-plans-storage-cap.xml");
                    DistributedController c = new DistributedController(comm, config, 1, 1);
                    c.run();
                    try {
                        comm.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        for (var f : futures) {
            f.get();
        }
        var expectedEventsPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links.expected-events-1-plan.xml";
        var actualEventsPath = utils.getOutputDirectory() + "three-links.output_events.xml";

         */

    /*    try (var reader = Files.newBufferedReader(Paths.get(actualEventsPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

     */


        //compareXmlFilesByLine(expectedEventsPath, actualEventsPath);
    }
}
