package org.matsim.urbanEV;

import com.google.common.collect.Iterables;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.compress.utils.Lists;
import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.withinday.utils.EditPlans;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
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
            args = new String[]{"scenarios/berlin-v5.5-1pct/input/ev/berlin-v5.5-10pct.config-ev-MPM-günstig.xml"};
        }

        Config config = RunBerlinScenario.prepareConfig(args);
        Scenario scenario = RunBerlinScenario.prepareScenario(config);


        List<ChargerSpecification> chargers = new ArrayList<>();




        String fileName = "C:\\Users\\admin\\IdeaProjects\\matsim-berlin\\src\\main\\java\\org\\matsim\\urbanEV\\ind_9619.csv";
        String fileName2 ="C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/Used_3.csv";
        CSVToXMLmpm csvreader = new CSVToXMLmpm(fileName, fileName2);
        chargers.addAll(csvreader.chargers);

        Map<Id<Person>, Id<Link>> homeChargerLinks = new HashMap<>();
        List<Person> personsWithEvs = new ArrayList<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {

            if (!person.getId().toString().contains("freight") && !PopulationUtils.getPersonAttribute(person, "home-activity-zone").equals("brandenburg")) {
                String mode = TransportMode.car;
                List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
//                List<Activity> activities = TripStructureUtils.getActivities(planElements, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                List<Leg> evLegs = TripStructureUtils.getLegs(person.getSelectedPlan()).stream().filter(leg -> leg.getMode().equals(mode)).collect(toList());

                if (evLegs.size() > 0) {
                    personsWithEvs.add(person);
                }

                if (evLegs.size() > 0 && evLegs.get(0).getRoute().getStartLinkId().equals(evLegs.get(evLegs.size() - 1).getRoute().getEndLinkId())) {

                    homeChargerLinks.put(person.getId(), evLegs.get(0).getRoute().getStartLinkId());
//
                }
            }


        }
//        try (CSVReader reader2 = new CSVReader(new FileReader(fileName2))) {
//            List<String[]> rows2 = reader2.readAll();
//            for (String[] row : Iterables.skip(rows2, 1)) {
//                usedChargers.add(row[0]);
//            }
//        }

        Collections.shuffle(personsWithEvs);
        List<Id<Link>> chargerLinks = chargers.stream().map(chargerSpecification -> chargerSpecification.getLinkId()).collect(toList());
        for (int i = 0; i < personsWithEvs.size() * 0.4; ) {
            Person person = personsWithEvs.get(i);

            if (homeChargerLinks.keySet().contains(person.getId())){

                Id<Link> homeLink = homeChargerLinks.get(person.getId());

                if(!chargerLinks.contains(homeLink)){

                ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
                chargers.add(builder
                        .linkId(homeLink)
                        .id(Id.create("HomeCharger" + person.getId().toString(), Charger.class))
                        .chargerType(person.getId().toString())
                        .plugCount(1)
                        .plugPower(11000)
                        .build());
                i++;
            }
            }

        }

//        String file = "C:\\Users\\admin\\IdeaProjects\\matsim-berlin\\scenarios\\berlin-v5.5-1pct\\input\\ev\\AktuelleChargerInBerlin\\Ladesäulen_in_Deutschland_v2.csv";

        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());

        Network subNetwork = NetworkUtils.createNetwork();

        Set<String> modes = Set.of(TransportMode.car);
        filter.filter(subNetwork, modes);
//        for (Id<Link> linkId : subNetwork.getLinks().keySet()) {
//            ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
//                chargers.add(builder
//                        .linkId(linkId)
//                        .id(Id.create("Charger" + linkId , Charger.class))
//                        .chargerType("AC")
//                        .plugCount(10)
//                        .plugPower(11000)
//                        .build());
//
//        }



//       CSVToXML2 csvreader =new CSVToXML2(file, subNetwork);
//        URL url = new URL("https://tsb-opendata.s3.eu-central-1.amazonaws.com/detailnetz_strassenabschnitte/Detailnetz-Strassenabschnitte.shp.zip");
//        berlinSHP = ShpGeometryUtils.loadGeometries(url);
//
//        List<Geometry> berlinSHP = ShpGeometryUtils.loadGeometries(url);
//
//        for (Id<Link> linkId : subNetwork.getLinks().keySet()) {
//            Coord coord = subNetwork.getLinks().get(linkId).getCoord();
//            Link link = subNetwork.getLinks().get(linkId);
//
//           if(ShpGeometryUtils.isCoordInGeometries(coord, berlinSHP)){
//               ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
//               chargers.add(builder
//                       .linkId(linkId)
//                        .id(Id.create("Charger" + linkId , Charger.class))
//                        .chargerType("AC")
//                        .plugCount(10)
//                        .plugPower(22000)
//                        .build());
//           };
//
//        }



        new ChargerWriter(chargers.stream()).write("C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/New_+_homeCharger.xml");


    }
}
