package playground.dziemke.cemdapMatsimCadyts;

import cadyts.calibrators.Calibrator;
import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.PlanBuilder;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.SimResults;
import org.apache.commons.math.util.MathUtils;
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
import java.util.*;

/**
 * @author gthunig on 21.02.2017.
 */
public class CadytsDistanceCalibrator {

    private static final Logger log = Logger.getLogger(CadytsDistanceCalibrator.class ) ;

//    enum HistogramBin {
//        B89000, B89200, B89400, B89600, B89800, B90000, B90200, B90400, B90600, B90800, B91000
//    }

    public static void main(String[] args) {

//        int poicum = 0;
//        for (int i = 0; i < 10; i++) {
//            double poisson = poisson(1, i);
//            System.out.println("poisson(4," + i + ") " + poisson);
//            poicum += poisson*1000;
//        }
//        System.out.println("poicum = " + poicum);


//        CadytsDistanceCalibrator calibrator = new CadytsDistanceCalibrator();
//        calibrator.run(args);

//        PersonDistHistoAnalyzer.analyze("testGT5poissonBerlin");

//      TODO work interrupted in favor to Amit Agarwal's work on distanceBins
    }

    private static double poisson(int averqageValue, int k) {
        return (Math.pow(averqageValue, k) / MathUtils.factorial(k)) * Math.pow(Math.E, -averqageValue);
    }

