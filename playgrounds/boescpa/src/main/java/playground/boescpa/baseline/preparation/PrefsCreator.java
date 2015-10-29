package playground.boescpa.baseline.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.util.Random;

/**
 * Creates preferences for a given population.
 *
 * This class is based on the class playground.stahela.matsim2030.CreatePrefs.java
 *
 * @author boescpa
 */
public class PrefsCreator {

    private final static Logger log = Logger.getLogger(PrefsCreator.class);
    private final static Random random = new Random(37835409);

    public static void main(final String[] args) {
        final String pathToInputPopulation = args[0];
        final String pathToOutputPrefs = args[1];

        Population population = getPopulation(pathToInputPopulation);
        ObjectAttributes prefs = createPrefsBasedOnPlans(population);

        ObjectAttributesXmlWriter attributesXmlWriterWriter = new ObjectAttributesXmlWriter(prefs);
        attributesXmlWriterWriter.writeFile(pathToOutputPrefs);
    }

    protected static ObjectAttributes createPrefsBasedOnPlans(final Population population) {
        ObjectAttributes prefs = new ObjectAttributes();
        Counter counter = new Counter(" person # ");
        ActivityAnalyzer activityAnalyzer = new ActivityAnalyzer();

        for (Person p : population.getPersons().values()) {
            counter.incCounter();

            // todo-boescpa: create prefs...

        }

        counter.printCounter();
        activityAnalyzer.printActChainAnalysis();
        return prefs;
    }

