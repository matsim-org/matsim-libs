package playground.mmoyo.pttest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;

/*TODO: resolver si el mapa de rutas se guarda tambien en cada nodo o solo en una lista general
*/
public class PTTimeTableInfo {

	private Map <IdImpl, Map<IdImpl,int[]>> timeTableMap = new TreeMap <IdImpl, Map<IdImpl,int[]>>();
	
	public PTTimeTableInfo(Map <IdImpl, Map<IdImpl,int[]>> timeTable){
		this.timeTableMap =timeTable;
	}
	
	public int NextDepartureR(Link l, int time){
		//Reports the next departure to matsim
		return NextDeparture(((PTNode)l.getFromNode()).getIdFather(),time );
	}
	
	public void NextDepartureH(String IdNode, String time){      
	//Report the next departure in humand understable format
		System.out.println(ToString(NextDeparture(new IdImpl(IdNode),ToSeconds(time))) );
	}
	
	private int NextDeparture(IdImpl idNode, int time){//Core 
		Map<IdImpl,int[]> map = timeTableMap.get(idNode); 
		int x2 = Integer.MAX_VALUE;
		for (Iterator iter =map.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			int[] tt = (int[])entry.getValue();
			int x = LoopDepartures(time,tt);
			if  (x < x2){
				x2= x;
			}
		}
		return x2;
	}	
	
	public int LoopDepartures(int intTime,int[] arrDep){//,
		//Finds the departure prior to the given time. If there is not a PT vehicle 
		//in the current day, it start looking again since the beginning
		int x=0;
		int length = arrDep.length;
		while (arrDep[x] < intTime){
			x++;
			if (x==length){ //
				return arrDep[0];
			}
		}
		return arrDep[x];
	}
	
	public int TravelTime (Link l, int time){
		//Calculate the travel time to cross a link on the basis of the information available in 
		//timetables of father nodes
		//The expected parameter is a Link from PTNetwork so it is assumed to get PTNodes
		
		IdImpl idPtLine = ((PTNode)l.getFromNode()).getIdPTLine(); 
		IdImpl idFromFather = ((PTNode)l.getFromNode()).getIdFather();
        IdImpl idToFather= ((PTNode)l.getToNode()).getIdFather();
		
		int departure = NextDepartureXLine(idFromFather, idPtLine, time); 
		int arrival =NextDepartureXLine(idToFather, idPtLine, departure+1); 
		
		idPtLine = null; 
		idFromFather =null;
		idToFather =null;
		
		return arrival - departure;
	}
	
	public int NextDepartureXLine(IdImpl idNode, IdImpl idPTLine, int time){
		Map<IdImpl,int[]> mp =timeTableMap.get(idNode);
		if (!mp.containsKey(idPTLine)){
			throw new NullPointerException("idNode " + idNode + " The Line " + idPTLine.toString()  +  " does not exist");
		}
		return LoopDepartures (time,mp.get(idPTLine));
	}
	
	private int ToSeconds(String strDeparture){
		//Convert a string in format hh:mm into integer representing millisecond after the midnight
		String[] strTime = strDeparture.split(":");  //if we had seconds:  + (departure + ":00").split(":");   //
		return ((Integer.parseInt(strTime[0]) * 3600) + (Integer.parseInt(strTime[1]))*60) ;  	////if we had seconds:   + Integer.parseInt(strTime[2] 
	}
	
	private String ToString(int intDeparture){
		//Converts integers representing a time into format "hh:mm"
		StringBuffer buffer = new StringBuffer(5);
		buffer.append(String.format("%02d", intDeparture/3600));
		buffer.append(":");
		buffer.append(String.format("%02d", (intDeparture%3600)/60));
		buffer.append(" ");
		return  buffer.toString(); 
	}	
	
	public void printTimeTable() {
		//Displays the information of departures 
		Iterator iter = timeTableMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			List tl = (ArrayList)entry.getValue();
			for (Iterator<PTTimeTable> iter2 = tl.iterator(); iter2.hasNext();) {
				PTTimeTable tt= (PTTimeTable)iter2.next();
				System.out.println("\n Node:"+ entry.getKey() + " = Line:" +	tt.getIdPtLine().toString());
				for (int x=0; x< tt.getDepartureTime().length; x++){
					System.out.print(tt.getDepartureTime()[x]+ " ");	
				}
			}
		}
		iter = null;
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