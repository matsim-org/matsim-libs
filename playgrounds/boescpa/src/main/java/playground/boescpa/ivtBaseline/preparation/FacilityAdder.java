package playground.boescpa.ivtBaseline.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import java.util.HashMap;
import java.util.Map;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.*;

/**
 * If an activity has no facility assigned, the closest facility offering this activity will be assigned.
 *
 * @author boescpa
 */
public class FacilityAdder {
	private final static Logger log = Logger.getLogger(FacilityAdder.class);

	private static final int CACHE_DISTANCE = 5000; // radius in meters within which a cached facility is reused
	private static Map<String, Map<Coord, ActivityFacility>> cache = new HashMap<>();

    public static void main(final String[] args) {
        final String pathToInputPopulation = args[0];
        final String pathToInputFacilities = args[1];
        final String pathToOutputPopulation = args[2];

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
        plansReader.readFile(pathToInputPopulation);
        Population population = scenario.getPopulation();
        FacilitiesReaderMatsimV1 facilitiesReader = new FacilitiesReaderMatsimV1(scenario);
        facilitiesReader.readFile(pathToInputFacilities);
        ActivityFacilities facilities = scenario.getActivityFacilities();

        addFacilitiesToPopulation(population, facilities);

        PopulationWriter writer = new PopulationWriter(population);
        writer.write(pathToOutputPopulation);
    }

    private static void addFacilitiesToPopulation(final Population population, final ActivityFacilities facilities) {
        Counter counter = new Counter(" person # ");
		Id<ActivityFacility> homeFacility = null, workFacility = null, eduFacility = null;
        for (Person p : population.getPersons().values()) {
            counter.incCounter();
            if (p.getSelectedPlan() != null) {
                for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof ActivityImpl) {
                        ActivityImpl act = (ActivityImpl) pe;
						if (act.getFacilityId() != null) {
							switch (act.getType()) {
								case HOME: homeFacility = act.getFacilityId(); break;
								case WORK: workFacility = act.getFacilityId(); break;
								case EDUCATION: eduFacility = act.getFacilityId(); break;
								default: log.warn("New act type with preset facility: " + act.getType());
							}
						} else {
							switch (act.getType()) {
								case HOME:
									if (homeFacility != null) {
										act.setFacilityId(homeFacility);
									} else {
										log.warn("No preset home facility found: Agent Id " + p.getId() + "\n" +
												"Will assign the closest home facility.");
										addFacilityToActivity(act, facilities);
										homeFacility = act.getFacilityId();
									}
									break;
								case WORK:
									if (workFacility != null) {
										act.setFacilityId(workFacility);
									} else {
										log.warn("No preset work facility found: Agent Id " + p.getId() + "\n" +
												"Will assign the closest work facility.");
										addFacilityToActivity(act, facilities);
										workFacility = act.getFacilityId();
									}
									break;
								case EDUCATION:
									if (eduFacility != null) {
										act.setFacilityId(eduFacility);
									} else {
										log.warn("No preset education facility found: Agent Id " + p.getId() + "\n" +
												"Will assign the closest education facility.");
										addFacilityToActivity(act, facilities);
										eduFacility = act.getFacilityId();
									}
									break;
								default:
									addFacilityToActivity(act, facilities);
							}
						}
                    }
                }
            }
        }
    }

    private static void addFacilityToActivity(ActivityImpl act, ActivityFacilities facilities) {
        Coord actCoord = act.getCoord();
        String actType = act.getType();

        ActivityFacility facility = getClosestFacility(actCoord, actType, facilities);

        act.setCoord(facility.getCoord());
        act.setFacilityId(facility.getId());
    }

    private static ActivityFacility getClosestFacility(Coord actCoord, String actType, ActivityFacilities facilities) {
		if (!cache.keySet().contains(actType)) {
			cache.put(actType, new HashMap<>());
		}

		if (!cache.get(actType).isEmpty()) {
			Map<Coord, ActivityFacility> typeCache = cache.get(actType);
			for (Coord cachedCoord : typeCache.keySet()) {
				if (CoordUtils.calcEuclideanDistance(cachedCoord, actCoord) < CACHE_DISTANCE) {
					return typeCache.get(cachedCoord);
				}
			}
		}

		ActivityFacility selectedFacility = null;
		double distanceToSelectedFacility = Double.MAX_VALUE;
        for (ActivityFacility facility : facilities.getFacilitiesForActivityType(actType).values()) {
			double distanceToCurrentFacility = CoordUtils.calcEuclideanDistance(actCoord, facility.getCoord());
			if (distanceToCurrentFacility < distanceToSelectedFacility) {
				distanceToSelectedFacility = distanceToCurrentFacility;
				selectedFacility = facility;
				if (distanceToSelectedFacility < CACHE_DISTANCE) {
					break;
				}
            }
        }
		cache.get(actType).put(selectedFacility.getCoord(), selectedFacility);
        return selectedFacility;
    }
}
