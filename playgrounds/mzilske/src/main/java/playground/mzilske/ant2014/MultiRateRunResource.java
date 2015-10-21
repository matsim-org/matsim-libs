package playground.mzilske.ant2014;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;

import playground.mzilske.cdr.CallBehavior;
import playground.mzilske.cdr.CallBehaviorModule;
import playground.mzilske.cdr.CollectSightingsModule;
import playground.mzilske.cdr.LinkIsZone;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.PowerPlans;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.ZoneTracker;

class MultiRateRunResource {

    private String WD;

    private String regime;

    private String alternative;

    public MultiRateRunResource(String wd, String regime, String alternative) {
        this.WD = wd;
        this.regime = regime;
        this.alternative = alternative;
    }

    final static int TIME_BIN_SIZE = 60*60;
    final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;

    public Collection<String> getRates() {
        final List<String> RATES = new ArrayList<String>();
        RATES.add(Integer.toString(0));
        RATES.add(Integer.toString(2));
        RATES.add(Integer.toString(5));
        RATES.add(Integer.toString(10));
        RATES.add(Integer.toString(20));
        RATES.add(Integer.toString(30));
        RATES.add(Integer.toString(40));
        RATES.add(Integer.toString(50));
        RATES.add(Integer.toString(100));
        RATES.add(Integer.toString(150));
        RATES.add("activity");
        RATES.add("contbaseplans");
        return RATES;
    }

    private RunResource getBaseRun() {
        return new RegimeResource(WD + "/../..", regime).getBaseRun();
    }

    public void rate(String string) {
        Scenario scenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        if (string.equals("contbaseplans")) {
            runContinuedBasePlans(scenario);
        } else if (string.equals("actevents")) {
            runPhoneOnActivityStartEnd(scenario);
        } else {
            int rate = Integer.parseInt(string);
            runRate(scenario, rate);
        }
    }

    public void allRates() {
        for (String rate : getRates()) {
            rate(rate);
        }
    }

    private Config phoneConfigCongested() {
        Config config = ConfigUtils.createConfig();
        ActivityParams sightingParam = new ActivityParams("sighting");
        sightingParam.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(sightingParam);
        final double traveling = -6;
        config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.controler().setWritePlansInterval(1);
        config.controler().setLastIteration(20);
        QSimConfigGroup tmp = config.qsim();
        tmp.setFlowCapFactor(0.02);
        tmp.setStorageCapFactor(0.06);
        tmp.setRemoveStuckVehicles(false);
        tmp.setStuckTime(10.0);
        {
            StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
            stratSets.setStrategyName("ChangeExpBeta");
            stratSets.setWeight(0.7);
            config.strategy().addStrategySettings(stratSets);
        }
        {
            StrategySettings stratSets = new StrategySettings(Id.create(2, StrategySettings.class));
            stratSets.setStrategyName("ReRoute");
            stratSets.setWeight(0.3);
            config.strategy().addStrategySettings(stratSets);
        }
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        return config;
    }

    private static Config phoneConfigUncongested() {
        Config config = ConfigUtils.createConfig();
        ActivityParams sightingParam = new ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        config.controler().setWritePlansInterval(1);
        config.planCalcScore().addActivityParams(sightingParam);
        final double traveling = -6;
        config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
        config.planCalcScore().setPerforming_utils_hr(0);
        double travelingOtherUtilsHr = -6;
        config.planCalcScore().getModes().get(TransportMode.other).setMarginalUtilityOfTraveling(travelingOtherUtilsHr);
        config.planCalcScore().getModes().get(TransportMode.car).setConstant((double) 0);
        config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate((double) 0);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.controler().setLastIteration(0);
        QSimConfigGroup tmp = config.qsim();
        tmp.setFlowCapFactor(100);
        tmp.setStorageCapFactor(100);
        tmp.setRemoveStuckVehicles(false);
        tmp.setStuckTime(10.0);
        {
            StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
            stratSets.setStrategyName("ChangeExpBeta");
            stratSets.setWeight(0.7);
            config.strategy().addStrategySettings(stratSets);
        }
        {
            StrategySettings stratSets = new StrategySettings(Id.create(2, StrategySettings.class));
            stratSets.setStrategyName("ReRoute");
            stratSets.setWeight(0.3);
            config.strategy().addStrategySettings(stratSets);
        }
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        return config;
    }

