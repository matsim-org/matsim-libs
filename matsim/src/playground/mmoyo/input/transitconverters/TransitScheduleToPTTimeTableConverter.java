package playground.mmoyo.input.transitconverters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Network;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.mmoyo.PTRouter.PTTimeTable2;
import playground.mmoyo.PTRouter.PTLine;

/**
 * Reads a transitschedule to feed a PTTimetable
 */
public class TransitScheduleToPTTimeTableConverter {
	private static Time time;
	public TransitScheduleToPTTimeTableConverter() {

	}

	public PTTimeTable2 getPTTimeTable(final String transitScheduleFile, final Network net) {
		TransitSchedule transitSchedule = new TransitSchedule();
		try {
			new TransitScheduleReaderV1(transitSchedule, net).readFile(transitScheduleFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return getPTTimeTable(transitSchedule);
	}
	
	
	public PTTimeTable2 getPTTimeTable(final TransitSchedule transitSchedule) {
		PTTimeTable2 ptTimeTable = new PTTimeTable2();
		List<PTLine> ptLineList = new ArrayList<PTLine>();
		
		/**Convert every transitRoute into PTLine    */
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				Id id = transitRoute.getId();
				System.out.println("TransitRoute: " + id);
				TransportMode transportMode =  transitRoute.getTransportMode();
				String direction = id.toString();
				List<String> strDepartureList=  getStrDepartureList (transitRoute.getDepartures().values());
				
				List<Double> minutesList = new ArrayList<Double>();
				List<Id> idNodeRouteList = new ArrayList<Id>();
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) {
					minutesList.add(transitRouteStop.getArrivalDelay());
					idNodeRouteList.add(transitRouteStop.getStopFacility().getId());
				}
				
				//transitRoute.getDescription();
				PTLine ptLine = new PTLine(id, transportMode, direction, idNodeRouteList, minutesList, strDepartureList); 
				ptLineList.add(ptLine);
			}
		}
		ptTimeTable.setptLineList(ptLineList);
//      ptTimeTable.calculateTravelTimes(net);
//		new PTNetworkFactory2().readTimeTable(net, ptTimeTable); // TODO [Manuel] this must work without explicit ptNetwork. If really needed, construct it from transitSchedule
		
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
