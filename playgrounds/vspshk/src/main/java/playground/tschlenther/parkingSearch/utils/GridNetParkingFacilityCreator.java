package playground.tschlenther.parkingSearch.utils;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.*;
import org.matsim.facilities.*;

public class GridNetParkingFacilityCreator {

	private final static String pathToZoneOne = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Links.txt";
	private final static String pathToZoneTwo = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Rechts.txt";
	private static final String output = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Facilites_links_rechts_V2.xml";
	private static final String pathToNetFile = "C:/Users/Work/Bachelor Arbeit/input/GridNet/grid_network_length200.xml";
	
	public static void main(String[] args){
		List<Id<Link>> zoneOneLinks = getLinkIDsOfZone(pathToZoneOne);
		List<Id<Link>> zoneTwoLinks = getLinkIDsOfZone(pathToZoneTwo);
		
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(network);
		netReader.readFile(pathToNetFile);
		
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityOption option = factory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
		option.setCapacity(1.0);
		
		for(Id<Link> link : zoneOneLinks){
			Coord coord = network.getLinks().get(link).getCoord();
			ActivityFacility facility = factory.createActivityFacility(Id.create("parking_" + link, ActivityFacility.class), link);
			facility.addActivityOption(option);
			facility.setCoord(coord);
			facilities.addActivityFacility(facility);
		}
		
		for(Id<Link> link : zoneTwoLinks){
			Coord coord = network.getLinks().get(link).getCoord();
			ActivityFacility facility = factory.createActivityFacility(Id.create("parking_" + link, ActivityFacility.class), link);
			facility.addActivityOption(option);
			facility.setCoord(coord);
			facilities.addActivityFacility(facility);
		}
		
		new FacilitiesWriter(facilities).write(output);
	}

	private static List<Id<Link>> getLinkIDsOfZone (String pathToZoneFile){
		
		List<Id<Link>> links = new ArrayList<Id<Link>>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName(pathToZoneFile);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				links.add(linkId);
			}
		
        });
		return links;
	}

}

