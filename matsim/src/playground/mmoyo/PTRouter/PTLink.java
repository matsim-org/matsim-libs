package playground.mmoyo.PTRouter;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;

//Validate the need of this class
public class PTLink extends LinkImpl {
	private String idPTLine;
	private boolean isBusStop;
	private String ptType;  // (Standard, Transfer, Walking) 
	private int nextDepature;

	public PTLink(IdImpl id, Node from, Node to, NetworkLayer network,double length, double freespeed, double capacity, double permlanes) {
		super(id, from, to, network, length, freespeed, capacity, permlanes);
	}

	public String getIdPTLine() {
		return idPTLine;
	}

	public void setIdPTLine(String idPTLine) {
		this.idPTLine = idPTLine;
	}

	public boolean isBusStop() {
		return isBusStop;
	}

	public void setBusStop(boolean isBusStop) {
		this.isBusStop = isBusStop;
	}

	public String getPtType() {
		return ptType;
	}

	public void setPtType(String ptType) {
		this.ptType = ptType;
	}

	public int getNextDepature() {
		return nextDepature;
	}

	public void setNextDepature(int nextDepature) {
		this.nextDepature = nextDepature;
	}
	
}// class
