package playground.mmoyo.pttest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;

/*TODO: resolvar si el mapa de rutas se guarda tambien en cada nodo o solo en una lista general
*/
public class PTTimeTableInfo {
	private Map <IdImpl, List<PTTimeTable>> timeTableMap = new TreeMap <IdImpl, List<PTTimeTable>>();		
	
	public PTTimeTableInfo(Map <IdImpl, List<PTTimeTable>> timeTable){
		this.timeTableMap =timeTable;
		//printTimeTable();
		System.out.println(ToString(NextDeparture(new IdImpl("1"),ToSeconds("13:15"))) );		
	}
	
	public void printTimeTable() {
		Iterator iter = timeTableMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			List tl = (ArrayList)entry.getValue();
			for (Iterator<PTTimeTable> iter2 = tl.iterator(); iter2.hasNext();) {
				PTTimeTable tt= (PTTimeTable)iter2.next();
				System.out.println("\n Node:"+ entry.getKey() + " = Line:" +	tt.getIdPtLine().toString());
				for (int x=0; x< tt.getDeparture().length; x++){
					System.out.print(tt.getDeparture()[x]+ " ");	
				}//for x
			}//for iterator
		}//while
		iter = null;
	}// showmap

	private int NextDeparture(IdImpl idNode, int time){ 		//for Matsim
		List tl =timeTableMap.get(idNode);
		int x2 = Integer.MAX_VALUE;
		for (Iterator<PTTimeTable> iter = tl.iterator(); iter.hasNext();) {
			PTTimeTable tt= (PTTimeTable)iter.next();
			
			int x = LoopDepartures(time,tt.getDeparture());
			if  (x < x2) {
				x2= x;
				}
		}//for iterator
		return x2;
	} 
	
	public int LoopDepartures(int intTime1,int[] arrDep){  //,
		int x=0;
		while (arrDep[x] < intTime1){
			x++;
			if (x==(arrDep.length)){ //if there is not a PT vehicle this day, we look  since beginning
				return arrDep[0];
			}
		}	
		return arrDep[x];
	}
	
	private int ToSeconds(String strDeparture){
		String[] strTime = strDeparture.split(":");  //if we had seconds:  + (departure + ":00").split(":");   //
		return ((Integer.parseInt(strTime[0]) * 3600) + (Integer.parseInt(strTime[1]))*60) ;  	////if we had seconds:   + Integer.parseInt(strTime[2] 
	}
	
	private String ToString(int intDeparture){
		//Converts integers into format "hh:mm"
		StringBuffer buffer = new StringBuffer(5);
		buffer.append(String.format("%02d", intDeparture/3600));
		buffer.append(":");
		buffer.append(String.format("%02d", (intDeparture%3600)/60));
		buffer.append(" ");
		return  buffer.toString(); 
	}	
	
}