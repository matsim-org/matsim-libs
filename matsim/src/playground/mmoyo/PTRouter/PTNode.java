package playground.mmoyo.PTRouter;

import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.geometry.Coord;
import org.matsim.network.Node;

/** 
 * Node with necessary data for the PT simulation 
 * These nodes are installed in a different layer in independent paths according to each PtLine route  
 *
 * @param idFather the "real" node from which is copied
 * @param idPTLine the PT line that exclusively travels through the node
 */
public class PTNode extends Node {
	private IdImpl idFather;
	private IdImpl idPTLine;
	private String stationName;

	public PTNode(IdImpl idImpl, Coord coord, final String type, IdImpl idFather, IdImpl idPTLine) {
		super(idImpl, coord, type);
		this.idFather = idFather;
		this.idPTLine = idPTLine;
	}

	public PTNode(IdImpl idImpl, Coord coord, final String type){
		super(idImpl, coord, type);
	}

	public IdImpl getIdFather() {
		return idFather;
	}

	public void setIdFather(IdImpl idFather) {
		this.idFather = idFather;
	}

	public IdImpl getIdPTLine() {
		return idPTLine;
	}

	public void setIdPTLine(IdImpl idPTLine) {
		this.idPTLine = idPTLine;
	}

	public String getStationName() {
		return stationName;
	}

	public void setStationName(String stationName) {
		this.stationName = stationName;
	}
}