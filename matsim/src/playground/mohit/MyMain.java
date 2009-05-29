package playground.mohit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;

//import playground.marcel.pt.transitSchedule.TransitRoute;
//import playground.marcel.visum.VisumNetwork;
//import playground.marcel.visum.VisumNetworkReader;
//import playground.marcel.visum.VisumNetwork.Stop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleWriterV1;

import playground.mohit.VisumNetwork.LineRouteItem;
import playground.mohit.VisumNetwork.Stop;
import playground.mohit.VisumNetwork.StopPoint;
import playground.mohit.VisumNetwork.TransitLine;
import playground.mohit.VisumNetwork.TransitLineRoute;

import playground.mohit.VisumNetwork.TimeProfile;
import playground.mohit.VisumNetwork.TimeProfileItem;
import playground.mohit.VisumNetwork.Departure;
import playground.mohit.VisumNetwork;
import playground.mohit.VisumNetworkReader;
public class MyMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final VisumNetwork vNetwork = new VisumNetwork();
		  
		try {
			new VisumNetworkReader(vNetwork).read("C:\\Dokumente und Einstellungen\\shah\\Desktop\\netw6.net"); // yalcin
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		System.out.println("Stops......." );
		for (Stop stop : vNetwork.stops.values()) {
			System.out.println("> " + stop.name +"   "+ stop.id +"  "+ stop.coord);
		}
		System.out.println("Stop Points......." );
		for (StopPoint sp1 : vNetwork.stopPoints.values()) {
			System.out.println("> " + sp1.id + "  " + sp1.stopId + "   " + sp1.name+ "   " + sp1.refLinkNo);
		}
		System.out.println("Lines......." );
		for (TransitLine l1 : vNetwork.lines.values()) {
			System.out.println("> " + l1.id + "  " + l1.tCode);
		}
		System.out.println("Lineroutes.......corr lines.." );
		for (TransitLineRoute lr1 : vNetwork.lineRoutes.values()) {
			System.out.println("> " + lr1.id +"          "+ lr1.lineName);
		}
		System.out.println("Lines Route Items......." );
		for (LineRouteItem lri1 : vNetwork.lineRouteItems.values()) {
			System.out.println("> " + lri1.lineRouteName + "  " + lri1.lineName + "   " + lri1.index+ "   " + lri1.stopPointNo);
		}
		System.out.println("Time profiles......." );
		for (TimeProfile tp1 : vNetwork.timeProfiles.values()) {
			System.out.println("> " + tp1.lineRouteName  + "   " + tp1.index);
		}
		System.out.println("Time Profile Items......." );
		for (TimeProfileItem tpi1 : vNetwork.timeProfileItems.values()) {
			System.out.println("> " + tpi1.lineRouteName +tpi1.timeProfileName+ "  " + tpi1.lineName+ "   " + tpi1.index + "   " + tpi1.arr+ "   " + tpi1.dep+ "   " + tpi1.lRIIndex );
		
		}
		System.out.println("Departures....." );
		for (Departure d1 : vNetwork.departures.values()) {
			System.out.println("> " + d1.lineRouteName + "  " + d1.lineName + "   " + d1.index+ "   " + d1.TRI+ "   " + d1.dep);
			
		}
		
		TransitSchedule schedule = new TransitSchedule();
		new Visum2TransitSchedule(vNetwork, schedule).convert();
		
		try {
			new TransitScheduleWriterV1(schedule).write("C:\\Dokumente und Einstellungen\\shah\\Desktop\\transitSchedule2.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
