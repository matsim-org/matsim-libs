package playground.mmoyo.input;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

import playground.mmoyo.input.PTStation;
import playground.mmoyo.PTRouter.PTTimeTable2;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Validators.StationValidator;

public class PTNodeFactory {

	private NetworkLayer net;
	private PTStation ptStation;;
	private StationValidator sValidator;

	public PTNodeFactory(){
		
	}
			
	public PTNodeFactory(final NetworkLayer net, final PTTimeTable2 ptTimeTable) {
		this.net = net;
		this.ptStation = new PTStation(ptTimeTable);
		this.sValidator = new StationValidator(net);
		this.sValidator.hasValidCoordinates(this.ptStation);
	}

	/**
	 * converts all nodes from the network into PTNodes 
	 */
	public NetworkLayer transformToPTNodes(final NetworkLayer netWithNodes, NetworkLayer netWithPTNodes){
		for (NodeImpl node: netWithNodes.getNodes().values()){
			PTNode ptNode = new PTNode(node.getId(),node.getCoord(),node.getType());
			netWithPTNodes.getNodes().put(node.getId(),ptNode);
		}
		return netWithPTNodes;
	}
	
	/**
	 * Receives a original Basicnode and creates a pair of PTNodes with prefix indicating both directions and sufix to differentiate from other PTL's 
	 */
	public BasicNode[] createPTNodes(final BasicNode basicNode){
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

	/**
	 * Creates a new PTNode and put it in the network
	 */
	private BasicNode insertNode(final String strIdStation, final String strId, final Coord coord){
		Id idPTNode = new IdImpl(strId);
		PTNode ptNode = new PTNode(idPTNode, coord, "");
		ptNode.setIdStation(new IdImpl(strIdStation));
		net.getNodes().put(idPTNode, ptNode);
		return ptNode;
	}
	
}
