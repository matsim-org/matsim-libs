package playground.dziemke.cemdapMatsimCadyts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.PlanBuilder;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.SimResults;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import playground.dziemke.cemdapMatsimCadyts.measurement.CadytsModule;
import playground.dziemke.cemdapMatsimCadyts.measurement.CadytsScoringSimplified;
import playground.dziemke.cemdapMatsimCadyts.measurement.PersoDistHistoModule;
import playground.dziemke.cemdapMatsimCadyts.measurement.PersoDistHistogram;

import javax.inject.Inject;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * @author gthunig on 21.02.2017.
 */
public class CadytsDistanceCalibrator {

    private static final Logger log = Logger.getLogger(CadytsDistanceCalibrator.class ) ;

    enum HistogramBin {
        B89000, B89200, B89400, B89600, B89800, B90000, B90200, B90400, B90600, B90800, B91000
    }

    public static void main(String[] args) {

        // Parameters
        Config config;
        double cadytsWeightLinks;
        double cadytsWeightHistogram;

        // Input and output
        String inputNetworkFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/network_diff_lengths2.xml";
//		String inputPlansFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans1000.xml";
        String inputPlansFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans1000_routes5.xml";
        String countsFileName = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/counts100-200_full.xml";
        String runId = "withoutCalibration";
        String outputDirectory = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/" + runId + "";

        if (args.length == 0) {
            // Config
            config = ConfigUtils.createConfig();
            config.controler().setLastIteration(100);
            config.controler().setWritePlansInterval(10);
            config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
            config.controler().setOutputDirectory(outputDirectory);
            config.counts().setInputFile(countsFileName);
//		    config.plans().setInputFile(inputPlansFile);
//		    config.network().setInputFile(inputNetworkFile);
//		    config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRate);

            cadytsWeightLinks = 0;
            cadytsWeightHistogram = 1000;
        } else {
            config = ConfigUtils.loadConfig(args[0]);
            cadytsWeightLinks = Double.parseDouble(args[1]);
            cadytsWeightHistogram = Double.parseDouble(args[2]);
        }

        // ... for randomizing router
//		final double sigma = 10.0; // The higher, the more randomness; 0.0 = no randomness
//		final double monetaryDistanceRate = -0.0002;

        log.info("----- Car: MarginalUtilityOfTraveling = " + config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
        log.info("----- Performing_utils = " + config.planCalcScore().getPerforming_utils_hr());
        log.info("----- Car: MonetaryDistanceRate = " + config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate());
        log.info("----- MarginalUtilityOfMoney = " + config.planCalcScore().getMarginalUtilityOfMoney());
        log.info("----- BrainExpBeta = " + config.planCalcScore().getBrainExpBeta());

        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
            stratSets.setStrategyName("ChangeExpBeta");
//			stratSets.setStrategyName("SelectRandom");
            stratSets.setWeight(0.8);
            config.strategy().addStrategySettings(stratSets);
        }
//		{
//			StrategySettings stratSets = new StrategySettings();
//			stratSets.setStrategyName("ReRoute");
//			stratSets.setWeight(0.2);
//			stratSets.setDisableAfter(70);
//			config.strategy().addStrategySettings(stratSets);
//		}

        // In case behavioral scoring is to be included, activities need to be defined
//		ActivityParams homeActivity = new ActivityParams("h");
//		homeActivity.setTypicalDuration(12*60*60);
//		config.planCalcScore().addActivityParams(homeActivity);
//
//		ActivityParams workActivity = new ActivityParams("w");
//		workActivity.setTypicalDuration(0.5*60*60);
//		config.planCalcScore().addActivityParams(workActivity);

        final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetworkFile);
        new PopulationReader(scenario).readFile(inputPlansFile);

        final Counts<Link> counts = new Counts<>();
        new CountsReaderMatsimV1(counts).readFile(countsFileName);
        scenario.addScenarioElement("calibrationCounts", counts);

        Controler controler = new Controler(scenario);

        cadytsWeightLinks = 0;
        cadytsWeightHistogram = 0;

        calibrate(controler, cadytsWeightLinks, cadytsWeightHistogram, 90000);
        PersonDistHistoAnalyzer.analyze(runId);
    }

    private static void calibrate(Controler controler, double cadytsWeightLinks ,double cadytsWeightHistogram, double distanceAverage) {

        // Randomizing router: Randomizes relation of time- and distance-based disutilities
//		final RandomizingTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore());
//		travelDisutilityFactory.setSigma(sigma);
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bindCarTravelDisutilityFactory().toInstance(travelDisutilityFactory);
//			}
//		});

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new CadytsModule());
                install(new PersoDistHistoModule());
            }
        });

        controler.addOverridingModule(new CadytsCarModule()); // required if org.matsim.contrib.cadyts.general.CadytsScoring is used

        // Add StartUpListener
        controler.addControlerListener((StartupListener) startupEvent -> {

            // Add AfterMobsimListener
            startupEvent.getServices().addControlerListener((AfterMobsimListener) afterMobsimEvent -> {
                PersoDistHistogram distService = afterMobsimEvent.getServices().getInjector().getInstance(PersoDistHistogram.class);
                HashMap<Id<Person>, Double> distances = distService.getDistances();

                distances.forEach((personId, v) -> {
                    double displacement = Math.abs(v-distanceAverage);
                    afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId, -cadytsWeightHistogram * displacement));
                });
            });
        });

        // Scoring
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            Config config;
            @Inject AnalyticalCalibrator cadyts;
            @Inject
            PlansTranslator plansTranslator;
//		    @Inject CadytsContext cadytsContext; // alternative
//		    @Inject CharyparNagelScoringParametersForPerson parameters;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();

                // Behavioral scoring
