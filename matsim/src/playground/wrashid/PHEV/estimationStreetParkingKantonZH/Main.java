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
import org.matsim.world.ZoneLayer;

import playground.wrashid.PHEV.parking.data.Facility;

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
		*/
		
		
		// read network layer
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(network);
		nr.readFile("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\networks\\ivtch\\network.xml");
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
		
		// ??? print some statistics about the existing parkings?
		// ??? do some anylysis
		// TODO: continue here...
		
	}

}