    private void runPhoneOnActivityStartEnd(Scenario baseScenario) {
        EventsManager events = EventsUtils.createEventsManager();
        LinkIsZone linkIsZone = new LinkIsZone();
        AbstractModule phoneModule = new CallBehaviorModule(new CallBehavior() {

            @Override
            public boolean makeACall(ActivityEndEvent event) {
                return true;
            }

            @Override
            public boolean makeACall(ActivityStartEvent event) {
                return true;
            }

            @Override
            public boolean makeACallAtMorningAndNight(Id<Person> id) {
                return true;
            }

        }, linkIsZone);
        ReplayEvents.Results results = ReplayEvents.run(
                baseScenario,
                getBaseRun().getLastIteration().getEventsFileName(),
                new CollectSightingsModule(),
                phoneModule);


        final Sightings sightings = results.get(Sightings.class);

        final Scenario scenario = new ScenarioBuilder(baseScenario.getConfig()).setNetwork(baseScenario.getNetwork()).build() ;
        scenario.getConfig().controler().setOutputDirectory(WD + "/rates/actevents");
        PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario, linkIsZone, sightings);
        PopulationFromSightings.preparePopulation(scenario, linkIsZone, sightings);

        Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.run();
    }

    private void runRate(final Scenario baseScenario, final int dailyRate) {
        final RunResource run = getRateRun(Integer.toString(dailyRate)); // The run we are producing
        CallBehavior callBehavior = new CallBehavior() {

            @Override
            public boolean makeACall(ActivityEndEvent event) {
                return false;
            }

            @Override
            public boolean makeACall(ActivityStartEvent event) {
                return false;
            }

            @Override
            public boolean makeACallAtMorningAndNight(Id<Person> id) {
                return false;
            }

        };


        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
        ReplayEvents.Results results = ReplayEvents.run(
                baseScenario,
                getBaseRun().getLastIteration().getEventsFileName(),
                new VolumesAnalyzerModule(),
                new CollectSightingsModule(),
                new CallBehaviorModule(callBehavior, linkToZoneResolver));

        final Config config = phoneConfig();
        config.controler().setOutputDirectory(WD + "/rates/" + dailyRate);
        final Scenario scenario = new ScenarioBuilder(config).setNetwork(baseScenario.getNetwork()).build() ;
        PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario, linkToZoneResolver, results.get(Sightings.class));
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, results.get(Sightings.class));
        final double flowCapacityFactor = config.qsim().getFlowCapFactor();
        final double storageCapacityFactor = config.qsim().getStorageCapFactor();
        Controler controler = new Controler(scenario);
        if (alternative.equals("sense")) {
            final double travelledKilometersBase = sum(PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork()).values());
            controler.addControlerListener(new BeforeMobsimListener() {
                @Override
                public void notifyBeforeMobsim(BeforeMobsimEvent event) {
                    if (event.getIteration() > config.controler().getFirstIteration()) {
                        Population previous = run.getIteration(event.getIteration() - 1).getExperiencedPlans();
                        double distanceSum = sum(PowerPlans.travelledDistancePerPerson(previous, baseScenario.getNetwork()).values());
                        double sensedSampleSize = distanceSum / travelledKilometersBase;
                        config.qsim().setFlowCapFactor(flowCapacityFactor * sensedSampleSize);
                        config.qsim().setStorageCapFactor(storageCapacityFactor * sensedSampleSize);
                    }
                    final String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "config.xml.gz");
                    new ConfigWriter(config).write(filename);
                }
            });
        }

        controler.getConfig().controler().setCreateGraphs(false);
        controler.run();

    }

    private Config phoneConfig() {
        if (regime.equals("congested")) {
            return phoneConfigCongested();
        } else if (regime.equals("uncongested")) {
            return phoneConfigUncongested();
        }
        throw new RuntimeException("Unknown regime");
    }

    private void runContinuedBasePlans(Scenario baseScenario) {
        Config config = phoneConfig();
        for (ActivityParams params : baseScenario.getConfig().planCalcScore().getActivityParams()) {
            ActivityParams zero = new ActivityParams(params.getActivityType());
            zero.setScoringThisActivityAtAll(false);
            config.planCalcScore().addActivityParams(zero);
        }
        config.controler().setOutputDirectory(WD + "/rates/contbaseplans");

        Scenario scenario = new ScenarioBuilder(config).setNetwork( baseScenario.getNetwork()).build();

        for (Person basePerson : baseScenario.getPopulation().getPersons().values()) {
            Person person = scenario.getPopulation().getFactory().createPerson(basePerson.getId());
            PlanImpl planImpl = (PlanImpl) scenario.getPopulation().getFactory().createPlan();
            planImpl.copyFrom(basePerson.getSelectedPlan());
            person.addPlan(planImpl);
            scenario.getPopulation().addPerson(person);
        }

        Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.run();
    }

    public void distances() {
        final String filename = WD + "/distances.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        EventsManager events = EventsUtils.createEventsManager();
        VolumesAnalyzer baseVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
        events.addHandler(baseVolumes);
        new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());
        final double baseSum = PowerPlans.drivenKilometersWholeDay(baseScenario, baseVolumes);
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("callrate\troutesum\tvolumesum\tvolumesumdiff\n");
                for (String rate : getRates()) {
                    final IterationResource lastIteration = getRateRun(rate).getLastIteration();
                    Scenario scenario = lastIteration.getExperiencedPlansAndNetwork();
                    double km = sum(PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork()).values());
                    EventsManager events1 = EventsUtils.createEventsManager();
                    VolumesAnalyzer volumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
                    events1.addHandler(volumes);
                    new MatsimEventsReader(events1).readFile(lastIteration.getEventsFileName());
                    double sum = PowerPlans.drivenKilometersWholeDay(baseScenario, volumes);
                    pw.printf("%s\t%f\t%f\t%f\n", rate, km, sum, baseSum - sum);
                    pw.flush();
                }
            }
        });
    }

    public void distances2() {
        final String filename = WD + "/distances2.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        final double baseKm = sum(PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork()).values());
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("regime\tcallrate\troutesum\trouterate\n");
                pw.printf("%s\t%s\t%f\t%f\n",
                        regime, "base", baseKm, baseKm/baseKm);
                for (String rate : getRates()) {
                    final IterationResource lastIteration = getRateRun(rate).getLastIteration();
                    Scenario scenario = lastIteration.getExperiencedPlansAndNetwork();
                    double km = sum(PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork()).values());
                    pw.printf("%s\t%s\t%f\t%f\n",
                            regime, rate, km, km/baseKm);
                    pw.flush();
                }
            }
        });
    }

    public RunResource getRateRun(String rate) {
        return new RunResource(WD + "/rates/" + rate, null);
    }

    public void personKilometers() {
        final String filename = WD + "/person-kilometers.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                final Map<Id, Double> distancePerPersonBase = PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork());
                pw.printf("regime\trate\tperson\tkilometers-base\tkilometers\n");
                for (String rate : getRates()) {
                    Scenario scenario = getRateRun(rate).getLastIteration().getExperiencedPlansAndNetwork();
                    final Map<Id, Double> distancePerPerson = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), scenario.getNetwork());
                    for (Person person : baseScenario.getPopulation().getPersons().values()) {
                        pw.printf("%s\t%s\t%s\t%f\t%f\n",
                                regime, rate, person.getId().toString(),
                                zeroForNull(distancePerPersonBase.get(person.getId())),
                                zeroForNull(distancePerPerson.get(person.getId())));
                    }
                    pw.flush();
                }
            }
        });
    }

    public void detourFactor() {
        final String filename = WD + "/detour-factor.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        final double travelledBase = sum(PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork()).values());
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                for (String rate : getRates()) {
                    Scenario scenario = getRateRun(rate).getLastIteration().getExperiencedPlansAndNetwork();
                    TripRouter tripRouter = TripRouterFactoryBuilderWithDefaults.createTripRouterProvider(
                            scenario,
                            new DijkstraFactory(),
                            null)
                            .get();
                    double travelled = sum(PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), scenario.getNetwork()).values());
                    double freespeedDistances = 0.0;
                    for (Person person : scenario.getPopulation().getPersons().values()) {
                        double freespeedDistance = PowerPlans.distance(scenario.getNetwork(), tripRouter, person.getSelectedPlan().getPlanElements());
                        freespeedDistances += freespeedDistance;
                    }
                    double detourFactor = travelled / freespeedDistances;
                    double reconstructionWithoutDetours = freespeedDistances / travelledBase;
                    pw.printf("%s\t%s\t%f\t%f\t%f\t%f\n",
                            regime, rate, travelled, freespeedDistances, detourFactor, reconstructionWithoutDetours);
                    pw.flush();
                }
            }
        });
    }


    private double sum(Collection<Double> values) {
        double result = 0.0;
        for (double summand : values) {
            result += summand;
        }
        return result;
    }



    public void permutations() {
        File file = new File(WD + "/permutations.txt");
        Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        PowerPlans.writePermutations(baseScenario, file);
    }

    private static Double zeroForNull(Double maybeDouble) {
        if (maybeDouble == null) {
            return 0.0;
        }
        return maybeDouble;
    }

    public void putPersonKilometers(final BufferedReader personKilometers) {
        final String filename = WD + "/person-kilometers.txt";
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(final PrintWriter pw) throws IOException {
                FileIO.readFromInput(personKilometers, new Reading() {
                    @Override
                    public void read(BufferedReader br) throws IOException {
                        String line = br.readLine();
                        while (br != null) {
                            pw.println(line);
                        }
                    }
                });
            }
        });
    }

    public StreamingOutput getPersonKilometers() {
        final String filename = WD + "/person-kilometers.txt";
        return new StreamingOutput() {
            @Override
            public void write(final PrintWriter pw) throws IOException {
                FileIO.readFromFile(filename, new Reading() {
                    @Override
                    public void read(BufferedReader br) throws IOException {
                        String line = br.readLine();
                        while (line != null) {
                            pw.println(line);
                            line = br.readLine();
                        }
                    }
                });
            }
        };
    }

}