//		    	final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);
//		        sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
//				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                // Counts-based scoring
                final CadytsScoringSimplified<Link> scoringFunction = new CadytsScoringSimplified<Link>(person.getSelectedPlan(), config, plansTranslator, cadyts);
//		        final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cadytsContext); // alternative
                scoringFunction.setWeightOfCadytsCorrection(cadytsWeightLinks);
                sumScoringFunction.addScoringFunction(scoringFunction);

                // Distribution-based scoring (currently implemented via money events)
                sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(1.0));

                return sumScoringFunction;
            }
        });

        controler.run();
    }

    private static void calibrateWithDistanceBins(Controler controler, double cadytsWeightLinks ,double cadytsWeightHistogram, double distanceAverage) {

        // Randomizing router: Randomizes relation of time- and distance-based disutilities
//		final RandomizingTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore());
//		travelDisutilityFactory.setSigma(sigma);
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bindCarTravelDisutilityFactory().toInstance(travelDisutilityFactory);
//			}
//		});

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new CadytsModule());
                install(new PersoDistHistoModule());
            }
        });

        controler.addOverridingModule(new CadytsCarModule()); // required if org.matsim.contrib.cadyts.general.CadytsScoring is used

        // Add StartUpListener
        controler.addControlerListener((StartupListener) startupEvent -> {

            AnalyticalCalibrator<HistogramBin> calibrator = new AnalyticalCalibrator<>(startupEvent.getServices().getConfig().controler().getOutputDirectory() + "/cadyts-histogram.txt", MatsimRandom.getRandom().nextLong(), 24*60*60);
            calibrator.setStatisticsFile(startupEvent.getServices().getControlerIO().getOutputFilename("histogram-calibration-stats.txt"));
            calibrator.addMeasurement(HistogramBin.B89000, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B89200, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B89400, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B89600, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B89800, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B90000, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B90200, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B90400, 0, 24*60*60, 333, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B90600, 0, 24*60*60, 333, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B90800, 0, 24*60*60, 333, SingleLinkMeasurement.TYPE.COUNT_VEH);
            calibrator.addMeasurement(HistogramBin.B91000, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);

            // Add BeforeMobsimListener
            startupEvent.getServices().addControlerListener((BeforeMobsimListener) beforeMobsimEvent -> {
                for (Person person : beforeMobsimEvent.getServices().getScenario().getPopulation().getPersons().values()) {
                    PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
                    // TODO implement this also for distance checks for single trips
                    double totalPlannedDistance = 0.0;
                    for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                        if (planElement instanceof Leg) {
                            // TODO check if using these distances makes sense
                            totalPlannedDistance += ((Leg) planElement).getRoute().getDistance();
                        }
                    }
                    HistogramBin bin = HistogramBin.values()[(int) ((Math.min(totalPlannedDistance, 91000) - 89000) / 200)];
                    planBuilder.addTurn(bin, 0);
                    calibrator.addToDemand(planBuilder.getResult());
                }
            });

            // Add AfterMobsimListener
            startupEvent.getServices().addControlerListener((AfterMobsimListener) afterMobsimEvent -> {
                PersoDistHistogram distService = afterMobsimEvent.getServices().getInjector().getInstance(PersoDistHistogram.class);
                EnumMap<HistogramBin, Integer> frequencies = new EnumMap<>(HistogramBin.class);
                for (HistogramBin bin : HistogramBin.values()) {
                    frequencies.put(bin, 0);
                }
                HashMap<Id<Person>, Double> distances = distService.getDistances();
                distances.values().forEach(v -> {
                    HistogramBin bin = HistogramBin.values()[(int) ((Math.min(v, 91000) - 89000)/ 200)];
                    frequencies.put(bin, frequencies.get(bin) + 1);
                });
                calibrator.afterNetworkLoading(new SimResults<HistogramBin>() {
                    @Override
                    public double getSimValue(HistogramBin histogramBin, int startTime_s, int endTime_s, SingleLinkMeasurement.TYPE type) {
                        return frequencies.get(histogramBin);
                    }
                });
                distances.forEach((personId, v) -> {
                    PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
                    planBuilder.addTurn(HistogramBin.values()[(int) ((Math.min(v, 91000) - 89000) / 200)], 0);
                    double offset = calibrator.calcLinearPlanEffect(planBuilder.getResult());
//					log.info("########## Offset = " + offset + " -- personId = " + personId + " -- v = " + v);
                    afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId, cadytsWeightHistogram * offset));
                });
//                distances.forEach((personId, v) -> {
//                    double displacement = Math.abs(v-distanceAverage);
//                    afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId, -cadytsWeightHistogram * displacement));
//                });
            });
        });

        // Scoring
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            Config config;
            @Inject AnalyticalCalibrator cadyts;
            @Inject
            PlansTranslator plansTranslator;
//		    @Inject CadytsContext cadytsContext; // alternative
//		    @Inject CharyparNagelScoringParametersForPerson parameters;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();

                // Behavioral scoring
//		    	final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);
//		        sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
//				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                // Counts-based scoring
                final CadytsScoringSimplified<Link> scoringFunction = new CadytsScoringSimplified<Link>(person.getSelectedPlan(), config, plansTranslator, cadyts);
//		        final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cadytsContext); // alternative
                scoringFunction.setWeightOfCadytsCorrection(cadytsWeightLinks);
                sumScoringFunction.addScoringFunction(scoringFunction);

                // Distribution-based scoring (currently implemented via money events)
                sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(1.0));

                return sumScoringFunction;
            }
        });

        controler.run();
    }
}
