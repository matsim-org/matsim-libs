package playground.sergioo.ptsim2013.pt;

import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.PassengerAccessEgress;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleType;


public class BoardAlightVehicleTransitStopHandler implements TransitStopHandler {

	//Constants
	private final static Logger log = Logger.getLogger(BoardAlightVehicleTransitStopHandler.class);
	private static final double openDoorsDuration = 1.0;
	private static final double closeDoorsDuration = 1.0;
	private static final double NON_UNIFORM_DOOR_OPERATION = 1.0;
	private static final double ACC_DEC_DELAY = 8.0;
	
	//Attributes
	private TransitStopFacility lastHandledStop = null;
	
	//Methods
	@Override
	public double handleTransitStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers, List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle) {
		if(((TransitVehicle)vehicle).getVehicle().getType().getDoorOperationMode() == VehicleType.DoorOperationMode.parallel){			
			return handleParallelStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);			
		} else if (((TransitVehicle)vehicle).getVehicle().getType().getDoorOperationMode() == VehicleType.DoorOperationMode.serial){
			return handleSerialStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);
		} else {
			log.info("Unimplemented door operation mode " + ((TransitVehicle)vehicle).getVehicle().getType().getDoorOperationMode() + " set. Using parralel mode as default.");
			return handleParallelStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);
		}
		
	}
	
	private double handleParallelStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers, List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle){
		int cntEgress = leavingPassengers.size();
		int cntAccess = enteringPassengers.size();
		double stopTime = 0;
		if ((cntAccess > 0) || (cntEgress > 0)) {
			stopTime = getStopTimeParallel(leavingPassengers.size(), enteringPassengers.size(), vehicle);
			if (this.lastHandledStop != stop)
				stopTime += openDoorsDuration+closeDoorsDuration+ACC_DEC_DELAY;
			for (PTPassengerAgent passenger : leavingPassengers)
				handler.handlePassengerLeaving(passenger, vehicle, stop.getLinkId(), now);
			for (PTPassengerAgent passenger : enteringPassengers)
				handler.handlePassengerEntering(passenger, vehicle, stop.getId(), now);
		}
		lastHandledStop = stop;
		return stopTime;
	}
	private double getStopTimeParallel(double leaving, double entering, MobsimVehicle vehicle) {
		double beta1 = ((TransitVehicle)vehicle).getVehicle().getType().getAccessTime();
		double beta2 = ((TransitVehicle)vehicle).getVehicle().getType().getEgressTime();
		double mean = Math.max(beta1*entering, beta2*leaving);
		if(mean==0)
			return 0;
		double std = (2.2+0.12*mean);
		try {
			double r = MatsimRandom.getRandom().nextDouble();
			return Math.max(new NormalDistribution(mean,std).inverseCumulativeProbability(r), 0.5*mean);
		} catch (NotStrictlyPositiveException e) {
			e.printStackTrace();
		}
		return 0;
	}
	private double handleSerialStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers, List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle){
		int cntEgress = leavingPassengers.size();
		int cntAccess = enteringPassengers.size();
		double stopTime = 0;
		if ((cntAccess > 0) || (cntEgress > 0)) {
			stopTime = getStopTimeSerial(leavingPassengers.size(), enteringPassengers.size(), vehicle);
			for (PTPassengerAgent passenger : leavingPassengers)
				handler.handlePassengerLeaving(passenger, vehicle, stop.getLinkId(), now);
			for (PTPassengerAgent passenger : enteringPassengers)
				handler.handlePassengerEntering(passenger, vehicle, stop.getId(), now);
		}
		return stopTime+(stopTime==0?0:(openDoorsDuration+closeDoorsDuration+ACC_DEC_DELAY+NON_UNIFORM_DOOR_OPERATION));
	}	
	private double getStopTimeSerial(double leaving, double entering, MobsimVehicle vehicle) {
		double beta1 = ((TransitVehicle)vehicle).getVehicle().getType().getAccessTime();
		double beta2 = ((TransitVehicle)vehicle).getVehicle().getType().getEgressTime();
		return beta1*entering+beta2*leaving;
	}

	

}
