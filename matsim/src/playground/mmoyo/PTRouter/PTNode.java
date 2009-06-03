package playground.mmoyo.PTRouter;

import java.util.Arrays;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.NodeImpl;

/**
 * Node with necessary data for the PT simulation
 * These nodes are installed in a different layer in independent paths according to each PtLine route
 *
 * @param idFather the "real" node from which is copied
 * @param idPTLine the PT line that exclusively travels through the node
 */
public class PTNode extends NodeImpl {
	private Id idStation;   
	private Id idPTLine;
	private int lineSequenceindex; 
	private int minutesAfterDeparture;
	private double[]arrDep = new double[72];  //30 april  //-> eliminate this
		
	public PTNode(final Id id, final Coord coord, final String type, final Id idStation, final Id idPTLine) {
		super(id, coord, type);
		this.idStation = idStation;
		this.idPTLine = idPTLine;
	}

	public PTNode(final Id id, final Coord coord, final Id idStation, final Id idPTLine, int lineSequenceindex) {
		super(id, coord, "PtNode");
		this.idStation = idStation;
		this.idPTLine = idPTLine;
		this.lineSequenceindex= lineSequenceindex;
		
		//ficticious timetable for the being time
		//-> eliminate this	
		int x=0;
		for(double time = 18000; time< 82800; time= time+900 ){
			arrDep[x++] = time;
		}
		//**********************
	}
	
	public PTNode(final Id id, final Coord coord, final String type){
		super(id, coord, type);
	}
	
	public int getLineSequenceindex() {
		return lineSequenceindex;
	}

	public void setLineSequenceindex(int lineSequenceindex) {
		this.lineSequenceindex = lineSequenceindex;
	}

	public int getMinutesAfterDeparture() {
		return minutesAfterDeparture;
	}

	public void setMinutesAfterDeparture(int minutesAfterDeparture) {
		this.minutesAfterDeparture = minutesAfterDeparture;
	}

	public Id getIdStation() {
		return this.idStation;
	}

	public void setIdStation(final Id idStation) {
		this.idStation = idStation;
	}
	
	public Id getIdPTLine() {
		return this.idPTLine;
	}

	public void setIdPTLine(final Id idPTLine) {
		this.idPTLine = idPTLine;
	}
	
	public double[] getArrDep() {
		return arrDep;
	}

	public void setArrDep(double[] arrDep) {
		this.arrDep = arrDep;
	}
	
	public String getStrIdStation(){
		
		String baseID = this.id.toString();
		if (baseID.charAt(0)=='_' || baseID.charAt(0)=='~')
			baseID= baseID.substring(1,baseID.length());
		if(Character.isLetter(baseID.charAt(baseID.length()-1))) 	//example of possible node values at intersection:   999, _999, 999b, _999b
			baseID= baseID.substring(0,baseID.length()-1);
		return baseID;
	}
	
	public double transferTime (double time){//,
		int length = arrDep.length;
		int index =  Arrays.binarySearch(arrDep, time);
		if (index<0){
			index = -index;
			if (index <= length)index--; else index=0;	
		}else{
			if (index < (length-1))index++; else index=0;	
		}
		double nextDeparture = arrDep[index];
		
		double transTime = nextDeparture-time;
		if (transTime<0){//wait till next day first departure
			transTime= 86400-time+ nextDeparture;
		}
		
		return transTime;
		
	}
		
}