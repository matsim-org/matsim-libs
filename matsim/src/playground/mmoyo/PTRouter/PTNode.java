package playground.mmoyo.PTRouter;

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
		
}