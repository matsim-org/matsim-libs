package playground.dziemke.cemdapMatsimCadyts.pt;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.contrib.cadyts.pt.CadytsPtModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * @author gthunig on 14.07.2016.
 */
public class RunCadyts4PtExample {

	public static void main(String[] args) {

		final int lastIteration = 100;

		String inputDir = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/pt/input/";
		String outputDir = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/pt/blupp/output/Cadyts4PtExample/";

		final Config config = createConfig(inputDir, outputDir);

		config.controler().setLastIteration(lastIteration);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
		stratSets.setStrategyName("ChangeExpBeta");
		stratSets.setWeight(1.0);
		config.strategy().addStrategySettings(stratSets);

		// ===

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.addOverridingModule(new CadytsPtModule());

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject ScoringParametersForPerson parameters;
			@Inject Network network;
			@Inject CadytsPtContext cContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<TransitStopFacility> scoringFunction = new CadytsScoring<>(person.getSelectedPlan() ,config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(950 * config.planCalcScore().getBrainExpBeta());
				scoringFunctionAccumulator.addScoringFunction(scoringFunction);

				return scoringFunctionAccumulator;
			}
		}) ;


		controler.run();
	}

	private static Config createConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig();
		// ---
		config.global().setRandomSeed(4711);
		// ---
		config.network().setInputFile(inputDir + "network.xml");
		// ---
		config.plans().setInputFile(inputDir + "plans.xml");
		// ---
		config.transit().setUseTransit(true);
		// ---
		config.controler().setOutputDirectory(outputDir);
		config.controler().setWriteEventsInterval(1);
		config.controler().setMobsim(ControlerConfigGroup.MobsimType.qsim.toString());
		// ---

		config.qsim().setFlowCapFactor(0.02);
		config.qsim().setStorageCapFactor(0.06);
		config.qsim().setStuckTime(10.);
		config.qsim().setRemoveStuckVehicles(false); // ??
		// ---
		config.transit().setTransitScheduleFile(inputDir + "transitSchedule.xml");
		config.transit().setVehiclesFile(inputDir + "vehicles.xml");
		Set<String> modes = new HashSet<>();
		modes.add("pt");
		config.transit().setTransitModes(modes);
		// ---
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("h");
			config.planCalcScore().addActivityParams(params );
			params.setTypicalDuration(12*60*60.);
		}{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("w");
			config.planCalcScore().addActivityParams(params );
			params.setTypicalDuration(8*60*60.);
		}
		// ---
		ConfigGroup cadytsPtConfig = config.createModule(CadytsConfigGroup.GROUP_NAME);

//		cadytsPtConfig.addParam(CadytsConfigGroup.START_TIME, "04:00:00");
//		cadytsPtConfig.addParam(CadytsConfigGroup.END_TIME, "20:00:00");
//		cadytsPtConfig.addParam(CadytsConfigGroup.REGRESSION_INERTIA, "0.95");
//		cadytsPtConfig.addParam(CadytsConfigGroup.USE_BRUTE_FORCE, "true");
//		cadytsPtConfig.addParam(CadytsConfigGroup.MIN_FLOW_STDDEV, "8");
//		cadytsPtConfig.addParam(CadytsConfigGroup.PREPARATORY_ITERATIONS, "1");
//		cadytsPtConfig.addParam(CadytsConfigGroup.TIME_BIN_SIZE, "3600");
		cadytsPtConfig.addParam(CadytsConfigGroup.CALIBRATED_LINES, "B1,B2");

		CadytsConfigGroup ccc = new CadytsConfigGroup();
		config.addModule(ccc);


		// ---
		config.ptCounts().setOccupancyCountsFileName(inputDir + "counts/counts_occupancy.xml");
		config.ptCounts().setBoardCountsFileName(inputDir + "counts/counts_boarding.xml");
		config.ptCounts().setAlightCountsFileName(inputDir + "counts/counts_alighting.xml");
		config.ptCounts().setOutputFormat("txt");
		config.ptCounts().setCountsScaleFactor(1.);
		// ---
		return config;
	}

}
