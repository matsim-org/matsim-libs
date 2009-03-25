package playground.mmoyo.input;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.basic.v01.IdImpl;
import org.matsim.core.api.network.Node;
import org.matsim.network.NetworkLayer;

import playground.mmoyo.PTCase2.PTStationMap;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Validators.StationValidator;

public class PTNodeFactory {

	NetworkLayer net;
	PTStationMap ptStationMap;;
	StationValidator sValidator;

	public PTNodeFactory(){
		
	}
			
	public PTNodeFactory(NetworkLayer net, PTTimeTable2 ptTimeTable) {
		this.net = net;
		this.ptStationMap = new PTStationMap(ptTimeTable);
		this.sValidator = new StationValidator(net);
		this.sValidator.hasValidCoordinates(this.ptStationMap);
	}

	public NetworkLayer TransformToPTNodes(NetworkLayer NetWithNodes, NetworkLayer NetWithPTNodes){
		for (Node node: NetWithNodes.getNodes().values()){
			PTNode ptNode = new PTNode(node.getId(),node.getCoord(),node.getType());
			NetWithPTNodes.getNodes().put(node.getId(),ptNode);
		}
		return NetWithPTNodes;
	}
	
	
	public BasicNode[] CreatePTNodes(BasicNode basicNode){
		BasicNode[] pair = new BasicNode[2];
		
		Coord newCoord = basicNode.getCoord();	    		
		String strBaseId = basicNode.getId().toString();
		String strNode1 = "~" + strBaseId;
		String strNode2 = "_" + strBaseId;
		
		List <Id> list = this.ptStationMap.getIntersecionMap().get(strBaseId);
		if(list != null){
    		Id firstId= list.get(0);
    		Coord coord = net.getNode(firstId).getCoord();
    		
   			if (!coord.equals(newCoord)){
    			throw new IllegalArgumentException("Wrong coordinates in node: " + strBaseId + " " + newCoord);
   			}
    		
    		char greatestChar = 'a';
    		for (Id id : list){
    			String strId= id.toString();
    			char lChar = strId.charAt(strId.length()-1);
				if(Character.isLetter(lChar)){
					if(lChar>greatestChar){
						greatestChar= lChar;
					}
				}
    		}
    		char nextChar=++greatestChar;
    		//System.out.println("nextChar:" + nextChar);
		
    		strNode1 = strNode1 + nextChar;
    		strNode2 = strNode2 + nextChar;
		}

		pair[0]= insertNode(strNode1, newCoord);
		pair[1]=  insertNode(strNode2, newCoord);
	
		return pair;
	}

	private BasicNode insertNode(String strId, Coord coord){
		Id idPTNode = new IdImpl(strId);
		PTNode ptNode = new PTNode(idPTNode, coord, "");
		net.getNodes().put(idPTNode, ptNode);
		return ptNode;
	}
	
}
