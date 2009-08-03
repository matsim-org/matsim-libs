package playground.mohit.converter;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.transitSchedule.TransitScheduleBuilderImpl;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.BasicVehiclesImpl;
public class MyMain {

	private static final Logger log = Logger.getLogger(MyMain.class);

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final VisumNetwork vNetwork = new VisumNetwork();

		log.info("reading visum network");
		try {
			new playground.mohit.converter.VisumNetworkReader(vNetwork).read("/Volumes/Data/VSP/coding/eclipse35/thesis-data/networks/yalcin/ptzh_orig.net"); // yalcin
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		System.out.println("Stops......." );
//		for (Stop stop : vNetwork.stops.values()) {
//			System.out.println("> " + stop.name +"   "+ stop.id +"  "+ stop.coord);
//		}
//		System.out.println("Stop Points......." );
//		for (StopPoint sp1 : vNetwork.stopPoints.values()) {
//			System.out.println("> " + sp1.id + "  " + sp1.stopAreaId + "   " + sp1.name+ "   " + sp1.refLinkNo);
//		}
//		System.out.println("Lines......." );
//		for (TransitLine l1 : vNetwork.lines.values()) {
//			System.out.println("> " + l1.id + "  " + l1.tCode);
//		}
//		System.out.println("Lineroutes.......corr lines.." );
//		for (TransitLineRoute lr1 : vNetwork.lineRoutes.values()) {
//			System.out.println("> " + lr1.id +"          "+ lr1.lineName);
//		}
//		System.out.println("Lines Route Items......." );
//		for (LineRouteItem lri1 : vNetwork.lineRouteItems.values()) {
//			System.out.println("> " + lri1.lineRouteName + "  " + lri1.lineName + "   " + lri1.index+ "   " + lri1.stopPointNo);
//		}
//		System.out.println("Time profiles......." );
//		for (TimeProfile tp1 : vNetwork.timeProfiles.values()) {
//			System.out.println("> " + tp1.lineRouteName  + "   " + tp1.index);
//		}
//		System.out.println("Time Profile Items......." );
//		for (TimeProfileItem tpi1 : vNetwork.timeProfileItems.values()) {
//			System.out.println("> " + tpi1.lineRouteName +tpi1.timeProfileName+ "  " + tpi1.lineName+ "   " + tpi1.index + "   " + tpi1.arr+ "   " + tpi1.dep+ "   " + tpi1.lRIIndex );
//
//		}
//		System.out.println("Departures....." );
//		for (Departure d1 : vNetwork.departures.values()) {
//			System.out.println("> " + d1.lineRouteName + "  " + d1.lineName + "   " + d1.index+ "   " + d1.TRI+ "   " + d1.dep);
//
//		}
//
		log.info("converting data");
		BasicVehicles vehicles = new BasicVehiclesImpl();
		TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		new Visum2TransitSchedule(vNetwork, schedule, vehicles).convert();

		try {
			log.info("writing transit schedule.");
			new TransitScheduleWriterV1(schedule).write("/Users/cello/Desktop/Mohit/zuerichSchedule.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("done");
	}
}
