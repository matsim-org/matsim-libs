package mkillat.tmc;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.utils.geometry.CoordImpl;


public class MessageToNCE {


	NetworkImpl net = NetworkImpl.createNetwork();
	
	IdImpl idN1 = new IdImpl("2");
	IdImpl idN2 = new IdImpl("7");
	IdImpl idN3 = new IdImpl("12");
	
	CoordImpl coord1 = new CoordImpl(13.0, 14.0);
	CoordImpl coord2 = new CoordImpl(15.0, 16.0);
	CoordImpl coord3 = new CoordImpl(17.0, 19.0);
	
	Node node1 = net.createAndAddNode(idN1, coord1);
	Node node2 = net.createAndAddNode(idN2, coord2);
	Node node3 = net.createAndAddNode(idN3, coord3);
	

	IdImpl idL1 = new IdImpl("6");
	IdImpl idL2 = new IdImpl("15");
	
	Link link1 = net.createAndAddLink(idL1, node1, node2, 10.0, 10.0, 10.0, 10.0);
	String linkS1 = "6";
	Link link2 = net.createAndAddLink(idL2, node2, node3, 10.0, 10.0, 10.0, 10.0);
	String linkS2 = "15";
	
//	Die Meldungen sind nach der MsId geordnet eingelsene worden.
//	Jetzt gibt es drei Fälle:
//		1. die MsID kommt einmal vor
//		2. die MsID kommt zweimal vor
//		3. die MsID kommt dreimal vor
//	Um diese drei Fälle überprüfen zu können, ist die Funktion "transform" in drei Teile geteilt.
//		1. Es sind >= 3 Elemente vorhanden.
//		2. Es sind =2 Elemente vorhanden.
//		3. Es ist nur eine Element vorhanden.
	

