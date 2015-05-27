package opdytsintegration.zurichtunnel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import opdytsintegration.MATSimDecisionVariableSetEvaluator;
import optdyts.ObjectiveFunction;

import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;

class TunnelMain {

	static void justRun() {

		final String configFileName = "./test/input/zurich/config_base-case.xml";

		System.out.println("STARTED ...");

		final File out = new File("test/input/zurich/output_base-case");
		if (out.exists()) {
			IOUtils.deleteDirectory(out);
		}

		final Controler controler = new Controler(configFileName);
		controler.run();

		System.out.println("... DONE.");

	}

	static void optimize() {

		final String configFileName = "./test/input/zurich/config_base-case.xml";

		System.out.println("STARTED ...");

		final Controler controler = new Controler(configFileName);

		final File out = new File("test/input/zurich/output_base-case");
		if (out.exists()) {
			IOUtils.deleteDirectory(out);
		}

		final Random rnd = new Random();

		final TunnelStateFactory stateFactory = new TunnelStateFactory(rnd);
		final ObjectiveFunction<TunnelState> objectiveFunction = new TunnelObjectiveFunction();

		// CREATE CANDIDATE DECISION VARIABLES

		final double vMax_km_h = 90;
		final int lanes = 2;

		final TunnelFactory tunnelFactory = new TunnelFactory(controler
				.getScenario().getNetwork());

		final Tunnel giesshuebel_Tiefenbrunn = tunnelFactory.newTunnel("983",
				"2529", lanes, vMax_km_h, "1000000", "1000001",
				"Giesshuebel_Tiefenbrunn");
		final Tunnel giesshuebel_Zollikon = tunnelFactory.newTunnel("983",
				"2526", lanes, vMax_km_h, "1000002", "1000003",
				"Giesshuebel_Zollikon");
		final Tunnel giesshuebel_Kuesnacht = tunnelFactory.newTunnel("983",
				"2525", lanes, vMax_km_h, "1000004", "1000005",
				"Giesshuebel_Kuesnacht");

		final Tunnel woellishofen_Tiefenbrunn = tunnelFactory.newTunnel("984",
				"2529", lanes, vMax_km_h, "1000006", "1000007",
				"Woellishofen_Tiefenbrunn");
		final Tunnel woellishofen_Zollikon = tunnelFactory.newTunnel("984",
				"2526", lanes, vMax_km_h, "1000008", "1000009",
				"Woellishofen_Zollikon");
		final Tunnel woellishofen_Kuesnacht = tunnelFactory.newTunnel("984",
				"2525", lanes, vMax_km_h, "1000010", "1000011",
				"Woellishofen_Kuesnacht");

		final Tunnel thalwil_Tiefenbrunn = tunnelFactory.newTunnel("486",
				"2529", lanes, vMax_km_h, "1000012", "1000013",
				"Thalwil_Tiefenbrunn");
		final Tunnel thalwil_Zollikon = tunnelFactory.newTunnel("486", "2526",
				lanes, vMax_km_h, "1000014", "1000015", "Thalwil_Zollikon");
		final Tunnel thalwil_Kuesnacht = tunnelFactory.newTunnel("486", "2525",
				lanes, vMax_km_h, "1000016", "1000017", "Thalwil_Kuesnacht");

		// System.exit(0);

		final Set<TunnelConfiguration> decisionVariables = new LinkedHashSet<TunnelConfiguration>();

		decisionVariables.add(new TunnelConfiguration(tunnelFactory));

		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				giesshuebel_Tiefenbrunn));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				giesshuebel_Zollikon));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				giesshuebel_Kuesnacht));

		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				woellishofen_Tiefenbrunn));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				woellishofen_Zollikon));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				woellishofen_Kuesnacht));

		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				thalwil_Tiefenbrunn));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				thalwil_Zollikon));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				thalwil_Kuesnacht));

		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				giesshuebel_Tiefenbrunn, giesshuebel_Zollikon,
				giesshuebel_Kuesnacht, woellishofen_Tiefenbrunn,
				woellishofen_Zollikon, woellishofen_Kuesnacht,
				thalwil_Tiefenbrunn, thalwil_Zollikon, thalwil_Kuesnacht));

		// AND RUN THE ENTIRE THING

		final double transitionNoiseVarianceScale = 0.01;
		final double convergenceNoiseVarianceScale = 0.01;

		final MATSimDecisionVariableSetEvaluator<TunnelState, TunnelConfiguration> predictor = new MATSimDecisionVariableSetEvaluator<TunnelState, TunnelConfiguration>(
				decisionVariables, objectiveFunction,
				transitionNoiseVarianceScale, convergenceNoiseVarianceScale,
				stateFactory);
		predictor.setLogFileName("tunnel-log.txt");
		predictor.setMemory(1);
		predictor.setBinSize_s(15 * 60);
		predictor.setBinCnt(24 * 4);

		controler.addControlerListener(predictor);
		controler.run();

		System.out.println("... DONE.");
	}

	public static void main(String[] args) throws FileNotFoundException {

		// justRun();
		optimize();

	}
}
