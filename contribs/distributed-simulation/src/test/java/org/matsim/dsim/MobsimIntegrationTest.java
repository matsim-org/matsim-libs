package org.matsim.dsim;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MobsimIntegrationTest {

    @RegisterExtension
    MatsimTestUtils utils = new MatsimTestUtils();

    private static void compareXmlFilesByLine(Path expectedPath, Path actualPath) throws IOException {

        Set<String> expected = Files.readAllLines(expectedPath).stream()
                .map(MobsimIntegrationTest::normalize)
                .collect(Collectors.toSet());

        Set<String> actual = Files.readAllLines(actualPath).stream()
                .map(MobsimIntegrationTest::normalize)
                .collect(Collectors.toSet());

        Set<String> visited = new HashSet<>();

        for (String line : actual) {
            String norm = normalize(line);

        /*    if (expected.contains(norm)) {
                visited.add(norm);
            } else {
                throw new AssertionError("Unexpected line: " + norm);
            }

         */


            if (!expected.remove(norm)) {
                var candidates = expected.stream()
                        // filter for events, which are similar. Start at an index well behind the timestamp
                        .filter(expectedLine -> expectedLine.contains(norm.substring(23)))
                        .collect(Collectors.joining("\n"));
                throw new AssertionError("Unexpected line: " + norm + "\n Maybe one of the following events is what should have happened:\n" + candidates);
            }


        }

        assertThat(expected).isEmpty();
    }

    private static String normalize(String line) {
        return line.trim().replace("  />", "/>");
    }

    @Disabled
    @Test
    @Order(1)
    void runLocal() {

        Config local = createScenario();
        DistributedController controller = new DistributedController(new NullCommunicator(), local, 1, 1);
        controller.run();

        assertThat(Path.of(utils.getOutputDirectory()))
                .isNotEmptyDirectory()
                .isDirectoryContaining("glob:**kelheim-mini.output_events.xml");
    }

    @Disabled
    @Test
    @Order(2)
    void runDistributed() throws IOException, InterruptedException {

        Path output = Path.of(utils.getOutputDirectory());
        Files.createDirectories(output);

        boolean ret;
        try (ExecutorService pool = Executors.newFixedThreadPool(3)) {
            List<Communicator> comms = LocalCommunicator.create(3);
            for (Communicator comm : comms) {
                pool.submit(() -> {
                    try {
                        Config config = createScenario();
                        DistributedController c = new DistributedController(comm, config, 2, 1);
                        c.run();
                        comm.close();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        throw new RuntimeException(e);
                    }
                });
            }

            pool.shutdown();
            ret = pool.awaitTermination(1, TimeUnit.MINUTES);
        }

        assert ret : "Execution timed out.";

        Path distOutput = output.resolve("kelheim-mini.output_events.xml");
        assertThat(distOutput)
                .isNotEmptyFile();

        compareXmlFilesByLine(
                output.resolve("..").resolve("runLocal/kelheim-mini.output_events.xml"),
                distOutput
        );
    }

    private Config createScenario() {

        URL kelheim = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config.xml");

        Config config = ConfigUtils.loadConfig(kelheim);

        config.controller().setOutputDirectory(utils.getOutputDirectory());
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setMobsim("dsim");

        // Randomness will lead to different results from the baseline
        config.routing().setRoutingRandomness(0);

        // Compatibility with many scenarios
        Activities.addScoringParams(config);

        return config;
    }

}
