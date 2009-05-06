package playground.mmoyo.input;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;

import playground.mmoyo.PTCase2.PTStation;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Validators.StationValidator;

public class PTNodeFactory {

	NetworkLayer net;
	PTStation ptStation;;
	StationValidator sValidator;

	public PTNodeFactory(){
		
	}
			
	public PTNodeFactory(NetworkLayer net, PTTimeTable2 ptTimeTable) {
		this.net = net;
		this.ptStation = new PTStation(ptTimeTable);
		this.sValidator = new StationValidator(net);
		this.sValidator.hasValidCoordinates(this.ptStation);
	}

	public NetworkLayer TransformToPTNodes(NetworkLayer NetWithNodes, NetworkLayer NetWithPTNodes){
		for (Node node: NetWithNodes.getNodes().values()){
			PTNode ptNode = new PTNode(node.getId(),node.getCoord(),node.getType());
			NetWithPTNodes.getNodes().put(node.getId(),ptNode);
		}
		return NetWithPTNodes;
	}
	
	public void createPtNode(){
		
		
	}
	
	public BasicNode[] CreatePTNodes(BasicNode basicNode){
		BasicNode[] pair = new BasicNode[2];
		
		Coord newCoord = basicNode.getCoord();	    		
		String strIdStation = basicNode.getId().toString();
		String strNode1 = "~" + strIdStation;
		String strNode2 = "_" + strIdStation;
		
		List <Id> list = this.ptStation.getIntersecionMap().get(strIdStation);
		if(list != null){
    		Id firstId= list.get(0);
    		Coord coord = net.getNode(firstId).getCoord();
    		
   			if (!coord.equals(newCoord)){
    			//-> 23 apr decide what to do here: throw exception or correct the coordinate taking first value
   				//throw new IllegalArgumentException("Wrong coordinates in node: " + strBaseId + " " + newCoord);
   				newCoord=coord;
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

		pair[0]= insertNode(strIdStation, strNode1, newCoord);
		pair[1]=  insertNode(strIdStation, strNode2, newCoord);
		//System.out.println(pair[0].getId() + ", "  + pair[1].getId());

		return pair;
	}

	private BasicNode insertNode(String strIdStation, String strId, Coord coord){
		Id idPTNode = new IdImpl(strId);
		PTNode ptNode = new PTNode(idPTNode, coord, "");
		ptNode.setIdStation(new IdImpl(strIdStation));
		net.getNodes().put(idPTNode, ptNode);
		return ptNode;
	}
	
}
