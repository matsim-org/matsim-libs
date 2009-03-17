package playground.mmoyo.PTRouter;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.NodeImpl;

/**
 * Node with necessary data for the PT simulation
 * These nodes are installed in a different layer in independent paths according to each PtLine route
 *
 * @param idFather the "real" node from which is copied
 * @param idPTLine the PT line that exclusively travels through the node
 */
public class PTNode extends NodeImpl {
	private Id idFather;
	private Id idPTLine;
	private String stationName;
	private Id IdFareStage;

	public PTNode(final Id id, final Coord coord, final String type, final Id idFather, final Id idPTLine) {
		super(id, coord, type);
		this.idFather = idFather;
		this.idPTLine = idPTLine;
	}

	public PTNode(final Id id, final Coord coord, final String type){
		super(id, coord, type);
	}

	public Id getIdFather() {
		return this.idFather;
	}

	public void setIdFather(final Id idFather) {
		this.idFather = idFather;
	}

	public Id getIdPTLine() {
		return this.idPTLine;
	}

	public void setIdPTLine(final Id idPTLine) {
		this.idPTLine = idPTLine;
	}

	public String getStationName() {
		return this.stationName;
	}

	public void setStationName(final String stationName) {
		this.stationName = stationName;
	}

	public Id getIdFarestage() {
		return this.IdFareStage;
	}

	public void setIdFarestage(final Id idFareStage) {
		this.IdFareStage = idFareStage;
	}
}