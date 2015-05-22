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

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final Controler controler = new Controler(
				"./test/input/zurich/config_base-case.xml");

		final File out = new File("test/input/zurich/output_base-case");
		if (out.exists()) {
			IOUtils.deleteDirectory(out);
		}

		final Random rnd = new Random();

		final TunnelStateFactory stateFactory = new TunnelStateFactory(rnd);
		final ObjectiveFunction<TunnelState> objectiveFunction = new TunnelObjectiveFunction();

		// CREATE CANDIDATE DECISION VARIABLES

		final TunnelFactory tunnelFactory = new TunnelFactory(controler
				.getScenario().getNetwork());

		final Tunnel giesshuebel_Tiefenbrunn = tunnelFactory.newTunnel("983",
				"2529", 2, 60.0, "1000000", "1000001",
				"Giesshuebel_Tiefenbrunn");
		final Tunnel giesshuebel_Zollikon = tunnelFactory.newTunnel("983",
				"2526", 2, 60.0, "1000002", "1000003", "Giesshuebel_Zollikon");
		final Tunnel giesshuebel_Kuesnacht = tunnelFactory.newTunnel("983",
				"2525", 2, 60.0, "1000004", "1000005", "Giesshuebel_Kuesnacht");

		final Tunnel goellishofen_Tiefenbrunn = tunnelFactory.newTunnel("984",
				"2529", 2, 60.0, "1000006", "1000007",
				"Woellishofen_Tiefenbrunn");
		final Tunnel goellishofen_Zollikon = tunnelFactory.newTunnel("984",
				"2526", 2, 60.0, "1000008", "1000009", "Woellishofen_Zollikon");
		final Tunnel goellishofen_Kuesnacht = tunnelFactory
				.newTunnel("984", "2525", 2, 60.0, "1000010", "1000011",
						"Woellishofen_Kuesnacht");

		final Tunnel thalwil_Tiefenbrunn = tunnelFactory.newTunnel("486",
				"2529", 2, 60.0, "1000012", "1000013", "Thalwil_Tiefenbrunn");
		final Tunnel thalwil_Zollikon = tunnelFactory.newTunnel("486", "2526",
				2, 60.0, "1000014", "1000015", "Thalwil_Zollikon");
		final Tunnel thalwil_Kuesnacht = tunnelFactory.newTunnel("486", "2525",
				2, 60.0, "1000016", "1000017", "Thalwil_Kuesnacht");

		final Set<TunnelConfiguration> decisionVariables = new LinkedHashSet<TunnelConfiguration>();

		decisionVariables.add(new TunnelConfiguration(tunnelFactory));

		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				giesshuebel_Tiefenbrunn));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				giesshuebel_Zollikon));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				giesshuebel_Kuesnacht));

		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				goellishofen_Tiefenbrunn));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				goellishofen_Zollikon));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				goellishofen_Kuesnacht));

		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				thalwil_Tiefenbrunn));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				thalwil_Zollikon));
		decisionVariables.add(new TunnelConfiguration(tunnelFactory,
				thalwil_Kuesnacht));

		// AND RUN THE ENTIRE THING

		final double convergenceNoiseVarianceScale = 0.05;

		// vary this and see how the solution changes: 0.05, 0.025, 0.0125, ...
		// use the smallest value for "validation runs" with one policy each
		final double transitionNoiseVarianceScale = 0.05;

		final MATSimDecisionVariableSetEvaluator<TunnelState, TunnelConfiguration> predictor = new MATSimDecisionVariableSetEvaluator<TunnelState, TunnelConfiguration>(
				decisionVariables, objectiveFunction,
				transitionNoiseVarianceScale, convergenceNoiseVarianceScale,
				stateFactory);
		controler.addControlerListener(predictor);
		controler.run();

		System.out.println("... DONE.");

	}

}
