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
	private Id idFather;   //--> this must be changed to "idStation"
	private Id idPTLine;
	private int indexInRoute; 
	private int minutesAfterDeparture;
	
	public PTNode(final Id id, final Coord coord, final String type, final Id idFather, final Id idPTLine) {
		super(id, coord, type);
		this.idFather = idFather;
		this.idPTLine = idPTLine;
	}

	public int getIndexInRoute() {
		return indexInRoute;
	}

	public void setIndexInRoute(int indexInRoute) {
		this.indexInRoute = indexInRoute;
	}

	public int getMinutesAfterDeparture() {
		return minutesAfterDeparture;
	}

	public void setMinutesAfterDeparture(int minutesAfterDeparture) {
		this.minutesAfterDeparture = minutesAfterDeparture;
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
}