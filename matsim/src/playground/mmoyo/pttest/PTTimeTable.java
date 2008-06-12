package playground.mmoyo.pttest;

import org.matsim.basic.v01.IdImpl;

public class PTTimeTable {
	private IdImpl idNode;
	private IdImpl idPtLine;
	private int[] departure;
	
	public PTTimeTable(IdImpl idNode, IdImpl idPtLine,int[] departures) {
		this.idNode = idNode;
		this.idPtLine= idPtLine;
		this.departure = departures;
	}

	public IdImpl getIdNode() {
		return idNode;
	}

	public void setIdNode(IdImpl idNode) {
		this.idNode = idNode;
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