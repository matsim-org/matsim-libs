package playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity;

import java.util.LinkedList;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class VisualizeParkings {

	public static void main(String[] args) {
		LinkedList<Parking> parkingCollection = ParkingHerbieControler.getParkingCollectionZHCity();
		
		String outputKmlFile="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/kmls/parkings-cityzh.kml";
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		for (Parking parking:parkingCollection){
			basicPointVisualizer.addPointCoordinate(parking.getCoord(), parking.getType() ,Color.GREEN);
		}
		
		System.out.println("writing kml file...");
		basicPointVisualizer.write(outputKmlFile);
		
	}
	
}
