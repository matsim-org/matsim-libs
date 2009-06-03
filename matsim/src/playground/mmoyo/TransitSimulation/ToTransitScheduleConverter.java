package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleWriterV1;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTLine;

/*
 * From PTtimeTable to transitShcedule converter 
 */

public class ToTransitScheduleConverter {
	final TransitSchedule transitSchedule = new TransitSchedule();
	
	public ToTransitScheduleConverter (){
				
	}
	
	public void createTransitSchedule(final PTTimeTable2 ptTimeTable, String outTransitFile) {
		
		for (PTLine ptLine: ptTimeTable.getPtLineList()){
			Id ptLineId = ptLine.getId();
			//String ptLineType= ptLine.getPtLineType().getType();  //-> if possible add more choices to MODE
			int x=0;
			
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			for (String strIdNode : ptLine.getRoute()){
				Id idNode = new IdImpl(strIdNode);

				/*
				facilities.getFacilities().containsKey(idNode)){
					throw new NullPointerException(this + "facility [id=" + idNode + " does not exist]");
				}
				*/
				
				TransitStopFacility stop = this.transitSchedule.getFacilities().get(idNode);
				
				double min = ptLine.getMinutes().get(x++).doubleValue();  
				double arrivalDelay = min*60;
				double departureDelay = min*60;
				stops.add(new TransitRouteStop(stop, arrivalDelay, departureDelay));
			}
			
			Id idTransitRoute = new IdImpl(ptLine.getId() + ptLine.getDirection());
			TransitLine transitLine = new TransitLine(ptLineId);
			NetworkRoute networkRoute= null; //new NodeNetworkRoute(null, null);
			TransitRoute transitRoute = new  TransitRoute(idTransitRoute, networkRoute, stops, TransportMode.pt);
			
			x=0;
			for (String strDeparture : ptLine.getDepartures()){
				Id idDeparture = new IdImpl(x + "." + ptLine.getId());

				//double dblDeparture =x;			// fictional departures
				double dblDeparture=  Time.parseTime(strDeparture);

				Departure departure = new Departure (idDeparture, dblDeparture);
				transitRoute.addDeparture(departure);
				x++;
			}
			
			transitLine.addRoute(transitRoute);
			this.transitSchedule.addTransitLine(transitLine);
		}

		TransitScheduleWriterV1 transitScheduleWriterV1 = new TransitScheduleWriterV1 (this.transitSchedule);
		try{
			transitScheduleWriterV1.write(outTransitFile);
		} catch (IOException ex) {
			System.out.println(this + ex.getMessage());
		}
	}
	
	public void createFacilities(NetworkLayer net){
		for (Node node:  net.getNodes().values()){
			this.transitSchedule.addStopFacility(new TransitStopFacility(node.getId(), node.getCoord()));
		}
	}

}