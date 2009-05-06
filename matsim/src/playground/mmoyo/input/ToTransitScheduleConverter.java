package playground.mmoyo.input;

import org.matsim.core.network.NetworkLayer;
import java.util.List;

import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTLine;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkLayer;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import java.util.ArrayList;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;

import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitScheduleWriterV1;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;

public class ToTransitScheduleConverter {
	Facilities facilities = new FacilitiesImpl();
	
	public ToTransitScheduleConverter (){
				
	}
	
	public void createTransitSchedule(final PTTimeTable2 ptTimeTable, final String inputFacilityFile, String outTransitFile) {
		FacilitiesReaderMatsimV1 facilityReader = new FacilitiesReaderMatsimV1(facilities);
		facilityReader.readFile(inputFacilityFile);
		System.out.println("Facilities found: " + facilities.getFacilities().size());

		TransitSchedule transitSchedule = new TransitSchedule();
		for (PTLine ptLine: ptTimeTable.getPtLineList()){
			Id ptLineId = ptLine.getId();
			String ptLineType= ptLine.getPtLineType().getType();
			String[] departures = ptLine.getDepartures().toArray(new String[ptLine.getDepartures().size()]);
			Double[] minutes = ptLine.getMinutes().toArray(new Double[ptLine.getMinutes().size()]);
			
			//StringBuffer bufferRoute = new StringBuffer();;
			int x=0;
			
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			for (String strIdNode : ptLine.getRoute()){
				Id idNode = new IdImpl(strIdNode);

				/*
				facilities.getFacilities().containsKey(idNode)){
					throw new NullPointerException(this + "facility [id=" + idNode + " does not exist]");
				}
				*/
				
				//bufferRoute.append(strIdNode);
				//bufferRoute.append(" ");
			
				Facility stop = facilities.getFacilities().get(idNode);
				double arrivalDelay = ptLine.getMinutes().get(x);
				double departureDelay = ptLine.getMinutes().get(x);
				stops.add(new TransitRouteStop(stop, arrivalDelay, departureDelay));
			}
			
			Id idTransitRoute = new IdImpl(ptLine.getId() + ptLine.getDirection());
			TransitLine transitLine = new TransitLine(ptLineId);
			NetworkRoute networkRoute = new NodeNetworkRoute(null, null);
			TransitRoute transitRoute = new  TransitRoute(idTransitRoute, networkRoute, stops, TransportMode.pt);
			
			transitLine.addRoute(transitRoute);
			transitSchedule.addTransitLine(transitLine);

		}

		TransitScheduleWriterV1 transitScheduleWriterV1 = new TransitScheduleWriterV1 (transitSchedule);
		//transitScheduleWriterV1.write(outTransitFile);
	}
	
	public void createFacilities(NetworkLayer net, String outFile){
		Facilities facilities = new FacilitiesImpl();
		
		for (Node node:  net.getNodes().values()){
			facilities.createFacility(node.getId(), node.getCoord());
		}
		
		FacilitiesWriter writer= new FacilitiesWriter(facilities, outFile);
		writer.write();
	}

}