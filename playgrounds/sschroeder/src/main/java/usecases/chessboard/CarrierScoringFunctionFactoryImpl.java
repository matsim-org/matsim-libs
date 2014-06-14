package usecases.chessboard;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.scoring.FreightActivity;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;

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
	static class DriversActivityScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {

		private double score;
		
		private double timeParameter = 0.008; 
		
		private double missedTimeWindowPenalty = 0.008;
		
		public DriversActivityScoring() {
			super();
		}

		@Override
		public void finish() {
			
			
		}

		@Override
		public double getScore() {
			return score;
		}

        @Override
        public void handleFirstActivity(Activity act) {
            handleActivity(act);
        }

        @Override
        public void handleActivity(Activity act) {
            if(act instanceof FreightActivity) {
                double actStartTime = act.getStartTime();
                TimeWindow tw = ((FreightActivity) act).getTimeWindow();
                if(actStartTime > tw.getEnd()){
                    double penalty_score = (-1)*(actStartTime - tw.getEnd())*missedTimeWindowPenalty;
                    assert penalty_score <= 0.0 : "penalty score must be negative";
                    score += penalty_score;
                }
                double actTimeCosts = (act.getEndTime()-actStartTime)*timeParameter;
                assert actTimeCosts >= 0.0 : "actTimeCosts must be positive";
                score += actTimeCosts*(-1);
            }
        }

        @Override
        public void handleLastActivity(Activity act) {
            handleActivity(act);
        }

    }
	
	/**
	 * Example leg scoring.
	 * 
	 * @author stefan
	 *
	 */
	static class DriversLegScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.LegScoring {

		private double score = 0.0;

		private final Network network;
		
		private final Carrier carrier;
		
		private Set<CarrierVehicle> employedVehicles;
		
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
		
		private double getTimeParameter(CarrierVehicle vehicle) {
			return vehicle.getVehicleType().getVehicleCostInformation().perTimeUnit;
		}


		private double getDistanceParameter(CarrierVehicle vehicle) {
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

        @Override
        public void handleLeg(Leg leg) {
            if(leg.getRoute() instanceof NetworkRoute){
                NetworkRoute nRoute = (NetworkRoute) leg.getRoute();
                Id vehicleId = nRoute.getVehicleId();
                CarrierVehicle vehicle = getVehicle(vehicleId);
                assert vehicle != null : "cannot find vehicle with id=" + vehicleId;
                if(!employedVehicles.contains(vehicle)){
                    employedVehicles.add(vehicle);
                }
                double distance = 0.0;
                if(leg.getRoute() instanceof NetworkRoute){
                    distance += network.getLinks().get(leg.getRoute().getStartLinkId()).getLength();
                    for(Id linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()){
                        distance += network.getLinks().get(linkId).getLength();
                    }
                    distance += network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();
                }

                double distanceCosts = distance*getDistanceParameter(vehicle);
                assert distanceCosts >= 0.0 : "distanceCosts must be positive";
                score += (-1) * distanceCosts;
                double timeCosts = leg.getTravelTime()*getTimeParameter(vehicle);
                assert timeCosts >= 0.0 : "timeCosts must be positive";
                score += (-1) * timeCosts;
            }
        }

    }
	
	private Network network;
	
	public CarrierScoringFunctionFactoryImpl(Network network) {
		super();
		this.network = network;
	}


	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
		DriversActivityScoring actScoring = new DriversActivityScoring();
		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(actScoring);
		return sf;
	}
	
	

}
