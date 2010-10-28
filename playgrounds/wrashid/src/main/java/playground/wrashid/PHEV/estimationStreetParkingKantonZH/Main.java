package playground.wrashid.PHEV.estimationStreetParkingKantonZH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;

import playground.balmermi.world.Layer;
import playground.balmermi.world.MatsimWorldReader;
import playground.balmermi.world.World;
import playground.balmermi.world.Zone;


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
 *
 *
 *
 *
 *   The simple model:
 *  ====================
 *
 *  - compute for each community the length-percentage of each street type.
 *  - do the same for different parts of city zurich to find the matching community
 *
 *
 *  - for the maching part of the city of zurich, do the following:
 *  	- compute for each type, how many types have parkings on them and how many do not
 *      - of the streets, which have parkings, compute how many parkings per meter they have (number of pakings / length of street)
 *
 *  - model these things on to the communities:
 *       - select randomly the same ratio of roads, on which to park or not in the community (per road type)
 *       - distribute on the selected streets  for parking the same number of parkings per meter as calculated above
 */


public class Main {


	public static HashMap<Integer, GG25Data> communityData=null; //key= communityId
	public static World world=null;
	public static NetworkImpl network=null;
	public static LinkedList<Coord> streetData=null;

	// key=linkId, value=NumberOfParkings
	// for the city of Zurich, this contains the number of parkings per link
	public static HashMap<LinkImpl, Integer> numberOfStreetParkingsPerLink=null;

	// key= communityId, value=List of Links in the community
	// for each community this contains the list of links, which belong to that community
	public static HashMap<Integer,LinkedList<Link>> streetsInCommunities=new HashMap<Integer,LinkedList<Link>>();
	public static int communityIdOfZurichCity=261;

	// key=communityId, Value=HashMap(LinkType,Percentage Road of this LinkType)
	// The value stored is the sum length of the road type divided by the sum length of all roads in the community
	public static HashMap<Integer,HashMap<Integer,Double>> lengthPercentageOfEachLinkType= new HashMap<Integer,HashMap<Integer,Double>>();

	// We define zones in the city of Zurich and
	public static LinkedList<CityZone> zonesOfCityZurich = new LinkedList<CityZone>();

	// the links which belong to the community of the zurich city
	public static LinkedList<Link> linksOfZurichCity=null;
	public static Zone cityZurichZone=null; // from world file the zurich city community

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		readAllDataFiles();

		removeDataWhichIsNotRelatedToKantonZH();

		init();

		//for (String linkId:numberOfStreetParkingsPerLink.keySet()){
		//	System.out.println(linkId + "-" + numberOfStreetParkingsPerLink.get(linkId));
		//}

