package playground.mmoyo.pttest;

import java.util.Iterator;
import java.util.List;

public class PTTimeTableInfo {
	private List<PTTimeTable> timeTable;
	
	public PTTimeTableInfo(List<PTTimeTable> timeTable){
		this.timeTable =timeTable;
		//System.out.println(ToString(NextDeparture("09:15",timeTable.get(0).getDeparture())));		
	}

	public void printTimetable(){
		for (Iterator<PTTimeTable> iter = timeTable.iterator(); iter.hasNext();) {
			PTTimeTable ptTimeTable = iter.next();
			System.out.println("\n" + ptTimeTable.getIdNode().toString()+ "-" + ptTimeTable.getIdPtLine().toString() + "-" ); //+ 
			for (int x= 0; x < ptTimeTable.getDeparture().length; x++){
				System.out.print(ToString(ptTimeTable.getDeparture()[x]) + " " );
			}
		}
	}
	
	
	public int NextDeparture(String strTime1,int[] arrDep){  //,
		int intTime1 = ToSeconds(strTime1);
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
		//TODO: force format "00:00"
		int hour =intDeparture/3600;
		int min = (intDeparture%3600)/60;
		return String.valueOf(hour) + ":" + String.valueOf(min);
	}	
}