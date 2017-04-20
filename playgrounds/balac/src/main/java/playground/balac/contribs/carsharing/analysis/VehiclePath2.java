package playground.balac.contribs.carsharing.analysis;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class VehiclePath2 {
	public static void main(String[] args) throws IOException {

		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/500.CS.txt");

		Config config = ConfigUtils.createConfig();
        config.network().setInputFile(args[0]);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        readLink.read();
        String s = readLink.readLine();
        
        while (s != null) {
        	String[] arr = s.split(",");
        	
        	if (arr[12].equals("FF_992")) {
        		
        		Link startLink = scenario.getNetwork().getLinks().get(Id.createLinkId(arr[4]));
        		Link endLink = scenario.getNetwork().getLinks().get(Id.createLinkId(arr[5]));
        		System.out.println(arr[0] + " " + arr[2] + " " + arr[3] + " " + Double.parseDouble(arr[8])/1000.0 + " " +  startLink.getCoord() + " " +  endLink.getCoord());        		
        		
        	}
        	
        	s= readLink.readLine();
        	
        }
	}

}
