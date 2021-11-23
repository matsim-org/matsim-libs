package org.matsim.urbanEV;


import com.opencsv.exceptions.CsvException;
import org.apache.log4j.Logger;
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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.run.RunBerlinScenario;
import java.io.IOException;
import java.util.*;


import static java.util.stream.Collectors.toList;

/*
This class was modified during the course of my work. So it was aligned to a certain purpose without keeping its original unction.
So maybe don't mind this class.
@Jonas116
 */

public class ChargerAssignment {


    private static final Logger log = Logger.getLogger(ChargerAssignment.class);


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
        MPMCsvToXML csvreader = new MPMCsvToXML(fileName, fileName2);
        chargers.addAll(csvreader.chargers);

        Map<Id<Person>, Id<Link>> homeChargerLinks = new HashMap<>();
        List<Person> personsWithEvs = new ArrayList<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {

            if (!person.getId().toString().contains("freight") && !PopulationUtils.getPersonAttribute(person, "home-activity-zone").equals("brandenburg")) {
                String mode = TransportMode.car;
                List<Leg> evLegs = TripStructureUtils.getLegs(person.getSelectedPlan()).stream().filter(leg -> leg.getMode().equals(mode)).collect(toList());

                if (evLegs.size() > 0) {
                    personsWithEvs.add(person);
                }

                if (evLegs.size() > 0 && evLegs.get(0).getRoute().getStartLinkId().equals(evLegs.get(evLegs.size() - 1).getRoute().getEndLinkId())) {

                    homeChargerLinks.put(person.getId(), evLegs.get(0).getRoute().getStartLinkId());
                }
            }


        }

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

        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());

        Network subNetwork = NetworkUtils.createNetwork();

        Set<String> modes = Set.of(TransportMode.car);
        filter.filter(subNetwork, modes);



        new ChargerWriter(chargers.stream()).write("C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/New_+_homeCharger.xml");


    }
}
