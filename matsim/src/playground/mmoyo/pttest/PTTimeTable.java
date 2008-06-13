package playground.mmoyo.pttest;

import org.matsim.basic.v01.IdImpl;

public class PTTimeTable {
	private IdImpl idPtLine;
	private int[] departure;
	
	public PTTimeTable(IdImpl idPtLine,int[] departures) {
		this.idPtLine= idPtLine;
		this.departure = departures;
	}

	public IdImpl getIdPtLine() {
		return idPtLine;
	}

	public void setIdPtLine(IdImpl idPtLine) {
		this.idPtLine = idPtLine;
	}

	public int[] getDeparture() {
		return departure;
	}

	public void setDeparture(int[] departure) {
		this.departure = departure;
	}
}