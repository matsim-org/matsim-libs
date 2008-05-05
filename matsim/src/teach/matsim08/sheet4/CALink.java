package teach.matsim08.sheet4;
import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.vis.netvis.DrawableAgentI;

import teach.matsim08.network.CANetStateWritableI;


public class CALink implements CANetStateWritableI, BasicLink  {

	final double CELLSIZE = 37.5 ;

	private int nCells;

	private List cells;

	private BasicLink basicLink;

	public CALink() {
	}

	public CALink(BasicLink l) {
		this.basicLink = l;
	}

	public List<DrawableAgentI> getDisplayAgents() {
		List<DrawableAgentI> list = new ArrayList() ;
		for ( int ii=0 ; ii< nCells ; ii++ ) {
			CAVehicle veh = (CAVehicle) cells.get(ii) ;
			if ( veh!= null ) {
				veh.setPosition(ii*CELLSIZE ) ;
				list.add( veh ) ;
			}
		}
		return list ;
	}

	public double getDisplayValue() {
		return 0.1;
	}


	public void removeFirstVeh() {
		cells.set(nCells-1, null);
	}

	public CAVehicle getFirstVeh() {
		return (CAVehicle) cells.get(nCells-1);
	}

	public boolean hasSpace() {
		return cells.get(0) == null;
	}

	public void doBoundary() {
		if (getFirstVeh() != null && hasSpace()) {
			CAVehicle veh = getFirstVeh();
			removeFirstVeh();
			addVeh(veh);
		}
	}

	public void addVeh(CAVehicle veh) {
		cells.set(0,veh);
	}




	public void build() {
		// calc number of cells
		nCells = (int)(getLength() /CELLSIZE);
		cells = new ArrayList(nCells);
		// Fill the cells up to nCells
		for (int i = 0; i < nCells; i++) cells.add(null);
	}

	public void tty() {
		for (int i=0; i< nCells; i++) {
			if (cells.get(i) != null) System.out.print("X");
			else System.out.print(".");
		}
		System.out.println("");
	}


	public void randomFill(double d) {
		for (int i=0; i< nCells; i++) {
			if (Math.random() >= d ) cells.set(i, new CAVehicle());
		}
	}

	public void move() {
		for (int i=0; i < nCells -1; i++) {
			if (cells.get(i) != null && cells.get(i+1) == null) {
				CAVehicle veh = (CAVehicle) cells.get(i);
				cells.set(i+1, veh);
				cells.set(i, null);
				i++; // Avoid multiple moves of the same vehicle
			}
		}
	}

	//delegates
	
	public void setLength(double l) {
		this.basicLink.setLength(l);
	}
	
	public double getCapacity() {
		return basicLink.getCapacity();
	}

	public double getFreespeed(double time) {
		return basicLink.getFreespeed(time);
	}

	public BasicNode getFromNode() {
		return basicLink.getFromNode();
	}

	public Id getId() {
		return basicLink.getId();
	}

	public double getLanes() {
		return basicLink.getLanes();
	}

	public int getLanesAsInt() {
		return basicLink.getLanesAsInt();
	}

	public double getLength() {
		return basicLink.getLength();
	}

	public BasicNode getToNode() {
		return basicLink.getToNode();
	}

	public void setCapacity(double capacity) {
		basicLink.setCapacity(capacity);
	}

	public void setFreespeed(double freespeed) {
		basicLink.setFreespeed(freespeed);
	}

	public boolean setFromNode(BasicNode node) {
		return basicLink.setFromNode(node);
	}

	public void setLanes(double lanes) {
		basicLink.setLanes(lanes);
	}

	public boolean setToNode(BasicNode node) {
		return basicLink.setToNode(node);
	}

}
