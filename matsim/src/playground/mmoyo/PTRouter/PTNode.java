package playground.mmoyo.PTRouter;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.network.Node;

/** 
 * Node with necessary data for the PT simulation 
 * These nodes are installed in a different layer in independent paths according to each PtLine route  
 *
 * @param idFather the "real" node from which is copied
 * @param idPTLine the PT line that exclusively travels through the node
 */
public class PTNode extends Node {
	private Id idFather;
	private Id idPTLine;
	private String stationName;
	private Id IdFareStage;
	
	public PTNode(Id id, Coord coord, final String type, Id idFather, Id idPTLine) {
		super(id, coord, type);
		this.idFather = idFather;
		this.idPTLine = idPTLine;
	}

	public PTNode(Id id, Coord coord, final String type){
		super(id, coord, type);
	}

	public Id getIdFather() {
		return idFather;
	}

	public void setIdFather(Id idFather) {
		this.idFather = idFather;
	}

	public Id getIdPTLine() {
		return idPTLine;
	}

	public void setIdPTLine(Id idPTLine) {
		this.idPTLine = idPTLine;
	}

	public String getStationName() {
		return stationName;
	}

	public void setStationName(String stationName) {
		this.stationName = stationName;
	}

	public Id getIdFarestage() {
		return IdFareStage;
	}

	public void setIdFarestage(Id idFareStage) {
		IdFareStage = idFareStage;
	}	
}