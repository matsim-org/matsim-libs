package org.matsim.contrib.freight.vrp;

public class RouterConfig {
	
	private static String EARLIEST_START = "earliestStart";
	
	private static String LATEST_START = "latestStart";
	
	private static String SINGLEDEPOT_DELIVERY = "singleDepotDelivery";
	
	private String scheduleTourStart = EARLIEST_START;
	
	private int transportTimeSlice = 24*3600;
	
	private double transportCostPerMeter = 0.4/1000.0; //in meter
	
	private double transportCostPerSecond = 30.0/3600.0; //in seconds
	
	private double fixCostPerActiveVehicle = 0.0;
	
	private int nOfWarmupIterations = 50;
	
	private int nOfIterations = 200;
	
	private String vrpProblemType = SINGLEDEPOT_DELIVERY;

	public String getVrpProblemType() {
		return vrpProblemType;
	}

	public double getFixCostPerActiveVehicle() {
		return fixCostPerActiveVehicle;
	}

	public void setFixCostPerActiveVehicle(double fixCostPerActiveVehicle) {
		this.fixCostPerActiveVehicle = fixCostPerActiveVehicle;
	}

	public int getTransportTimeSlice() {
		return transportTimeSlice;
	}

	public void setTransportTimeBinSize(int transportTimeBinSize) {
		this.transportTimeSlice = transportTimeBinSize;
	}

	public String getTourStart() {
		return scheduleTourStart;
	}

//	public void setTourStart(String scheduleTourStart) {
//		this.scheduleTourStart = scheduleTourStart;
//	}

	public double getTransportCostPerMeter() {
		return transportCostPerMeter;
	}

	public void setTransportCostPerMeter(double transportCostPerMeter) {
		this.transportCostPerMeter = transportCostPerMeter;
	}

	public double getTransportCostPerSecond() {
		return transportCostPerSecond;
	}

	public void setTransportCostPerSecond(double transportCostPerSecond) {
		this.transportCostPerSecond = transportCostPerSecond;
	}

	public int getWarmupIterations() {
		return nOfWarmupIterations;
	}

	public void setWarmupIterations(int warmupIterations) {
		this.nOfWarmupIterations = warmupIterations;
	}

	public int getIterations() {
		return nOfIterations;
	}

	public void setIterations(int iterations) {
		this.nOfIterations = iterations;
	}
	
	
}
