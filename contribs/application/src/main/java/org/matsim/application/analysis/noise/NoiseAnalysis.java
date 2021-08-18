package org.matsim.application.analysis.noise;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.noise.MergeNoiseCSVFile;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import picocli.CommandLine;

@CommandLine.Command(
        name = "noise-analysis",
        description = "Noise analysis",
        mixinStandardHelpOptions = true,
        showDefaultValues = true
)

public class NoiseAnalysis implements MATSimAppCommand {
    private static final Logger log = LogManager.getLogger(NoiseAnalysis.class);

    @CommandLine.Option(names = "--directory", description = "Path to run directory", required = true)
    private String runDirectory;

    @CommandLine.Option(names = "--runId", description = "Pattern to match runId.", defaultValue = "")
    private String runId;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();


    public static void main(String[] args) {
        System.exit(new CommandLine(new NoiseAnalysis()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
        config.global().setCoordinateSystem(crs.getInputCRS());
        if (runId != null) {
            runId = runId + ".";
            config.controler().setRunId(runId);
        }
        config.network().setInputFile(runDirectory + "/" + runId + "output_network.xml.gz");
        config.plans().setInputFile(runDirectory + runId + "output_plans.xml.gz");
        config.controler().setOutputDirectory(runDirectory);

        // adjust the default noise parameters
        NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
        noiseParameters.setReceiverPointGap(250);
        noiseParameters.setConsideredActivitiesForReceiverPointGridArray(new String[]{"h", "w", "home", "work"});
        noiseParameters.setConsideredActivitiesForDamageCalculationArray(new String[]{"h", "w", "home", "work"});
        CoordinateTransformation ct = shp.createInverseTransformation(crs.getInputCRS());
        // ...

        Scenario scenario = ScenarioUtils.loadScenario(config);

        String outputDirectory = runDirectory + "/analysis/";
        NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
        noiseCalculation.run();

        // some processing of the output data
        if (!outputDirectory.endsWith("/")) outputDirectory = outputDirectory + "/";

        String outputFilePath = outputDirectory + "noise-analysis/";
        ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
        process.run();

        final String[] labels = {"immission", "consideredAgentUnits", "damages_receiverPoint"};
        final String[] workingDirectories = {outputFilePath + "/immissions/", outputFilePath + "/consideredAgentUnits/", outputFilePath + "/damages_receiverPoint/"};

        MergeNoiseCSVFile merger = new MergeNoiseCSVFile();
        merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
        merger.setOutputDirectory(outputFilePath);
        merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
        merger.setWorkingDirectory(workingDirectories);
        merger.setLabel(labels);
        merger.run();

        return 0;
    }

}
