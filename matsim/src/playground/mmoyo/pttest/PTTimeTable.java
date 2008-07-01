package playground.mmoyo.pttest;

import org.matsim.basic.v01.IdImpl;

public class PTTimeTable {
	private IdImpl idPtLine;
	private int[] departureTime;
	
	public PTTimeTable(IdImpl idPtLine,int[] departures) {
		this.idPtLine= idPtLine;
		this.departureTime = departures;
	}

	public IdImpl getIdPtLine() {
		return idPtLine;
	}

	public void setIdPtLine(IdImpl idPtLine) {
		this.idPtLine = idPtLine;
	}

	public int[] getDepartureTime() {
		return departureTime;
	}

	public void setDeparture(int[] departureTime) {
		this.departureTime = departureTime;
	}
}