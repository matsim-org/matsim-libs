package playground.andreas.fixedHeadway.ana;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;

public class VehicleDelayAnaEventHandler implements PTEventHandler, VehicleDepartsAtFacilityEventHandler {
	
	private final static Logger log = Logger.getLogger(VehicleDelayAnaEventHandler.class);


	VehicleCountBox vcb = null;

	HashMap<Id, String> debug = new HashMap<Id, String>();


	private String vehId;
	private double startTime;
	private double stopTime;
	
	public VehicleDelayAnaEventHandler(String vehId, double startTime, double stopTime){
		this.vehId = vehId;
		this.startTime = startTime;
		this.stopTime = stopTime;
	}
	
	public VehicleDelayAnaEventHandler(String vehId, String startTime, String stopTime){
		this(vehId, Time.parseTime(startTime), Time.parseTime(stopTime));
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if(event.getVehicleId().toString().equalsIgnoreCase(this.vehId)){
			if(event.getTime() >= this.startTime && event.getTime() < this.stopTime){
				if(this.vcb == null){
					this.vcb = new VehicleCountBox(event.getVehicleId());
				}
				
				this.vcb.delay += Math.abs(event.getDelay());
				this.vcb.stopsServed++;				
			}
		}
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub		
	}
	
	class VehicleCountBox{
		Id vehID;
		double delay;
		int stopsServed;
		
		public VehicleCountBox(Id vehicleId){
			this.vehID = vehicleId;
		}
		
		public double getDelay(){
			return this.delay;
		}
		
		public int getStopsServed(){
			return this.stopsServed;
		}
		
		public double getAverageDelay(){
			return this.delay / this.stopsServed;
		}
	}

	@Override
	public String printResults() {		
		log.info("Veh " + this.vehId + " from " + Time.writeTime(this.startTime) + " to " + Time.writeTime(this.stopTime) + " served " + this.vcb.getStopsServed() 
				+ " stops with a total delay of " + ((int) this.vcb.getDelay()) + "s or " + this.vcb.getAverageDelay() + "s per stop in average.");
		
		return this.vehId + ", " + Time.writeTime(this.startTime) + ", " + Time.writeTime(this.stopTime) + ", " + this.vcb.getStopsServed() + ", " 
		+ ((int) this.vcb.getDelay()) + ", " + this.vcb.getAverageDelay();
		
	}
	
}
