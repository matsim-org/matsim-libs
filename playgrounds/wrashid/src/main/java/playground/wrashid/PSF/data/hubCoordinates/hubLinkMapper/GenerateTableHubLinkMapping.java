package playground.wrashid.PSF.data.hubCoordinates.hubLinkMapper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.GenericResult;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class GenerateTableHubLinkMapping {

	public final static Integer unMappedLinkHubNumber=0;
	
	public static void main(String[] args) {
		StringMatrix matrix = GeneralLib
				.readStringMatrix("C:/Users/Admin/Desktop/psl-temp/GIS_coordinates_of_managers.txt");
		// key: hub number, value: linkIds
		LinkedListValueHashMap<Integer, Id> hubLinkMapping = new LinkedListValueHashMap<Integer, Id>();
		BasicPointVisualizer basicPointVisualizer = new BasicPointVisualizer();
		

		Object[] genericResult = getCornerCoordinates(matrix).getResult();

		Coord bottomLeft = (Coord) genericResult[0];
		Coord topRight = (Coord) genericResult[1];

		NetworkImpl network = GeneralLib
				.readNetwork("C:/Users/Admin/Desktop/psl-temp/network.xml.gz");

		System.out.println("network loaded...");
		
		for (Link link : network.getLinks().values()) {
			if (isInBox(bottomLeft, topRight, link)) {
				Integer hubNumber=getHubNumberForLink(link,matrix);
				
				if (hubNumber!=null){
					hubLinkMapping.putAndSetBackPointer(hubNumber, link.getId());
				} else {
					//hubLinkMapping.putAndSetBackPointer(unMappedLinkHubNumber, link.getId());
					//basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(), Color.BLUE);
					//System.out.println(link.getId().toString());
				}
			} else {
				//hubLinkMapping.putAndSetBackPointer(unMappedLinkHubNumber, link.getId());
			}
		}
		
		writeResultToConsole(hubLinkMapping);
		
		
		//basicPointVisualizer.write("C:/Users/Admin/Desktop/psl-temp/unmappedLinks.kml");

	}

	private static void writeResultToConsole(LinkedListValueHashMap<Integer, Id> hubLinkMapping) {
		System.out.println("hubNumber" + "\t" +"linkId");
		for (Integer hubNumber:hubLinkMapping.getKeySet()){
			LinkedList<Id> linkIds = hubLinkMapping.get(hubNumber);
			
			linkIds=eliminateDuplicates(linkIds);
			
			for (int i=0;i<linkIds.size();i++){
				System.out.println(hubNumber + "\t" +linkIds.get(i).toString());
			}
		}		
	}

	public static LinkedList<Id> eliminateDuplicates(LinkedList<Id> linkIds) {
		LinkedList<Id> resultIds=new LinkedList<Id>();
		HashMap<String,Integer> hm=new HashMap<String, Integer>();
		
		for (Id linkId:linkIds){
			hm.put(linkId.toString(), null);
		}
		
		for (String linkIdString:hm.keySet()){
			resultIds.add(new IdImpl(linkIdString));
		}
		
		return resultIds;
	}


	private static Integer getHubNumberForLink(Link link, StringMatrix matrix) {
		Random rand=new Random();
		// as the values used for the distance were average values, the spread of the sample needs to be bigger than 1
		double sampleSpread=3;
		double maxDistance=sampleSpread*100;
		
		
		
		double closestHubDistance=Double.MAX_VALUE;
		Integer closestHubNumber=null;
		
		
		for (int i=0;i<matrix.getNumberOfRows();i++){
			Coord currentHubManagerCoord=new CoordImpl(matrix.getDouble(i, 1), matrix.getDouble(i, 2)); 
			double distance=GeneralLib.getDistance(link.getCoord(), currentHubManagerCoord);
			
			if (distance<maxDistance && distance<closestHubDistance){
				closestHubDistance=distance;
				closestHubNumber=matrix.getInteger(i, 0);
			}
		
		}
		
		return closestHubNumber;
	}

	private static boolean isInBox(Coord bottomLeft, Coord topRight, Link link) {
		if (link.getCoord().getX() > bottomLeft.getX()
				&& link.getCoord().getY() > bottomLeft.getY()) {
			if (link.getCoord().getX() < topRight.getX()
					&& link.getCoord().getY() < topRight.getY()) {
				return true;
			}
		}
		return false;
	}

	private static GenericResult getCornerCoordinates(StringMatrix matrix) {
		GenericResult genericResult;

		Coord bottomLeft = new CoordImpl(Double.MAX_VALUE, Double.MAX_VALUE);
		Coord topRight = new CoordImpl(Double.MIN_VALUE, Double.MIN_VALUE);

		for (int i = 0; i < matrix.getNumberOfRows(); i++) {
			double x = matrix.getDouble(i, 1);
			double y = matrix.getDouble(i, 2);

			if (x < bottomLeft.getX()) {
				bottomLeft.setX(x);
			}

			if (y < bottomLeft.getY()) {
				bottomLeft.setY(y);
			}

			if (x > topRight.getX()) {
				topRight.setX(x);
			}

			if (y > topRight.getY()) {
				topRight.setY(y);
			}
		}

		genericResult = new GenericResult(bottomLeft, topRight);
		return genericResult;
	}

}