	public List <NetworkChangeEvent> transform (List <MessagesPlusFactor> messagesPlusFactor){
		List <NetworkChangeEvent> output = new ArrayList <NetworkChangeEvent>();
		String t = "true";
		int i;
		for (i=0; i<messagesPlusFactor.size(); i++){
			
//////////////////////////////////////////////////////////////////////////////////////////////////			
//			1. Teil
			if (i<messagesPlusFactor.size()-2){
			MessagesPlusFactor currentM = messagesPlusFactor.get(i);
			MessagesPlusFactor nextM = messagesPlusFactor.get(i+1);
			MessagesPlusFactor nextM2 = messagesPlusFactor.get(i+2);
			
//			1. Fall
			if (!currentM.msId.equals(nextM.msId)){
				
				StringTimeToDouble aa = new StringTimeToDouble();
				double startTime1 = aa.transformer(currentM.startTime);
				
				
				ChangeType  type = ChangeType.FACTOR;
				ChangeValue cv1 = new ChangeValue (type, currentM.factor);
				
				NetworkChangeEvent message1 = new NetworkChangeEvent (startTime1);
			
				if (currentM.link.equals(linkS1)){
					message1.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message1.addLink(link2);
				}
				if (currentM.flowCapacityChange.equals(t)){
					message1.setFlowCapacityChange(cv1);
				}
				if (currentM.freespeedChange.equals(t)){
					message1.setFreespeedChange(cv1);
				}
				if (currentM.lanesChange.equals(t)){
					message1.setLanesChange(cv1);
				}
				output.add(message1);
				
				double startTime2 = aa.transformer(currentM.endTime);
				NetworkChangeEvent message2 = new NetworkChangeEvent (startTime2);
				double factor = 0;
				factor = 1.0/currentM.factor;
				ChangeValue cv2 = new ChangeValue (type, factor);
				if (currentM.flowCapacityChange.equals(t)){
					message2.setFlowCapacityChange(cv2);
				}
				if (currentM.freespeedChange.equals(t)){
					message2.setFreespeedChange(cv2);
				}
				if (currentM.lanesChange.equals(t)){
					message2.setLanesChange(cv2);
				}
				if (currentM.link.equals(linkS1)){
					message2.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message2.addLink(link2);
				}
				output.add(message2);
				
			}
			
		

//			2. Fall
			if (currentM.msId.equals(nextM.msId) && !currentM.msId.equals(nextM2.msId) ){
				StringTimeToDouble aa = new StringTimeToDouble();
				double startTime1 = aa.transformer(currentM.startTime);
				
				
				ChangeType  type = ChangeType.FACTOR;
				ChangeValue cv1 = new ChangeValue (type, currentM.factor);
				
				NetworkChangeEvent message1 = new NetworkChangeEvent (startTime1);
				
				if (currentM.link.equals(linkS1)){
					message1.addLink(link1);
				}
				if (currentM.link.equals(linkS2)){
					message1.addLink(link2);
				}
				if (currentM.flowCapacityChange.equals(t)){
					message1.setFlowCapacityChange(cv1);
				}
				if (currentM.freespeedChange.equals(t)){
					message1.setFreespeedChange(cv1);
				}
				if (currentM.lanesChange.equals(t)){
					message1.setLanesChange(cv1);
				}
				output.add(message1);
				
				double startTime2 = aa.transformer(nextM.endTime);
				NetworkChangeEvent message2 = new NetworkChangeEvent (startTime2);
				double factor = 0;
				factor = 1.0/currentM.factor;
				ChangeValue cv2 = new ChangeValue (type, factor);
				if (currentM.flowCapacityChange.equals(t)){
					message2.setFlowCapacityChange(cv2);
				}
				if (currentM.freespeedChange.equals(t)){
					message2.setFreespeedChange(cv2);
				}
				if (currentM.lanesChange.equals(t)){
					message2.setLanesChange(cv2);
				}
				if (currentM.link.equals(linkS1)){
					message2.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message2.addLink(link2);
				}
				output.add(message2);
				i++;
					
				
			}
			
//			3. Fall
			if (currentM.msId.equals(nextM.msId) && currentM.msId.equals(nextM2.msId) ){
				StringTimeToDouble aa = new StringTimeToDouble();
				double startTime1 = aa.transformer(currentM.startTime);
				
				
				ChangeType  type = ChangeType.FACTOR;
				ChangeValue cv1 = new ChangeValue (type, currentM.factor);
				
				NetworkChangeEvent message1 = new NetworkChangeEvent (startTime1);
				
				if (currentM.link.equals(linkS1)){
					message1.addLink(link1);
				}
				if (currentM.link.equals(linkS2)){
					message1.addLink(link2);
				}
				if (currentM.flowCapacityChange.equals(t)){
					message1.setFlowCapacityChange(cv1);
				}
				if (currentM.freespeedChange.equals(t)){
					message1.setFreespeedChange(cv1);
				}
				if (currentM.lanesChange.equals(t)){
					message1.setLanesChange(cv1);
				}
				output.add(message1);
				
			
				double startTime2 = aa.transformer(nextM.startTime);
				
				double factor1 = 0;
				factor1 = (1/currentM.factor) * nextM.factor;
				ChangeValue cv2 = new ChangeValue (type, factor1);
				
				NetworkChangeEvent message2 = new NetworkChangeEvent (startTime2);
				
				if (currentM.link.equals(linkS1)){
					message2.addLink(link1);
				}
				if (currentM.link.equals(linkS2)){
					message2.addLink(link2);
				}
				if (currentM.flowCapacityChange.equals(t)){
					message2.setFlowCapacityChange(cv2);
				}
				if (currentM.freespeedChange.equals(t)){
					message2.setFreespeedChange(cv2);
				}
				if (currentM.lanesChange.equals(t)){
					message2.setLanesChange(cv2);
				}
				output.add(message2);
				
				
				
				double startTime3 = aa.transformer(nextM2.endTime);
				NetworkChangeEvent message3 = new NetworkChangeEvent (startTime3);
				double factor3 = 0;
				
				factor3 = 1.0/nextM.factor;
				ChangeValue cv3 = new ChangeValue (type, factor3);
				if (currentM.flowCapacityChange.equals(t)){
					message3.setFlowCapacityChange(cv3);
				}
				if (currentM.freespeedChange.equals(t)){
					message3.setFreespeedChange(cv3);
				}
				if (currentM.lanesChange.equals(t)){
					message3.setLanesChange(cv3);
				}
				if (currentM.link.equals(linkS1)){
					message3.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message3.addLink(link2);
				}
				output.add(message3);
				i = i + 2 ;
					
				
			}

		
		}	
			
/////////////////////////////////////////////////////////////////////////////////////////////
//			2. Teil
			else if (i<messagesPlusFactor.size()-1){
			MessagesPlusFactor currentM = messagesPlusFactor.get(i);
			MessagesPlusFactor nextM = messagesPlusFactor.get(i+1);
			
//			1. Fall
			if (!currentM.msId.equals(nextM.msId)){
				
				StringTimeToDouble aa = new StringTimeToDouble();
				double startTime1 = aa.transformer(currentM.startTime);
				
				
				ChangeType  type = ChangeType.FACTOR;
				ChangeValue cv1 = new ChangeValue (type, currentM.factor);
				
				NetworkChangeEvent message1 = new NetworkChangeEvent (startTime1);
			
				if (currentM.link.equals(linkS1)){
					message1.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message1.addLink(link2);
				}
				if (currentM.flowCapacityChange.equals(t)){
					message1.setFlowCapacityChange(cv1);
				}
				if (currentM.freespeedChange.equals(t)){
					message1.setFreespeedChange(cv1);
				}
				if (currentM.lanesChange.equals(t)){
					message1.setLanesChange(cv1);
				}
				output.add(message1);
				
				double startTime2 = aa.transformer(currentM.endTime);
				NetworkChangeEvent message2 = new NetworkChangeEvent (startTime2);
				double factor = 0;
				factor = 1.0/currentM.factor;
				ChangeValue cv2 = new ChangeValue (type, factor);
				if (currentM.flowCapacityChange.equals(t)){
					message2.setFlowCapacityChange(cv2);
				}
				if (currentM.freespeedChange.equals(t)){
					message2.setFreespeedChange(cv2);
				}
				if (currentM.lanesChange.equals(t)){
					message2.setLanesChange(cv2);
				}
				if (currentM.link.equals(linkS1)){
					message2.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message2.addLink(link2);
				}
				output.add(message2);
				
			}
			
//			2. Fall
			if (currentM.msId.equals(nextM.msId) ){
				StringTimeToDouble aa = new StringTimeToDouble();
				double startTime1 = aa.transformer(currentM.startTime);
				
				
				ChangeType  type = ChangeType.FACTOR;
				ChangeValue cv1 = new ChangeValue (type, currentM.factor);
				
				NetworkChangeEvent message1 = new NetworkChangeEvent (startTime1);
				
				if (currentM.link.equals(linkS1)){
					message1.addLink(link1);
				}
				if (currentM.link.equals(linkS2)){
					message1.addLink(link2);
				}
				if (currentM.flowCapacityChange.equals(t)){
					message1.setFlowCapacityChange(cv1);
				}
				if (currentM.freespeedChange.equals(t)){
					message1.setFreespeedChange(cv1);
				}
				if (currentM.lanesChange.equals(t)){
					message1.setLanesChange(cv1);
				}
				output.add(message1);
				
				double startTime2 = aa.transformer(nextM.endTime);
				NetworkChangeEvent message2 = new NetworkChangeEvent (startTime2);
				double factor = 0;
				factor = 1.0/currentM.factor;
				ChangeValue cv2 = new ChangeValue (type, factor);
				if (currentM.flowCapacityChange.equals(t)){
					message2.setFlowCapacityChange(cv2);
				}
				if (currentM.freespeedChange.equals(t)){
					message2.setFreespeedChange(cv2);
				}
				if (currentM.lanesChange.equals(t)){
					message2.setLanesChange(cv2);
				}
				if (currentM.link.equals(linkS1)){
					message2.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message2.addLink(link2);
				}
				output.add(message2);
				i++;
					
				
			}
		}
		
/////////////////////////////////////////////////////////////////////////////////////////////////
//		3. Teil
			
		else {
			MessagesPlusFactor currentM = messagesPlusFactor.get(i);
		
//			1. Fall
				StringTimeToDouble aa = new StringTimeToDouble();
				double startTime1 = aa.transformer(currentM.startTime);
				
				
				ChangeType  type = ChangeType.FACTOR;
				ChangeValue cv1 = new ChangeValue (type, currentM.factor);
				
				NetworkChangeEvent message1 = new NetworkChangeEvent (startTime1);
			
				if (currentM.link.equals(linkS1)){
					message1.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message1.addLink(link2);
				}
				if (currentM.flowCapacityChange.equals(t)){
					message1.setFlowCapacityChange(cv1);
				}
				if (currentM.freespeedChange.equals(t)){
					message1.setFreespeedChange(cv1);
				}
				if (currentM.lanesChange.equals(t)){
					message1.setLanesChange(cv1);
				}
				output.add(message1);
				
				double startTime2 = aa.transformer(currentM.endTime);
				NetworkChangeEvent message2 = new NetworkChangeEvent (startTime2);
				double factor = 0;
				factor = 1.0/currentM.factor;
				ChangeValue cv2 = new ChangeValue (type, factor);
				if (currentM.flowCapacityChange.equals(t)){
					message2.setFlowCapacityChange(cv2);
				}
				if (currentM.freespeedChange.equals(t)){
					message2.setFreespeedChange(cv2);
				}
				if (currentM.lanesChange.equals(t)){
					message2.setLanesChange(cv2);
				}
				if (currentM.link.equals(linkS1)){
					message2.addLink(link1);
				}
				
				if (currentM.link.equals(linkS2)){
					message2.addLink(link2);
				}
				output.add(message2);
				
			}

		}
		
		return output;
			
	

}

}