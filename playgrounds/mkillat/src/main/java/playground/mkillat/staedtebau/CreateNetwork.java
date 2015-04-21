package playground.mkillat.staedtebau;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class CreateNetwork {

   public static void main(String[] args) {
      String osm = "input/staedtebau/bergmannstr.osm";
      String configFile= "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/staedtebau/0.config.xml";
	Config config = org.matsim.core.config.ConfigUtils.loadConfig(configFile);
      Scenario sc = ScenarioUtils.createScenario(config);
      Network net = sc.getNetwork();
      CoordinateTransformation ct = 
       TransformationFactory.getCoordinateTransformation(
        TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
      OsmNetworkReader onr = new OsmNetworkReader(net,ct);
      onr.parse(osm); 
      new NetworkCleaner().run(net);
      new NetworkWriter(net).write("input/staedtebau/bermannstr.xml");
      
     
   }

}