    public void run(String[] args) {

        // Parameters
        Config config;
        double cadytsWeightLinks;
        double cadytsWeightHistogram;

        // Input and output
//        String inputNetworkFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/network_diff_lengths2.xml";
////		String inputPlansFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans1000.xml";
//        String inputPlansFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans1000_routes5.xml";
//        String countsFileName = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/counts100-200_full.xml";
//        String runId = "testGT5poisson2";
//        String outputDirectory = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/" + runId + "";

        String inputNetworkFile = "C:\\Users\\gthunig\\Desktop\\cadytsDistanceBinCalculationBerlin/network_shortIds_v1.xml.gz";
        String inputPlansFile = "C:\\Users\\gthunig\\Desktop\\cadytsDistanceBinCalculationBerlin/100-plans_1pct.xml.gz";
        String countsFileName = "C:\\Users\\gthunig\\Desktop\\cadytsDistanceBinCalculationBerlin/vmz_di-do_shortIds_v1.xml";
        String runId = "testGT5poissonBerlin";
        String outputDirectory = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/" + runId + "";

        if (args.length == 0) {
            // Config
            config = ConfigUtils.createConfig();
            config.controler().setLastIteration(50);
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
        cadytsWeightHistogram = 1000;

        calibrate(controler, cadytsWeightLinks, cadytsWeightHistogram, 30000);

//        DistanceBinContainer container = new DistanceBinContainer(89200, 91000, 200, 89400, scenario.getPopulation().getPersons().size());
//        calibrateWithDistanceBins(controler, cadytsWeightLinks, cadytsWeightHistogram, container);
        PersonDistHistoAnalyzer.analyze(runId);
    }

    private void calibrate(Controler controler, double cadytsWeightLinks ,double cadytsWeightHistogram, double distanceAverage) {

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

    private void calibrateWithDistanceBins(Controler controler, double cadytsWeightLinks , double cadytsWeightHistogram, DistanceBinResults results) {

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

            Calibrator<DistanceBin> calibrator = new Calibrator<>(startupEvent.getServices().getConfig().controler().getOutputDirectory() + "/cadyts-histogram.txt", MatsimRandom.getRandom().nextLong(), 24*60*60);
            calibrator.setStatisticsFile(startupEvent.getServices().getControlerIO().getOutputFilename("histogram-calibration-stats.txt"));
            for (DistanceBin bin : results.getDistanceBins()) {
                calibrator.addMeasurement(bin, 0, 24*60*60, bin.getDesiredValue(), SingleLinkMeasurement.TYPE.COUNT_VEH);
            }

            // Add BeforeMobsimListener
            startupEvent.getServices().addControlerListener((BeforeMobsimListener) beforeMobsimEvent -> {
                for (Person person : beforeMobsimEvent.getServices().getScenario().getPopulation().getPersons().values()) {
                    PlanBuilder<DistanceBin> planBuilder = new PlanBuilder<>();
                    // TODO implement this also for distance checks for single trips
                    double totalPlannedDistance = 0.0;
                    for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                        if (planElement instanceof Leg) {
                            // TODO check if using these distances makes sense
                            totalPlannedDistance += ((Leg) planElement).getRoute().getDistance();
                        }
                    }
                    DistanceBin bin = results.getBinFromDistance((int)totalPlannedDistance);
                    planBuilder.addTurn(bin, 0);
                    calibrator.addToDemand(planBuilder.getResult());
                }
            });

            // Add AfterMobsimListener
            startupEvent.getServices().addControlerListener((AfterMobsimListener) afterMobsimEvent -> {
                PersoDistHistogram distService = afterMobsimEvent.getServices().getInjector().getInstance(PersoDistHistogram.class);
//                Map<DistanceBin, Integer> frequencies = new HashMap<>();
//                for (DistanceBin bin : results.getDistanceBins()) {
//                    frequencies.put(bin, 0);
//                }
                results.reset();
                HashMap<Id<Person>, Double> distances = distService.getDistances();
                distances.values().forEach(v -> {
                    results.raiseBinFromDistance(v.intValue());
//                    DistanceBin bin = results.getBinFromDistance(v.intValue());
//
//                    frequencies.put(bin, frequencies.get(bin) + 1);
                });
//                calibrator.afterNetworkLoading(new SimResults<DistanceBin>() {
//                    @Override
//                    public double getSimValue(DistanceBin histogramBin, int startTime_s, int endTime_s, SingleLinkMeasurement.TYPE type) {
//                        return frequencies.get(histogramBin);
//                    }
//                });
                calibrator.afterNetworkLoading(results);
                distances.forEach((personId, v) -> {
//                    DistanceBin bin = results.getBinFromDistance(v.intValue());
//                    int actualValue = frequencies.get(bin);
//                    int desiredValue = bin.getDesiredValue();
//                    double displacement = actualValue-desiredValue;
                    PlanBuilder<DistanceBin> planBuilder = new PlanBuilder<>();
                    planBuilder.addTurn(results.getBinFromDistance(v.intValue()), 0);
                    double offset = calibrator.calcLinearPlanEffect(planBuilder.getResult());
                    afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId,
                            cadytsWeightHistogram * offset));


//                    PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
//                    planBuilder.addTurn(HistogramBin.values()[(int) ((Math.min(v, 91000) - 89000) / 200)], 0);
//                    double offset = calibrator.calcLinearPlanEffect(planBuilder.getResult());
//					log.info("########## Offset = " + offset + " -- personId = " + personId + " -- v = " + v);
//                    afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId, cadytsWeightHistogram * offset));
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

    public class DistanceBinResults implements SimResults<DistanceBin> {

        private final int fromDist;
        private final int binSize;
        private final List<DistanceBin> distanceBins;

        public DistanceBinResults(int toDist, int binSize, int averageValue, int personNumber) {
            this(0, toDist, binSize, averageValue, personNumber);
        }

        public DistanceBinResults(int fromDist, int toDist, int binSize, int averageValue, int personNumber) {
            this.fromDist = fromDist;
            this.binSize = binSize;
            distanceBins = new ArrayList<>();
            int numberOfDistanceBins = (toDist - fromDist) / binSize;
            for (int i = 0; i < numberOfDistanceBins; i++) {
                int fromI = fromDist + (i * binSize);
                int averageI = (averageValue - fromDist) / binSize;
                double poisson = poisson(averageI, i);
                DistanceBin bin = new DistanceBin("B" + Integer.toString(fromI), i, (int) (poisson * personNumber));
                distanceBins.add(bin);
            }
        }

        DistanceBin getBinFromDistance(int distance) {
            int binNumber = ((distance - fromDist) / binSize);
            return get(binNumber);
        }

        DistanceBin get(int binNumer) {
            for (DistanceBin bin : distanceBins) {
                if (bin.getBinNumer() == binNumer) return bin;
            }
            return null;
        }

        void raiseBinFromDistance(int distance) {
            raiseBin(getBinFromDistance(distance));
        }

        void raiseBin(DistanceBin bin) {
            bin.raiseActualValue();
        }

        void reset() {
            for (DistanceBin bin : distanceBins) {
                bin.reset();
            }
        }

        List<DistanceBin> getDistanceBins() {
            return distanceBins;
        }

        @Override
        public double getSimValue(DistanceBin distanceBin, int i, int i1, SingleLinkMeasurement.TYPE type) {
            return distanceBin.getActualValue();
        }

        private double poisson(int averqageValue, int k) {
            return (Math.pow(averqageValue, k) / MathUtils.factorial(k)) * Math.pow(Math.E, -averqageValue);
        }
    }

    private class DistanceBin {

        private final String name;
        private final int binNumer;
        private final int desiredValue;
        private int actualValue;

        DistanceBin (String name, int binNumer, int desiredValue) {
            this.name = name;
            this.binNumer = binNumer;
            this.desiredValue = desiredValue;
            this.actualValue = 0;
        }

        private String getName() {
            return name;
        }

        private int getBinNumer() {
            return binNumer;
        }

        private int getDesiredValue() {
            return desiredValue;
        }

        private int getActualValue() { return actualValue; }

        private void raiseActualValue() { actualValue++; }

        private void reset() { actualValue = 0; }
    }
}
