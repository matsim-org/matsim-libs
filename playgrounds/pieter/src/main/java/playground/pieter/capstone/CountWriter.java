package playground.pieter.capstone;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CountWriter {

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, NoConnectionException {
        String properties = "connections/capstone.properties";
        DataBaseAdmin dba = new DataBaseAdmin(new File(properties));
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new NetworkReaderMatsimV2(scenario.getNetwork()).readFile("/Users/fouriep/Documents/capstone/50_calibration/final/network/SingaporeNetworkwFreightOtherPassengerTypes.xml");
//		get all the modes
        String[] modes = new String[]{"car", "hgv", "lgv", "motorcycle", "other", "privateBus", "publicBus", "taxiFull", "taxiEmpty", "vhgv"};


        for (String mode : modes) {
            Counts counts = new Counts();
            ResultSet resultSet = dba.executeQuery("select * from s_00_baseline.countstation_id_to_link_id " +
                    "order by \"Dir_SiteID\"; ");

            while (resultSet.next()) {
                String siteId = resultSet.getString("Dir_SiteID");
                String dir = resultSet.getString("Direction");
                String linkIdString = resultSet.getString("link_id");
                Id<Link> linkId = Id.createLinkId(linkIdString);
                Count count = counts.createAndAddCount(linkId, siteId + ": " + dir);
                count.setCoord(scenario.getNetwork().getLinks().get(linkId).getCoord());
                ResultSet resultSet1 = dba.executeQuery(String.format("select * from s_00_baseline.countstation_id_to_link_id " +
                        "natural join s_00_baseline.counts_by_hour_mode_2013 where \"Dir_SiteID\" = '%s' and mode = '%s' order by hour;", siteId, mode));
                while (resultSet1.next()) {
                    count.createVolume(resultSet1.getInt("hour"), resultSet1.getInt("count"));
                }
            }
            CountsWriter writerCounts = new CountsWriter(counts);
            writerCounts.write(String.format("/Users/fouriep/Documents/capstone/50_calibration/counts/%s.xml", mode));
        }


    }
}

