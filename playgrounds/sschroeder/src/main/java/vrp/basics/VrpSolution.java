package vrp.basics;

import java.util.Collection;

public class VrpSolution {
	
	public final Collection<Tour> tours;
	
	private Double transportTime = null;
	
	private Double transportDistance = null;
	
	private Double transportCosts = null;

	public VrpSolution(Collection<Tour> tours) {
		super();
		this.tours = tours;
	}

	public Double getTransportTime() {
		return transportTime;
	}

	public void setTransportTime(double transportTime) {
		this.transportTime = transportTime;
	}

	public Double getTransportDistance() {
		return transportDistance;
	}

	public void setTransportDistance(double transportDistance) {
		this.transportDistance = transportDistance;
	}

	public Double getTransportCosts() {
		return transportCosts;
	}

	public void setTransportCosts(double transportCosts) {
		this.transportCosts = transportCosts;
	}
	
	
}
