package playground.toronto.gtfsutils;

import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import playground.toronto.demand.util.TableReader;

public class CreateNetworkFromGTFS{
	
	public static void main(String args[]) throws NumberFormatException, IOException{
		String ScheduleFile = args[0];
		String NetworkFile = args [1];
		String StopsFile = args[2];
		String NetworkPrefix = "17";
		String NetworkOutput = args[3];
		
        HashMap<Id,Coord> StopAndCoordinates = new HashMap<Id,Coord>();
        HashMap<Id,Link> RemoveLinks = new HashMap<Id,Link>();
		
		/*read the stops file
		 * 
		 */
		TableReader rdStops = new TableReader(StopsFile);
		rdStops.open();
		rdStops.ignoreTrailingBlanks(true);
		
		while (rdStops.next()){
			IdImpl StopID = new IdImpl(rdStops.current().get("stop_id").toString());
			CoordImpl StopCoord = new CoordImpl(Double.parseDouble(rdStops.current().get("X").toString()), Double.parseDouble(rdStops.current().get("Y").toString()));
			StopAndCoordinates.put(StopID, StopCoord);	
		}//end of while loop
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//TransitScheduleReaderV1 ReadSchedule = new TransitScheduleReaderV1(scenario);
		
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(NetworkFile);
		
		TransitScheduleFactoryImpl builder = new TransitScheduleFactoryImpl();
		TransitScheduleImpl Schedule = (TransitScheduleImpl) builder.createTransitSchedule();
		
	
		//what is moderoute factory thing?
		TransitScheduleReaderV1 tsreader = new TransitScheduleReaderV1(Schedule, new ModeRouteFactory(), scenario);
		tsreader.readFile(ScheduleFile);
		
		CreateNetworkFromTransitSchedule PseudoNetwork = new CreateNetworkFromTransitSchedule(Schedule, network, NetworkPrefix);
		PseudoNetwork.createNetwork(StopAndCoordinates);
		
		//remove express links
		for (Link link : network.getLinks().values()){
			if (link.getLength()>2000){
			   RemoveLinks.put(link.getId(),link);
			}
		}
		for (Link link : RemoveLinks.values()){
			network.removeLink(link.getId());
		}
		
		new NetworkWriter(network).write(NetworkOutput);
		
	}

}