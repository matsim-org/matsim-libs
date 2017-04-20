package playground.dziemke.cemdapMatsimCadyts.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtOccupancyAnalyzerI;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizingTransitRouterTravelTimeAndDisutility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.dziemke.cemdapMatsimCadyts.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.Set;

/**
 * @author gthunig on 14.07.2016.
 */
public class RndPtRouterLauncherV2 {
	private enum Variant { orig, reduced }
	private static final Variant variant = Variant.orig ;

	private static Provider<TransitRouter> createRandomizedTransitRouterFactory (final TransitSchedule schedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return
				() -> {
					RandomizingTransitRouterTravelTimeAndDisutility ttCalculator =
							new RandomizingTransitRouterTravelTimeAndDisutility(trConfig);
					ttCalculator.setDataCollection(RandomizingTransitRouterTravelTimeAndDisutility.DataCollection.randomizedParameters, false) ;
					ttCalculator.setDataCollection(RandomizingTransitRouterTravelTimeAndDisutility.DataCollection.additionalInformation, false) ;
					return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
				};
	}

	public static void main(final String[] args) {

		int lastIteration = 500;
		double cadytsWeight = 30.0;

		String configFile;
		if (args.length == 0) {
			configFile = "../../../shared-svn/projects/ptManuel/calibration/moyo24hrs.xml";
		} else if (args.length == 1){
			configFile = args[0];
		} else {
			configFile = args[0];
			lastIteration = Integer.parseInt(args[1]);
			cadytsWeight = Double.parseDouble(args[2]);
		}
		final double finalCadytsWeight = cadytsWeight;

		Config config = ConfigUtils.loadConfig(configFile);

		config.controler().setLastIteration(lastIteration);
		config.ptCounts().setPtCountsInterval(5);

//		config.vspExperimental().setAbleToOverwritePtInteractionParams(true);
		// should work without this, otherwise something is wrong
		
		switch ( variant ) {
		case orig:
			break;
		case reduced:
			// relative to config.xml:
			config.plans().setInputFile("../../../studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_1x_subset_routed.xml.gz");
			break;
		default:
			throw new RuntimeException("not implemented") ;
		}
		
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		int lastStrategyIdx = config.strategy().getStrategySettings().size();
		if (lastStrategyIdx >= 1){
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}

		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
			stratSets.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
			stratSets.setStrategyName(DefaultStrategy.ReRoute.name()); 
			stratSets.setWeight(0.1);
			stratSets.setDisableAfter((new Double(lastIteration * 0.9)).intValue());
			config.strategy().addStrategySettings(stratSets);
		}
//		{
//			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
//			stratSets.setStrategyName(DefaultStrategy.ChangeSingleTripMode.name());
//			stratSets.setWeight(0.1);
//			stratSets.setDisableAfter((new Double(lastIteration * 0.9)).intValue());
//			config.strategy().addStrategySettings(stratSets);
//
//			config.changeMode().setIgnoreCarAvailability(true);
//			config.changeMode().setModes( new String[]{TransportMode.car, TransportMode.walk, TransportMode.pt });
//		}

		CadytsConfigGroup ccc = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class ) ;

		// ---
		
		final Scenario scn = ScenarioUtils.loadScenario(config);
		Set<String> items = new HashSet<>() ;
		switch ( variant ) {
		case orig:
			for ( TransitLine line : scn.getTransitSchedule().getTransitLines().values() ) {
				items.add( line.getId().toString() ) ;
			}
			break;
		case reduced:
			items.add("B-M44") ;
			break;
		default:
			break;
		
		}
		ccc.setCalibratedItems(items);
		

		// ---
		
		final Controler controler = new Controler(scn);

		//add analyzer for specific bus line and stop Zone conversion
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(true);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				bind(CadytsPtContext2.class).asEagerSingleton();
				addControlerListenerBinding().to(CadytsPtContext2.class);
				bind(CadytsPtOccupancyAnalyzerI.class).toInstance(ctrlListener4configurableOcuppAnalysis.getAnalyzer()) ;
			}
		});

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject private CadytsPtContext2 cadytsContext;
			@Inject ScoringParametersForPerson parameters;
			@Override public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<TransitStopFacility> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				final double cadytsScoringWeight = finalCadytsWeight * config.planCalcScore().getBrainExpBeta();
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
				sumScoringFunction.addScoringFunction(scoringFunction);

				return sumScoringFunction;
			}
		});

//		create the factory for rndizedRouter
		final TransitRouterConfig trConfig = new TransitRouterConfig(config);
		final TransitSchedule schedule = scn.getTransitSchedule();
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.getBeelineWalkConnectionDistance());
		final Provider<TransitRouter> randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (schedule, trConfig, routerNetwork);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(randomizedTransitRouterFactory);
			}
		});


		controler.run();
	}

}
