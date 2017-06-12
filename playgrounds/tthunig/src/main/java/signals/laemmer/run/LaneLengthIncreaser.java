package signals.laemmer.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.LanesReader;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.lanes.data.LanesWriter;

import java.util.Map;

/**
 * Created by Nico on 31.05.2017.
 */
public class LaneLengthIncreaser {

    public static void main(String[] args) {

        String directory = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,TransformationFactory.WGS84), scenario.getNetwork());
        reader.readFile(directory + "/network.xml.gz");
        LanesReader lanesReader = new LanesReader(scenario);
        lanesReader.readFile(directory + "/lanes.xml");

       for(LanesToLinkAssignment assignment: scenario.getLanes().getLanesToLinkAssignments().values()) {
           Link link = scenario.getNetwork().getLinks().get(assignment.getLinkId());
           double reqDistance = 10 * link.getFreespeed();
           double resultingDistance = Math.min(reqDistance, Math.max(link.getLength()-10, 5));
           for(Lane lane: assignment.getLanes().values()) {
               if(lane.getStartsAtMeterFromLinkEnd() < resultingDistance) {
                   if(lane.getId().toString().endsWith("ol")) {
                       lane.setStartsAtMeterFromLinkEnd(resultingDistance+1);
                   } else {
                       lane.setStartsAtMeterFromLinkEnd(resultingDistance);
                   }
               }
           }
       }

        LanesWriter writer = new LanesWriter(scenario.getLanes());
        writer.write(directory +"/lanes_long.xml");
    }

}
