package playground.mmoyo.input;

import java.util.List;

import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTLine;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.NetworkRoute;
import java.util.ArrayList;

import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitScheduleWriterV1;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;

public class ToTransitScheduleConverter {

	public ToTransitScheduleConverter(PTTimeTable2 ptTimeTable ) {
		TransitSchedule transitSchedule = new TransitSchedule();
		
		for (PTLine ptLine: ptTimeTable.getPtLineList()){
			Id idTransitLine = ptLine.getId();
			String type= ptLine.getPtLineType().getType();
			Double[] minutes = ptLine.getMinutes().toArray(new Double[ptLine.getMinutes().size()]);
			int x=0;
			for (String strIdNode : ptLine.getRoute()){
				String stopRefId = strIdNode;
				double departure =  minutes[x++];
				//TODO: identify links of the route
			}
		
			TransitLine transitLine = new TransitLine(idTransitLine);
			Id idTransitRoute = new IdImpl("H");  //temporal
			NetworkRoute networkRoute; // = new ????
			List<TransitRouteStop> TransitRouteStopList = new ArrayList<TransitRouteStop>(); //TODO
			TransportMode transportMode; //TODO;
			// TODO: TransitRoute transitRoute = new TransitRoute(idTransitRoute, networkRoute, TransitRouteStopList, transportMode);
			//TODO: transitLine.addRoute(transitRoute);
			transitSchedule.addTransitLine(transitLine);
		}

		TransitScheduleWriterV1 transitScheduleWriterV1 = new TransitScheduleWriterV1 (transitSchedule);
	}

}
