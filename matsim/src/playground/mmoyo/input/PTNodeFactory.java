package playground.mmoyo.input;

import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.network.NetworkLayer;

import playground.mmoyo.PTCase2.PTStation;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Validators.StationValidator;

public class PTNodeFactory {

	NetworkLayer net;
	PTTimeTable2 ptTimeTable;
	
	/**
	 * @param net
	 */
	public PTNodeFactory(NetworkLayer net, PTTimeTable2 ptTimeTable) {
		this.net = net;
		this.ptTimeTable = ptTimeTable;
	}

	public void insertNodes(BasicNode basicNode){
		Coord newCoord = basicNode.getCoord();	    		
		String strBaseId = basicNode.getId().toString();
		String strNode1 = "~" + strBaseId;
		String strNode2 = "_" + strBaseId;
		
		PTStation ptStation = new PTStation(ptTimeTable);
		
		List <Id> list = ptStation.getIntersecionMap().get(strBaseId);
		if(list != null){
    		//System.out.println(list.toString());

    		Id firstId= list.get(0);
    		Coord coord = net.getNode(firstId).getCoord();
    		
    		StationValidator sValidator = new StationValidator();
    		if (sValidator.validateStations(net, ptStation)){
    			if (!coord.equals(newCoord)){
    				throw new IllegalArgumentException("Wrong coordinates in node: " + strBaseId + " " + newCoord);
    			}
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

		insertNode(strNode1, newCoord);
		insertNode(strNode2, newCoord);
	}

	private void insertNode(String strId, Coord coord){
		Id idPTNode = new IdImpl(strId);
		PTNode newPTnode = new PTNode(idPTNode, coord,"");
		net.getNodes().put(idPTNode,newPTnode);
	}
	
}
