package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleWriterV1;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTLine;

public class ToTransitScheduleConverter {
	ActivityFacilities facilities = new ActivityFacilitiesImpl();
	private static Time time;
	
	public ToTransitScheduleConverter (){
				
	}
	
	public void createTransitSchedule(final PTTimeTable2 ptTimeTable, final String inputFacilityFile, String outTransitFile) {
		FacilitiesReaderMatsimV1 facilityReader = new FacilitiesReaderMatsimV1(facilities);
		facilityReader.readFile(inputFacilityFile);
		System.out.println("Facilities found: " + facilities.getFacilities().size());

		TransitSchedule transitSchedule = new TransitSchedule();
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
				}*/
			
				ActivityFacility stop = facilities.getFacilities().get(idNode);
				
				double min = ((Double)ptLine.getMinutes().get(x)).doubleValue();
				double arrivalDelay = min*60;
				double departureDelay = min*60;
				stops.add(new TransitRouteStop(stop, arrivalDelay, departureDelay));

				x++;
			}
			
			Id idTransitRoute = new IdImpl(ptLine.getId() + ptLine.getDirection());
			TransitLine transitLine = new TransitLine(ptLineId);
			NetworkRoute networkRoute= null; //new NodeNetworkRoute(null, null);
			TransitRoute transitRoute = new  TransitRoute(idTransitRoute, networkRoute, stops, TransportMode.pt);
			
			x=0;
			for (String strDeparture : ptLine.getDepartures()){
				Id idDeparture = new IdImpl(x + "." + ptLine.getId());
				double dblDeparture=  time.parseTime(strDeparture);
				Departure departure = new Departure (idDeparture, dblDeparture);
				transitRoute.addDeparture(departure);
				x++;
			}
			
			transitLine.addRoute(transitRoute);
			transitSchedule.addTransitLine(transitLine);
		}

		TransitScheduleWriterV1 transitScheduleWriterV1 = new TransitScheduleWriterV1 (transitSchedule);
		try{
			transitScheduleWriterV1.write(outTransitFile);
		} catch (IOException ex) {
			System.out.println(this + ex.getMessage());
		}
	}
	
	
	public void createFacilities(NetworkLayer net, String outFile){
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		
		for (Node node:  net.getNodes().values()){
			facilities.createFacility(node.getId(), node.getCoord());
		}
		
		FacilitiesWriter writer= new FacilitiesWriter(facilities, outFile);
		writer.write();
	}

}