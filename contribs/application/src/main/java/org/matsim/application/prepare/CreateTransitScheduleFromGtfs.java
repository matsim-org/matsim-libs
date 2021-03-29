package org.matsim.application.prepare;

import com.conveyal.gtfs.model.Stop;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.MatsimVehicleWriter;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;


/**
 * This script utilizes GTFS2MATSim and creates a pseudo network and vehicles using MATSim standard API functionality.
 *
 * @author rakow
 */
@CommandLine.Command(
        name = "transit-from-gtfs",
        description = "Create transit schedule and vehicles from GTFS data and merge pt network into existing network",
        showDefaultValues = true
)
public class CreateTransitScheduleFromGtfs implements MATSimAppCommand {

    @CommandLine.Parameters(arity = "1..*", paramLabel = "INPUT", description = "Input GTFS zip files")
    private List<Path> gtfsFiles;

    @CommandLine.Option(names = "--name", description = "Prefix of the output files", required = true)
    private String name;

    @CommandLine.Option(names = "--network", description = "Base network that will be merged with pt network.", required = true)
    private Path networkFile;

    @CommandLine.Option(names = "--date", description = "The day for which the schedules will be extracted", required = true)
    private LocalDate date;

    @CommandLine.Option(names = "--output", description = "Output folder", defaultValue = "scenarios/input")
    private File output;

    @CommandLine.Option(names = "--include-stops", description = "Fully qualified class name to a Predicate<Stop> for filtering certain stops")
    private Class<?> includeStops;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();

    public static void main(String[] args) {
        System.exit(new CommandLine(new CreateTransitScheduleFromGtfs()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), crs.getTargetCRS());

        // Output files
        File scheduleFile = new File(output, name + "-transitSchedule.xml.gz");
        File networkPTFile = new File(output, networkFile.getFileName().toString().replace(".xml", "-with-pt.xml"));
        File transitVehiclesFile = new File(output, name + "-transitVehicles.xml.gz");

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        for (Path gtfsFile : gtfsFiles) {

            GtfsConverter converter = GtfsConverter.newBuilder()
                    .setScenario(scenario)
                    .setTransform(ct)
                    .setDate(date)
                    .setFeed(gtfsFile)
                    //.setIncludeAgency(agency -> agency.equals("rbg-70"))
                    .setIncludeStop(includeStops != null ? (Predicate<Stop>) includeStops.getDeclaredConstructor().newInstance() : (stop) -> true)
                    .setMergeStops(true)
                    .build();

            converter.convert();
        }

        Network network = Files.exists(networkFile) ? NetworkUtils.readNetwork(networkFile.toString()) : scenario.getNetwork();

        // Create a network around the schedule
        new CreatePseudoNetwork(scenario.getTransitSchedule(), network, "pt_").createNetwork();
        new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleFile.getAbsolutePath());
        new NetworkWriter(network).write(networkPTFile.getAbsolutePath());
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(transitVehiclesFile.getAbsolutePath());

        return 0;
    }

}
