package org.matsim.core.replanning.annealing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ReplanningAnnealerTest {

    private static final String FILENAME_ANNEAL = "annealingRates.txt";
    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();
    private Scenario scenario;
    private Config config;
    private ReplanningAnnealerConfigGroup saConfig;
    private ReplanningAnnealerConfigGroup.AnnealingVariable saConfigVar;
    private String expectedLinearAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000\n" +
                    "1;0.4500;0.2250;0.2250;0.5500\n" +
                    "2;0.4000;0.2000;0.2000;0.6000\n" +
                    "3;0.3500;0.1750;0.1750;0.6500\n" +
                    "4;0.3000;0.1500;0.1500;0.7000\n" +
                    "5;0.2500;0.1250;0.1250;0.7500\n" +
                    "6;0.2000;0.1000;0.1000;0.8000\n" +
                    "7;0.1500;0.0750;0.0750;0.8500\n" +
                    "8;0.1000;0.0500;0.0500;0.9000\n" +
                    "9;0.0500;0.0250;0.0250;0.9500\n" +
                    "10;0.0000;0.0000;0.0000;1.0000\n";
    private String expectedMsaAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000\n" +
                    "1;0.5000;0.2500;0.2500;0.5000\n" +
                    "2;0.2500;0.1250;0.1250;0.7500\n" +
                    "3;0.1667;0.0833;0.0833;0.8333\n" +
                    "4;0.1250;0.0625;0.0625;0.8750\n" +
                    "5;0.1000;0.0500;0.0500;0.9000\n" +
                    "6;0.0833;0.0417;0.0417;0.9167\n" +
                    "7;0.0714;0.0357;0.0357;0.9286\n" +
                    "8;0.0625;0.0313;0.0313;0.9375\n" +
                    "9;0.0556;0.0278;0.0278;0.9444\n" +
                    "10;0.0500;0.0250;0.0250;0.9500\n";
    private String expectedGeometricAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000\n" +
                    "1;0.4500;0.2250;0.2250;0.5500\n" +
                    "2;0.4050;0.2025;0.2025;0.5950\n" +
                    "3;0.3645;0.1823;0.1823;0.6355\n" +
                    "4;0.3281;0.1640;0.1640;0.6719\n" +
                    "5;0.2952;0.1476;0.1476;0.7048\n" +
                    "6;0.2657;0.1329;0.1329;0.7343\n" +
                    "7;0.2391;0.1196;0.1196;0.7609\n" +
                    "8;0.2152;0.1076;0.1076;0.7848\n" +
                    "9;0.1937;0.0969;0.0969;0.8063\n" +
                    "10;0.1743;0.0872;0.0872;0.8257\n";
    private String expectedExponentialAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000\n" +
                    "1;0.4094;0.2047;0.2047;0.5906\n" +
                    "2;0.3352;0.1676;0.1676;0.6648\n" +
                    "3;0.2744;0.1372;0.1372;0.7256\n" +
                    "4;0.2247;0.1123;0.1123;0.7753\n" +
                    "5;0.1839;0.0920;0.0920;0.8161\n" +
                    "6;0.1506;0.0753;0.0753;0.8494\n" +
                    "7;0.1233;0.0616;0.0616;0.8767\n" +
                    "8;0.1009;0.0505;0.0505;0.8991\n" +
                    "9;0.0826;0.0413;0.0413;0.9174\n" +
                    "10;0.0677;0.0338;0.0338;0.9323\n";
    private String expectedSigmoidAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000\n" +
                    "1;0.4910;0.2455;0.2455;0.5090\n" +
                    "2;0.4763;0.2381;0.2381;0.5237\n" +
                    "3;0.4404;0.2202;0.2202;0.5596\n" +
                    "4;0.3656;0.1828;0.1828;0.6344\n" +
                    "5;0.2501;0.1250;0.1250;0.7500\n" +
                    "6;0.1345;0.0673;0.0673;0.8655\n" +
                    "7;0.0597;0.0298;0.0298;0.9403\n" +
                    "8;0.0238;0.0119;0.0119;0.9762\n" +
                    "9;0.0091;0.0045;0.0045;0.9909\n" +
                    "10;0.0034;0.0017;0.0017;0.9966\n";
    private String expectedParameterAnneal =
            "it;BrainExpBeta\n" +
                    "0;10.0000\n" +
                    "1;9.0000\n" +
                    "2;8.0000\n" +
                    "3;7.0000\n" +
                    "4;6.0000\n" +
                    "5;5.0000\n" +
                    "6;4.0000\n" +
                    "7;3.0000\n" +
                    "8;2.0000\n" +
                    "9;1.0000\n" +
                    "10;0.0000\n";
    private String expectedTwoParameterAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta;BrainExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000;10.0000\n" +
                    "1;0.5000;0.2500;0.2500;0.5000;9.0000\n" +
                    "2;0.2500;0.1250;0.1250;0.7500;8.0000\n" +
                    "3;0.1667;0.0833;0.0833;0.8333;7.0000\n" +
                    "4;0.1250;0.0625;0.0625;0.8750;6.0000\n" +
                    "5;0.1000;0.0500;0.0500;0.9000;5.0000\n" +
                    "6;0.0833;0.0417;0.0417;0.9167;4.0000\n" +
                    "7;0.0714;0.0357;0.0357;0.9286;3.0000\n" +
                    "8;0.0625;0.0313;0.0313;0.9375;2.0000\n" +
                    "9;0.0556;0.0278;0.0278;0.9444;1.0000\n" +
                    "10;0.0500;0.0250;0.0250;0.9500;0.0000\n";
    private String expectedFreezeEarlyAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000\n" +
                    "1;0.5000;0.2500;0.2500;0.5000\n" +
                    "2;0.2500;0.1250;0.1250;0.7500\n" +
                    "3;0.1667;0.0833;0.0833;0.8333\n" +
                    "4;0.1250;0.0625;0.0625;0.8750\n" +
                    "5;0.1000;0.0500;0.0500;0.9000\n" +
                    "6;0.1000;0.0500;0.0500;0.9000\n" +
                    "7;0.1000;0.0500;0.0500;0.9000\n" +
                    "8;0.1000;0.0500;0.0500;0.9000\n" +
                    "9;0.1000;0.0500;0.0500;0.9000\n" +
                    "10;0.1000;0.0500;0.0500;0.9000\n";
    private String expectedInnovationSwitchoffAnneal =
            "it;globalInnovationRate;ReRoute;SubtourModeChoice;ChangeExpBeta\n" +
                    "0;0.5000;0.2500;0.2500;0.5000\n" +
                    "1;0.5000;0.2500;0.2500;0.5000\n" +
                    "2;0.2500;0.1250;0.1250;0.7500\n" +
                    "3;0.1667;0.0833;0.0833;0.8333\n" +
                    "4;0.1250;0.0625;0.0625;0.8750\n" +
                    "5;0.1000;0.0500;0.0500;0.9000\n" +
                    "6;0.0000;0.0000;0.0000;1.0000\n" +
                    "7;0.0000;0.0000;0.0000;1.0000\n" +
                    "8;0.0000;0.0000;0.0000;1.0000\n" +
                    "9;0.0000;0.0000;0.0000;1.0000\n" +
                    "10;0.0000;0.0000;0.0000;1.0000\n";

    private static String readResult(String filePath) throws IOException {
        BufferedReader br = IOUtils.getBufferedReader(filePath);
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }

        return sb.toString();
    }

    @BeforeEach
    public void setup() {
        this.config = ConfigUtils.createConfig();
        config.global().setDefaultDelimiter(";");
        this.saConfig = config.replanningAnnealer();
        this.saConfig.setActivateAnnealingModule(true);
        this.saConfigVar = new ReplanningAnnealerConfigGroup.AnnealingVariable();
        this.saConfig.addAnnealingVariable(this.saConfigVar);

        ReplanningConfigGroup.StrategySettings s1 = new ReplanningConfigGroup.StrategySettings();
        s1.setStrategyName("ReRoute");
        s1.setWeight(0.2);
        this.config.replanning().addStrategySettings(s1);
        ReplanningConfigGroup.StrategySettings s2 = new ReplanningConfigGroup.StrategySettings();
        s2.setStrategyName("SubtourModeChoice");
        s2.setWeight(0.2);
        this.config.replanning().addStrategySettings(s2);
        ReplanningConfigGroup.StrategySettings s3 = new ReplanningConfigGroup.StrategySettings();
        s3.setStrategyName("ChangeExpBeta"); // shouldn't be affected
        s3.setWeight(0.5);
        this.config.replanning().addStrategySettings(s3);

        this.config.controller().setCreateGraphs(false);
        this.config.controller().setDumpDataAtEnd(false);
        this.config.controller().setWriteEventsInterval(0);
        this.config.controller().setWritePlansInterval(0);
        this.config.controller().setWriteSnapshotsInterval(0);
        this.config.controller().setLastIteration(10);
        this.config.controller().setOutputDirectory(this.utils.getOutputDirectory() + "annealOutput");

        this.scenario = ScenarioUtils.createScenario(this.config);
    }

	@Test
	void testLinearAnneal() throws IOException {
        this.saConfigVar.setAnnealType("linear");
        this.saConfigVar.setEndValue(0.0);
        this.saConfigVar.setStartValue(0.5);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedLinearAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testMsaAnneal() throws IOException {
        this.saConfigVar.setAnnealType("msa");
        this.saConfigVar.setShapeFactor(1.0);
        this.saConfigVar.setStartValue(0.5);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedMsaAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testGeometricAnneal() throws IOException {
        this.saConfigVar.setAnnealType("geometric");
        this.saConfigVar.setShapeFactor(0.9);
        this.saConfigVar.setStartValue(0.5);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedGeometricAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testExponentialAnneal() throws IOException {
        this.saConfigVar.setAnnealType("exponential");
        this.saConfigVar.setHalfLife(0.5);
        this.saConfigVar.setStartValue(0.5);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedExponentialAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testSigmoidAnneal() throws IOException {
        this.saConfigVar.setAnnealType("sigmoid");
        this.saConfigVar.setHalfLife(0.5);
        this.saConfigVar.setShapeFactor(1.0);
        this.saConfigVar.setStartValue(0.5);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedSigmoidAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testParameterAnneal() throws IOException {
        this.saConfigVar.setAnnealType("linear");
        this.saConfigVar.setAnnealParameter("BrainExpBeta");
        this.saConfigVar.setEndValue(0.0);
        this.saConfigVar.setStartValue(10.0);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedParameterAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));
        Assertions.assertEquals(0.0, controler.getConfig().scoring().getBrainExpBeta(), 1e-4);
    }

	@Test
	void testTwoParameterAnneal() throws IOException {
        this.saConfigVar.setAnnealType("msa");
        this.saConfigVar.setShapeFactor(1.0);
        this.saConfigVar.setStartValue(0.5);

        ReplanningAnnealerConfigGroup.AnnealingVariable otherAv = new ReplanningAnnealerConfigGroup.AnnealingVariable();
        otherAv.setAnnealType("linear");
        otherAv.setEndValue(0.0);
        otherAv.setAnnealParameter("BrainExpBeta");
        otherAv.setStartValue(10.0);
        this.saConfig.addAnnealingVariable(otherAv);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedTwoParameterAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));
        Assertions.assertEquals(0.0, controler.getConfig().scoring().getBrainExpBeta(), 1e-4);

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testInnovationSwitchoffAnneal() throws IOException {
        this.config.replanning().setFractionOfIterationsToDisableInnovation(0.5);
        this.saConfigVar.setAnnealType("msa");
        this.saConfigVar.setShapeFactor(1.0);
        this.saConfigVar.setStartValue(0.5);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedInnovationSwitchoffAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testFreezeEarlyAnneal() throws IOException {
        this.saConfigVar.setAnnealType("msa");
        this.saConfigVar.setShapeFactor(1.0);
        this.saConfigVar.setEndValue(0.1);
        this.saConfigVar.setStartValue(0.5);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedFreezeEarlyAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(null);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

	@Test
	void testSubpopulationAnneal() throws IOException {
        String targetSubpop = "subpop";
        this.saConfigVar.setAnnealType("linear");
        this.saConfigVar.setEndValue(0.0);
        this.saConfigVar.setStartValue(0.5);
        this.saConfigVar.setDefaultSubpopulation(targetSubpop);
        this.config.replanning().getStrategySettings().forEach(s -> s.setSubpopulation(targetSubpop));
        ReplanningConfigGroup.StrategySettings s = new ReplanningConfigGroup.StrategySettings();
        s.setStrategyName("TimeAllocationMutator");
        s.setWeight(0.25);
        s.setSubpopulation("noAnneal");
        this.config.replanning().addStrategySettings(s);

        Controler controler = new Controler(this.scenario);
        controler.run();

        Assertions.assertEquals(expectedLinearAnneal, readResult(controler.getControlerIO().getOutputFilename(FILENAME_ANNEAL)));

        StrategyManager sm = controler.getInjector().getInstance(StrategyManager.class);
        List<Double> weights = sm.getWeights(targetSubpop);

        Assertions.assertEquals(1.0, weights.stream().mapToDouble(Double::doubleValue).sum(), 1e-4);
    }

}
