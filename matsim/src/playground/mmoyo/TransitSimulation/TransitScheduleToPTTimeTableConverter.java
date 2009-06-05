package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.input.PTNetworkFactory2;
import playground.mmoyo.PTRouter.PTLine;

/**
 * Reads a transitschedule to feed a PTTimetable
 */
public class TransitScheduleToPTTimeTableConverter {
	//->Ideally the router should use transitroute to get directly these data
	private static Time time;
	
	public TransitScheduleToPTTimeTableConverter() {

	}

	public PTTimeTable2 getPTTimeTable(final String transitScheduleFile, final NetworkLayer net){
		PTTimeTable2 ptTimeTable = new PTTimeTable2();
		TransitSchedule transitSchedule = new TransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, net);
		List<PTLine> ptLineList = new ArrayList<PTLine>();
		
		/** Read TransitSchedule */
		try{
			transitScheduleReaderV1.readFile(transitScheduleFile);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		
		/**Convert every transitRoute into PTLine    */
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				Id id = transitRoute.getId();
				System.out.println("TransitRoute: " + id);
				TransportMode transportMode =  transitRoute.getTransportMode();
				String Direction = id.toString();
				List<String> strDepartureList=  getStrDepartureList (transitRoute.getDepartures().values());
				
				List<Double> minutesList = new ArrayList<Double>();
				List<Id> idNodeRouteList = new ArrayList<Id>();
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) {
					minutesList.add(transitRouteStop.getArrivalDelay());
					idNodeRouteList.add(transitRouteStop.getStopFacility().getId());
				}
				
				//transitRoute.getDescription();
				PTLine ptLine = new PTLine(id, transportMode, Direction, idNodeRouteList, minutesList, strDepartureList); 
				ptLineList.add(ptLine);
			}
		}
		ptTimeTable.setptLineList(ptLineList);
		new PTNetworkFactory2().readTimeTable(net, ptTimeTable);
		
		return ptTimeTable;
	}
	
	/**
	 * Converts double departure.getDepartureTime into String TimeTable.departure 
	 * @param departuresCollection Departure objects whose time will be converted in text as PTTimeTable uses it
	 * @return
	 */
	private List<String> getStrDepartureList (Collection<Departure> departuresCollection){
		List<String> strDepartureList = new ArrayList<String>(); 
		for (Departure departure : departuresCollection){
			String strDeparture= time.writeTime(departure.getDepartureTime(), time.TIMEFORMAT_HHMM);
			strDepartureList.add(strDeparture);
		}
		return strDepartureList;
	}
	
	
	

		
}
