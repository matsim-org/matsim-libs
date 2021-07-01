package org.matsim.urbanEV;

import com.google.common.collect.Iterables;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.compress.utils.Lists;
import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.withinday.utils.EditPlans;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class ChargerPlacer {


    private static final Logger log = Logger.getLogger(ChargerPlacer.class);


    public static void main(String[] args) throws IOException, CsvException {


        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[]{"scenarios/berlin-v5.5-1pct/input/ev/berlin-v5.5-1pct.config-ev-test2.xml"};
        }

        Config config = RunBerlinScenario.prepareConfig(args);
        Scenario scenario = RunBerlinScenario.prepareScenario(config);


        List<ChargerSpecification> chargers = new ArrayList<>();





        List<Map<Id<Person>, Id<Link>>> homeChargerLinks = new ArrayList<>();
        List<Person> personsWithEvs = new ArrayList<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {

            if (person.getId().toString().contains("freight") || PopulationUtils.getPersonAttribute(person, "home-activity-zone").equals("brandenburg")) {
                String mode = "car";
                List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
//                List<Activity> activities = TripStructureUtils.getActivities(planElements, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                List<Leg> evLegs = TripStructureUtils.getLegs(person.getSelectedPlan()).stream().filter(leg -> leg.getMode().equals(mode)).collect(toList());

                if (evLegs.size() > 0) {
                    personsWithEvs.add(person);
                }

                if (evLegs.size() > 0 && evLegs.get(0).getRoute().getStartLinkId().equals(evLegs.get(evLegs.size() - 1).getRoute().getEndLinkId())) {
                    Map<Id<Person>, Id<Link>> possibleHomeLink = new HashMap<>();
                    possibleHomeLink.put(person.getId(), evLegs.get(0).getRoute().getStartLinkId());
                    homeChargerLinks.add(possibleHomeLink);
                }
            }


        }


        for (int i = 0; i < personsWithEvs.size() * 0.6; ) {
            Person person = personsWithEvs.get(i);

            if (homeChargerLinks.stream()
                    .map(idMap -> idMap.keySet())
                    .anyMatch(ids -> ids.contains(person.getId()))) {

                Id<Link> homeLink = homeChargerLinks.stream()
                        .filter(idIdMap -> idIdMap.containsKey(person.getId()))
                        .findAny()
                        .get()
                        .get(person.getId());


                ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
                chargers.add(builder
                        .linkId(homeLink)
                        .id(Id.create("HomeCharger" + homeLink.toString(), Charger.class))
                        .chargerType(person.getId().toString())
                        .plugCount(1)
                        .plugPower(11000)
                        .build());
                i++;
            }

//            URL url = new URL("https://download.geofabrik.de/europe/germany/berlin-latest-free.shp.zip")
//            SimpleFeatureSource shp = ShapeFileReader.readDataFile("C:\\Users\\admin\\IdeaProjects\\matsim-berlin\\scenarios\\berlin-v5.5-1pct\\input\\ev\\SHPfile\\lor_bzr.shp");
//             List<Geometry> berlinSHP = ShpGeometryUtils.loadGeometries(url);


        }

        String file = "C:\\Users\\admin\\IdeaProjects\\matsim-berlin\\scenarios\\berlin-v5.5-1pct\\input\\ev\\AktuelleChargerInBerlin\\chargersBerlin.csv";
        CSVToXML2 csvreader =new CSVToXML2(file, scenario.getNetwork());
        chargers.addAll(csvreader.chargers);
        new ChargerWriter(chargers.stream()).write("C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/HomeChargersBerlin.xml");


    }
}