		//TODO: Find best match CityZone for each community
		// Do the statistics written above in CityZone class, so that they can be used afterwards.
		// write out the facilities to the file



	}




	// precondition: initialize world and streetsInCommunities
	private static void init() {

		// initialize streetsInCommunities
		// this means for each community, we put the streets belonging to the community in there
		Layer layer = world.getLayer("municipality");
		for (Link link:network.getLinks().values()){
			for (Iterator iter=layer.getLocations().values().iterator();iter.hasNext();){
				Zone zone=(Zone)iter.next();
				if (zone.contains(link.getCoord())){
					LinkedList<Link> list=streetsInCommunities.get((Integer.parseInt(zone.getId().toString())));
					list.add(link);
					break;
				}
			}
		}


		// calculate the number of parkings per link for the city of Zurich
		numberOfStreetParkingsPerLink=new HashMap<LinkImpl, Integer>();
		for (int i=0;i<streetData.size();i++){
			LinkImpl link = network.getNearestLink(streetData.get(i));
			if (!numberOfStreetParkingsPerLink.containsKey(link)){
				numberOfStreetParkingsPerLink.put(link,0);
			}

			int numberOfParkings=numberOfStreetParkingsPerLink.get(link);
			numberOfParkings++;
			numberOfStreetParkingsPerLink.put(link,numberOfParkings);
		}


		//  precondition: streetsInCommunities must have been initialized
		// initialize lengthPercentageOfEachLinkType,
		Collection keys = new ArrayList(streetsInCommunities.keySet());
		for (Iterator iter=keys.iterator();iter.hasNext();){
			Integer communityId=(Integer)iter.next();
			LinkedList<Link> list=streetsInCommunities.get(communityId);
			lengthPercentageOfEachLinkType.put(communityId,calculateLengthPercentageOfEachLinkType(list));
		}



		// the links of the city of Zurich
		linksOfZurichCity = Main.streetsInCommunities.get(communityIdOfZurichCity);
		layer = world.getLayer("municipality");
		cityZurichZone=(Zone)layer.getLocation(new IdImpl(communityIdOfZurichCity));


		// create random city zones
		for (int i=0;i<100;i++){
			zonesOfCityZurich.add(new CityZone());
		}

	}


	public static HashMap<Integer,Double> calculateLengthPercentageOfEachLinkType(LinkedList<Link> list){
		HashMap<Integer,Double> hm = new HashMap<Integer,Double>();
		double sumOfAllStreetLengths=0;
		for (int i=0;i<list.size();i++){
			Link link=list.get(i);
			sumOfAllStreetLengths+=link.getLength();
			addValue(hm,Integer.parseInt(((LinkImpl) link).getType()),link.getLength());
		}
		normalizeCollection(hm,1/sumOfAllStreetLengths);
		return hm;
	}




	// add value to at key in hashmap
	private static void addValue(HashMap hm,Object key, double value){
		if (!hm.containsKey(key)){
			hm.put(key, 0.0);
		}
		double old = (Double) hm.get(key);
		hm.put(key, old+value);
	}

	// all the values in the HashMap are multiplied with the scalingFactor
	private static void normalizeCollection(HashMap hm, double scalingFactor){
		Collection keys=new ArrayList(hm.keySet());
		for (Iterator iter=keys.iterator();iter.hasNext();){
			Object key=iter.next();
			double oldValue=(Double)hm.get(key);
			hm.put(key, oldValue * scalingFactor);
		}
	}




	private static void removeDataWhichIsNotRelatedToKantonZH() {
		// remove all communties, which are not in the Kanton ZH
		Collection keys = new ArrayList(communityData.keySet());
		for (Iterator iter=keys.iterator();iter.hasNext();){
			Object key= iter.next();
			GG25Data commData=communityData.get(key);
			if (commData.kantonId!=1){
				communityData.remove(commData.communityId);
			}

			// performance insert: initialize streetsInCommunities
			streetsInCommunities.put((Integer)key, new LinkedList<Link>()) ;
		}

		// remove all zones, which do not belong to Kanton ZH
		Layer layer = world.getLayer("municipality");

		keys = new ArrayList(layer.getLocations().keySet());
		for (Iterator iter=keys.iterator();iter.hasNext();){
			Object key= iter.next();
			Zone zone = (Zone)layer.getLocation((Id)key);
			if (!communityData.containsKey((Integer.parseInt(zone.getId().toString())))){
				layer.getLocations().remove(key);
			}
		}

		//Iterator iter=layer.getLocations().values().iterator();
		//while (iter.hasNext()){
		//	Zone zone = (Zone)iter.next();
			//System.out.println(zone.getId());
		//}







	}





	public static void readAllDataFiles(){
		ScenarioImpl scenario = new ScenarioImpl();

		// read data about communities
		communityData=GG25Data.readGG25Data("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\world\\gg25_2001_infos.txt");


		// read world data
		world=new World();
		MatsimWorldReader worldReader = new MatsimWorldReader(scenario, world);
		worldReader .readFile("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\world\\world.xml");
		//Layer layer = world.getLayer("municipality");

		//ZoneLayer zl = (ZoneLayer) layer;
		//Zone zone = (Zone) zl.getLocation("6712");



		//System.out.println(zone.getName());



		// read network layer
		network = scenario.getNetwork();
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(scenario);
		//nr.readFile("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\networks\\navteq\\network.xml\\network.xml");
		nr.readFile("C:\\data\\SandboxCVS\\ivt\\studies\\switzerland\\networks\\ivtch\\network.xml");




		// read street parking data of zurich
		streetData = StreetParkingReader.readData("C:\\data\\Projekte\\ETH TH-22 07-3 PHEV\\Parkh�user\\facilities\\input\\streetParking2007_1.txt");







	}


	private static class CityZone{
		public double xMin,xMax,yMin,yMax=0;


		public LinkedList<Link> allContainingLinks=new LinkedList<Link>();
		public HashMap<Link,Integer> linksWithParking=new HashMap<Link,Integer>();
		public HashMap<Integer,Double> lengthPercentageOfEachLinkType = null;


		public CityZone(){
			Random r=new Random();
			// create a city zone randomly in the zurich city area
			xMin=cityZurichZone.getMin().getX() + r.nextDouble() * (cityZurichZone.getMax().getX()-cityZurichZone.getMin().getX());
			xMax=xMin + r.nextDouble() * (cityZurichZone.getMax().getX()-cityZurichZone.getMin().getX());

			yMin=cityZurichZone.getMin().getY() + r.nextDouble() * (cityZurichZone.getMax().getY()-cityZurichZone.getMin().getY());
			yMax=yMin + r.nextDouble() * (cityZurichZone.getMax().getY()-cityZurichZone.getMin().getY());


			// Find out, which links are in the selected area
			for (int i=0;i<linksOfZurichCity.size();i++){
				if (xMin<linksOfZurichCity.get(i).getCoord().getX() && xMax>linksOfZurichCity.get(i).getCoord().getX() && yMin<linksOfZurichCity.get(i).getCoord().getY() && yMax<linksOfZurichCity.get(i).getCoord().getY()){
					allContainingLinks.add(linksOfZurichCity.get(i));
				}
			}

			// the links, which contain parkings
			for (LinkImpl link:numberOfStreetParkingsPerLink.keySet()){
				if (xMin<link.getCoord().getX() && xMax>link.getCoord().getX() && yMin<link.getCoord().getY() && yMax<link.getCoord().getY()){
					linksWithParking.put(link, numberOfStreetParkingsPerLink.get(link));
				}
			}


			lengthPercentageOfEachLinkType=calculateLengthPercentageOfEachLinkType(allContainingLinks);

		}
	}


}
