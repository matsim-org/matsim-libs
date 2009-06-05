package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.transitSchedule.*;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTLine;

/**
 * From PTtimeTable to transitShcedule converter 
 */
public class ToTransitScheduleConverter {
	final TransitSchedule transitSchedule = new TransitSchedule();
	
	public ToTransitScheduleConverter (){
				
	}
	
	public void createTransitSchedule(final PTTimeTable2 ptTimeTable, final NetworkLayer ptNet, String outTransitFile) {
		for (Node node:  ptNet.getNodes().values()){
			this.transitSchedule.addStopFacility(new TransitStopFacility(node.getId(), node.getCoord()));
		}
		
		for (PTLine ptLine: ptTimeTable.getPtLineList()){
			Id ptLineId = ptLine.getId();
			//String ptLineType= ptLine.getPtLineType().getType();  //-> if possible add more choices to MODE
			int x=0;
			
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			for (Id idNode : ptLine.getNodeRoute()){				
				//Node node= ptNet.getNode(idNode);
				//TransitStopFacility transitStopFacility =  new 	TransitStopFacility(node.getId(), node.getCoord());
				//transitSchedule.addStopFacility(transitStopFacility);
				
				double min = ptLine.getMinutes().get(x++).doubleValue();  
				double arrivalDelay = min*60;
				double departureDelay = min*60;
				transitRouteStops.add(new TransitRouteStop(transitSchedule.getFacilities().get(idNode), arrivalDelay, departureDelay));
			}
			
			Id idTransitRoute = new IdImpl(ptLine.getId() + ptLine.getDirection());
			TransitLine transitLine = new TransitLine(ptLineId);
			NetworkRoute networkRoute= null; //new NodeNetworkRoute(null, null);
			TransitRoute transitRoute = new  TransitRoute(idTransitRoute, networkRoute, transitRouteStops, TransportMode.pt);
			
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
	
	}
	
	
	
	
}