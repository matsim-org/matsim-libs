package playground.mmoyo.PTRouter;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;

/** 
 * Gives information of departures in the complete PT network 
 * *
 * @param timeTable An object with information of departures in each node
 */
public class PTTimeTableInfo {
	private PTTimeTable ptTimeTable;	
	
	public PTTimeTableInfo(PTTimeTable timeTable){
		this.ptTimeTable =timeTable;
	}

	//Reports the next departure to matsim
	public int nextDeparture(Link l, int time){
		return ptTimeTable.nextDeparture(((PTNode)l.getFromNode()).getIdFather(),time );
	}
	
	//Calculates the travel time to cross a link on the basis of the information available in 
	//timetables of father nodes
	//The expected parameter is a Link from PTNetwork so it is assumed to get PTNodes
	public int travelTime (Link l, int time){
		IdImpl idPtLine = ((PTNode)l.getFromNode()).getIdPTLine(); 
		IdImpl idFromFather = ((PTNode)l.getFromNode()).getIdFather();
        IdImpl idToFather= ((PTNode)l.getToNode()).getIdFather();
   		int departure = ptTimeTable.nextDepartureXLine(idFromFather, idPtLine, time); 
		int arrival =ptTimeTable.nextDepartureXLine(idToFather, idPtLine, departure+1); 
		
		idPtLine = null; 
		idFromFather =null;
		idToFather =null;
		
		return arrival - departure;
	}
	
	public void printTimeTable(){
		this.ptTimeTable.dumpTimeTable();
	}
}

/*OLD CODE
/*
private int LookIdPTLine (IdImpl IdPTLine, List <PTTimeTable> tl){
	//List tl =timeTableMap.get(idNode);  este se debe usar para invocar este metodo
	Iterator<PTTimeTable> iter = tl.iterator();
	int index =-1;
	while(iter.hasNext()){   
		index++;
		if (((PTTimeTable)iter).getIdPtLine().equals(IdPTLine)){
			return index;
		}
	}
}		
*/