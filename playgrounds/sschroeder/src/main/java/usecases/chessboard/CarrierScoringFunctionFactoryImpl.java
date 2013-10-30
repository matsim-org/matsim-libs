package usecases.chessboard;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.mobsim.FreightActivity;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ActivityScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;

/**
 * Defines example carrier scoring function (factory).
 * 
 * <p>Just saw that there are some Deprecations. Needs to be adapted.
 * 
 * @author stefan
 *
 */
public class CarrierScoringFunctionFactoryImpl implements CarrierScoringFunctionFactory{

	/**
	 * 
	 * Example activity scoring that penalizes missed time-windows with 1.0 per second.
	 * 
	 * @author stefan
	 *
	 */
	static class DriversActivityScoring implements BasicScoring, ActivityScoring{

		private double score;
		
		private Double actStartTime;
		
		private double timeParameter = 0.008; 
		
		private double missedTimeWindowPenalty = 1.0;
		
		public DriversActivityScoring() {
			super();
		}

		@Override
		public void startActivity(double time, Activity act) {
			actStartTime = time;
			if(act instanceof FreightActivity){
				TimeWindow tw = ((FreightActivity) act).getTimeWindow();
				if(time > tw.getEnd()){
					double penalty_score = (-1)*(time - tw.getEnd())*missedTimeWindowPenalty;
					assert penalty_score <= 0.0 : "penalty score must be negative";
					score += penalty_score;
				}
			}
		}

		@Override
		public void endActivity(double time, Activity act) {
			if(actStartTime!=null){
				double actTimeCosts = (time-actStartTime)*timeParameter;
				assert actTimeCosts >= 0.0 : "actTimeCosts must be positive";
				score += actTimeCosts*(-1);
			}
		}

		@Override
		public void finish() {
			
			
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void reset() {
			score = 0.0;
			actStartTime = null;
		}
		
	}
	
	/**
	 * Example leg scoring.
	 * 
	 * @author stefan
	 *
	 */
	static class DriversLegScoring implements BasicScoring, LegScoring {

		private double score = 0.0;

		private final Network network;
		
		private final Carrier carrier;
		
		private Set<CarrierVehicle> employedVehicles;
		
		private Leg currentLeg = null;
		
		private double currentLegStartTime;
		
		public DriversLegScoring(Carrier carrier, Network network) {
			super();
			this.network = network;
			this.carrier = carrier;
			employedVehicles = new HashSet<CarrierVehicle>();
		}

		
		@Override
		public void finish() {
			for(CarrierVehicle v : employedVehicles){
				score += (-1)*v.getVehicleType().getVehicleCostInformation().fix;
			}
		}


		@Override
		public double getScore() {
			return score;
		}


		@Override
		public void reset() {
			score = 0.0;
			employedVehicles.clear();
		}


		@Override
		public void startLeg(double time, Leg leg) {
			currentLeg = leg;
			currentLegStartTime = time; 
		}


		@Override
		public void endLeg(double time) {
			if(currentLeg.getRoute() instanceof NetworkRoute){
				NetworkRoute nRoute = (NetworkRoute) currentLeg.getRoute();
				Id vehicleId = nRoute.getVehicleId();
				CarrierVehicle vehicle = getVehicle(vehicleId);
				assert vehicle != null : "cannot find vehicle with id=" + vehicleId;
				if(!employedVehicles.contains(vehicle)){
					employedVehicles.add(vehicle);
				}
				double distance = 0.0;
				if(currentLeg.getRoute() instanceof NetworkRoute){
					distance += network.getLinks().get(currentLeg.getRoute().getStartLinkId()).getLength();
					for(Id linkId : ((NetworkRoute) currentLeg.getRoute()).getLinkIds()){
						distance += network.getLinks().get(linkId).getLength();
					}
					distance += network.getLinks().get(currentLeg.getRoute().getEndLinkId()).getLength();
				}
				
				double distanceCosts = distance*getDistanceParameter(vehicle,null);
				assert distanceCosts >= 0.0 : "distanceCosts must be positive";
				score += (-1) * distanceCosts;
				double timeCosts = (time-currentLegStartTime)*getTimeParameter(vehicle);
				assert timeCosts >= 0.0 : "timeCosts must be positive";
				score += (-1) * timeCosts;
			}
			
		}
		
		private double getTimeParameter(CarrierVehicle vehicle) {
			return vehicle.getVehicleType().getVehicleCostInformation().perTimeUnit;
		}


		private double getDistanceParameter(CarrierVehicle vehicle, Person driver) {
			return vehicle.getVehicleType().getVehicleCostInformation().perDistanceUnit;
		}

	
		private CarrierVehicle getVehicle(Id vehicleId) {
			for(CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
				if(cv.getVehicleId().equals(vehicleId)){
					return cv;
				}
			}
			return null;
		}
		
	}
	
	private Network network;
	
	public CarrierScoringFunctionFactoryImpl(Network network) {
		super();
		this.network = network;
	}


	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		ScoringFunctionAccumulator sf = new ScoringFunctionAccumulator();
		DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
		DriversActivityScoring actScoring = new DriversActivityScoring();
		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(actScoring);
		return sf;
	}
	
	

}