    protected static ObjectAttributes createRandomPrefs(final Population population) {
        //////////////////////////////////////////////////////////////////////
        // create prefs

        ObjectAttributes prefs = new ObjectAttributes();
        Counter counter = new Counter(" person # ");
        ActivityAnalyzer activityAnalyzer = new ActivityAnalyzer();
        int nrOfActs, nrWorkActs;
        double timeBudget;
        String actChain;
        boolean education, shopping, leisure;

        for (Person p : population.getPersons().values()) {
            counter.incCounter();

            if (p.getSelectedPlan() != null) {
                // reset person
                nrOfActs = 0;
                nrWorkActs = 0;
                timeBudget = 24*3600.0;
                actChain = "";
                education = false;
                shopping = false;
                leisure = false;
                // get number of activities and actChain
                for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof ActivityImpl) {
                        nrOfActs += 1;
                        ActivityImpl act = (ActivityImpl) pe;
                        actChain = actChain.concat(act.getType().substring(0, 1));
                        switch (act.getType()) {
                            case "work":
                                nrWorkActs += 1;
                                break;
                            case "education":
                                education = true;
                                break;
                            case "shopping":
                                shopping = true;
                                break;
                            case "leisure":
                                leisure = true;
                                break;
                        }
                    }
                    activityAnalyzer.addActChain(actChain);
                }
                // assign durations
                timeBudget = getRandomWorkDurations(prefs, nrOfActs, nrWorkActs, timeBudget, p);
                timeBudget = getRandomEducationDurations(prefs, nrOfActs, timeBudget, education, shopping, leisure, p);
                timeBudget = getRandomSecondaryActivityDurations(prefs, timeBudget, shopping, leisure, p);
                setTimeBudgetAsHomeDuration(prefs, timeBudget, p);
            } else {
                log.warn("person " +p.getId().toString()+ " has no plan defined");
            }
        }

        counter.printCounter();
        activityAnalyzer.printActChainAnalysis();
        return prefs;
    }

    private static void setTimeBudgetAsHomeDuration(ObjectAttributes prefs, double timeBudget, Person p) {
        // assign remaining timeBudget to home activities
        double typicalHomeDuration = timeBudget;

        prefs.putAttribute(p.getId().toString(), "typicalDuration_home", typicalHomeDuration);
        prefs.putAttribute(p.getId().toString(), "minimalDuration_home", 0.5 * 3600.0);
        prefs.putAttribute(p.getId().toString(), "earliestEndTime_home", 0.0 * 3600.0);
        prefs.putAttribute(p.getId().toString(), "latestStartTime_home", 24.0 * 3600.0);

        //log.info("person p " +p.getId().toString()+ " has home duration: " +typicalHomeDuration/3600);
    }

    private static double getRandomSecondaryActivityDurations(ObjectAttributes prefs, double timeBudget, boolean shopping, boolean leisure, Person p) {
        // draw a duration for secondary activities
        if (shopping || leisure) {
            // agent should be home at least for 5-7 hours
            double maxSecActDur = timeBudget-(5*3600.0+random.nextInt(2));
            double typicalShoppingDuration;
            double typicalLeisureDuration;

            // if both act types are reported:
            // both between 0.5 and available time budget
            if (shopping && leisure) {
                // agent should be home at least for 5-7 hours
                typicalShoppingDuration = 1800.0 + random.nextInt((int) (maxSecActDur-3600.0));
                typicalLeisureDuration = maxSecActDur - typicalShoppingDuration;

                prefs.putAttribute(p.getId().toString(), "typicalDuration_shopping", typicalShoppingDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_shopping", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_shopping", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_shopping", 24.0 * 3600.0);

                prefs.putAttribute(p.getId().toString(), "typicalDuration_leisure", typicalLeisureDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_leisure", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_leisure", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_leisure", 24.0 * 3600.0);

                timeBudget -= typicalLeisureDuration;
                timeBudget -= typicalShoppingDuration;
                //log.info("person p " +p.getId().toString()+ " has leisure duration: " +typicalLeisureDuration/3600);
                //log.info("person p " +p.getId().toString()+ " has shopping duration: " +typicalShoppingDuration/3600);
            }
            else if (shopping && !leisure) {
                typicalShoppingDuration = 1800 + random.nextInt((int) ((maxSecActDur-1800.0)));

                prefs.putAttribute(p.getId().toString(), "typicalDuration_shopping", typicalShoppingDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_shopping", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_shopping", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_shopping", 24.0 * 3600.0);

                timeBudget -= typicalShoppingDuration;
                //log.info("person p " +p.getId().toString()+ " has shopping duration: " +typicalShoppingDuration/3600);
            }
            else if (!shopping && leisure) {
                typicalLeisureDuration = 1800 + random.nextInt((int) ((maxSecActDur-1800.0)));

                prefs.putAttribute(p.getId().toString(), "typicalDuration_leisure", typicalLeisureDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_leisure", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_leisure", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_leisure", 24.0 * 3600.0);

                timeBudget -= typicalLeisureDuration;
                //log.info("person p " +p.getId().toString()+ " has leisure duration: " +typicalLeisureDuration/3600);
            }
        }
        return timeBudget;
    }

    private static double getRandomEducationDurations(ObjectAttributes prefs, int nrOfActs, double timeBudget, boolean education, boolean shopping, boolean leisure, Person p) {
        // draw a duration for education activities
        if (education) {
            double typicalEducationDuration;
            // less than 4 acts or only one other secondary activity type:
            // 7-9h
            if (nrOfActs < 4 || (leisure && !shopping) || (!leisure && shopping)) {
                typicalEducationDuration = 25200 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
            }
            // else:
            // 4-6
            else {
                typicalEducationDuration = 14400 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
            }

            prefs.putAttribute(p.getId().toString(), "typicalDuration_education", typicalEducationDuration);
            prefs.putAttribute(p.getId().toString(), "minimalDuration_education", 0.5 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "earliestEndTime_education", 0.0 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "latestStartTime_education", 24.0 * 3600.0);

            timeBudget -= typicalEducationDuration;
            //log.info("person p " +p.getId().toString()+ " has education duration: " +typicalEducationDuration/3600);
        }
        return timeBudget;
    }

    private static double getRandomWorkDurations(ObjectAttributes prefs, int nrOfActs, int nrWorkActs, double timeBudget, Person p) {
        // draw a duration for work activities
        if (nrWorkActs > 0) {
            double typicalWorkDuration = 0;
            // if number of nonWorkActs < 4:
            // 4-7h with 10% prob, 7-9h with 80% prob and 9-12 hours with 10% prob
            if (nrOfActs-nrWorkActs < 4) {
                double prob = random.nextDouble();
                if (prob <= 0.1) {
                    typicalWorkDuration = 14400 + random.nextInt(2)*3600.0 + random.nextDouble()*3600.0;
                }
                else if (prob > 0.1 && prob < 0.9) {
                    typicalWorkDuration = 25200 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
                }
                else if (prob >= 0.9) {
                    typicalWorkDuration = 32400 + random.nextInt(2)*3600.0 + random.nextDouble()*3600.0;
                }
            }
            // if number of nonWorkActs >= 4:
            // 3-8h
            else {
                typicalWorkDuration = 10800 + random.nextInt(4) + random.nextDouble()*3600.0;
            }

            prefs.putAttribute(p.getId().toString(), "typicalDuration_work", typicalWorkDuration);
            prefs.putAttribute(p.getId().toString(), "minimalDuration_work", 0.5 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "earliestEndTime_work", 0.0 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "latestStartTime_work", 24.0 * 3600.0);

            timeBudget -= typicalWorkDuration;
            //log.info("person p " +p.getId().toString()+ " has working duration: " +typicalWorkDuration/3600);
        }
        return timeBudget;
    }

    private static Population getPopulation(final String pathToPopFile) {
        //////////////////////////////////////////////////////////////////////
        // read in population

        log.info("Reading plans...");
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimPopulationReader PlansReader = new MatsimPopulationReader(scenario);
        PlansReader.readFile(pathToPopFile);
        Population population = scenario.getPopulation();
        log.info("Reading plans...done.");
        log.info("Population size is " +population.getPersons().size());
        return population;
    }

}
