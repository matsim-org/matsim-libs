package playground.wrashid.PHEV.estimationStreetParkingKantonZH;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkReaderMatsimV1;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import playground.wrashid.PHEV.parking.data.Facility;


/*
 * - We have all the communities in the GG25Data. 
 * - From these we can simple filter out only the communities in the Kanton Zurich
 * - Using the communityId, we can use the world file, where the zone of each community is defined.
 * - Using the zone information, we can make a collection of roads per community
 * - We have street parking locations, which we can map to all the roads of Zurich
 * - 
 * 
 * 
 * - for each community, we do the following calculations: 
 * 	   1.) The percentage of each street type (length).
 *     2.) sum of the length of all streets divided by the number of streets
 * - We randomly draw boxes in the zurich city area. And calculate the same two things.
 * - Then we calculate the difference 
 * - we select the area of the city of zurich, which has the least difference to a community
 * - for the selected zurich city area, we calculate for each street type two things
 *   1.) percentage of streets 
 */


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// read data about communities
		// key= communityId
		HashMap<Integer, GG25Data> communityData=GG25Data.readGG25Data("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\world\\gg25_2001_infos.txt");
		
		
		// read world data
		/*
		World world=new World();
		MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader .readFile("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\world\\world.xml");
		Layer layer = world.getLayer("municipality");
		Location loc = layer.getLocation("6712");

		ZoneLayer zl = (ZoneLayer) layer;
		Zone zone = (Zone) zl.getLocation("6712");
		System.out.println(zone.getName());
		// TODO: continue here also
		*/
		
		
		// read network layer
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(network);
		nr.readFile("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\networks\\navteq\\network.xml\\network.xml");
		//for (Link link:network.getLinks().values()){
		//	System.out.println(link.getId() +  " - " + link.getCapacity(network.getCapacityPeriod()));
		//}
		
		
		// read street parking data of zurich
		LinkedList<Coord> streetData = StreetParkingReader.readData("C:\\data\\Projekte\\ETH TH-22 07-3 PHEV\\Parkhäuser\\facilities\\input\\streetParking2007_1.txt");
		
		
		
		// calculate the number of parkings per link for the city of Zurich
		// key=linkId, value=NumberOfParkings
		HashMap<String, Integer> numberOfStreetParkingsPerLink=new HashMap<String, Integer>();
		for (int i=0;i<streetData.size();i++){
			String linkId = network.getNearestLink(streetData.get(i)).getId().toString();
			if (!numberOfStreetParkingsPerLink.containsKey(linkId)){
				numberOfStreetParkingsPerLink.put(linkId,0);
			}
			
			int numberOfParkings=numberOfStreetParkingsPerLink.get(linkId);
			numberOfParkings++;
			numberOfStreetParkingsPerLink.put(linkId,numberOfParkings);
		}
		for (String linkId:numberOfStreetParkingsPerLink.keySet()){
			System.out.println(linkId + "-" + numberOfStreetParkingsPerLink.get(linkId));
		}
		// ??? print some statistics about the existing parkings?
		// ??? do some anylysis
		// look for some good tool for producing charts
		// TODO: continue here...
		
	}

}
