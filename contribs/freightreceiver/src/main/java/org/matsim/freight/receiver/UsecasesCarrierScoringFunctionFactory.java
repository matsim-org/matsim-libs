package org.matsim.freight.receiver;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.FreightActivity;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

/*
 * This is a sample carrier scoring function factory implementation (CarrierScoringFunctionImpl.class) developed by sschroeder,
 * I just changed the penalty cost and time parameter, etc.
 */
public class UsecasesCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {

	private static Logger log = LogManager.getLogger(UsecasesCarrierScoringFunctionFactory.class);

	private final Network network;

	public UsecasesCarrierScoringFunctionFactory( Network network ) {
		this.network = network;
	}

	   static class DriversActivityScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {

	        private double score;

//	        private double timeParameter = 0.0889;
	        private double timeParameter = 1.0000;
	        // yyyyyy I have set the time parameter to a relatively high value.

	        private double missedTimeWindowPenalty = 0.01667;
//	        private double missedTimeWindowPenalty = 1.000;

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
	                double actStartTime = act.getStartTime().seconds();
	                TimeWindow tw = ((FreightActivity) act).getTimeWindow();
	                if(actStartTime > tw.getEnd()){
	                    double penalty_score = (-1)*(actStartTime - tw.getEnd())*missedTimeWindowPenalty;
	                    assert penalty_score <= 0.0 : "penalty score must be negative";
	                    score += penalty_score;
	                }
	                double actTimeCosts = (act.getEndTime().seconds()-actStartTime)*timeParameter;
	                assert actTimeCosts >= 0.0 : "actTimeCosts must be positive";
	                score += actTimeCosts*(-1);
	            }
	        }

	        @Override
	        public void handleLastActivity(Activity act) {
	            handleActivity(act);
	        }

	    }

	    static class VehicleEmploymentScoring implements SumScoringFunction.BasicScoring {

	        private Carrier carrier;

	        public VehicleEmploymentScoring(Carrier carrier) {
	            this.carrier = carrier;
	        }

	        @Override
	        public void finish() {

	        }

	        @Override
	        public double getScore() {
	            double score = 0.;
	            CarrierPlan selectedPlan = carrier.getSelectedPlan();
	            if(selectedPlan == null) return 0.;
	            for(ScheduledTour tour : selectedPlan.getScheduledTours()){
	                if(!tour.getTour().getTourElements().isEmpty()){
						score += (-1)* ((Vehicle) tour.getVehicle()).getType().getCostInformation().getFix();
	                }
	            }
	            return score;
	        }

	    }

	    static class DriversLegScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.LegScoring {

	        private double score = 0.0;

	        private final Network network;

	        private final Carrier carrier;

	        private Set<CarrierVehicle> employedVehicles;

	        public DriversLegScoring(Carrier carrier, Network network) {
	            this.network = network;
	            this.carrier = carrier;
	            employedVehicles = new HashSet<CarrierVehicle>();
	        }


	        @Override
	        public void finish() {

	        }


	        @Override
	        public double getScore() {
	            return score;
	        }

	        private double getTimeParameter(CarrierVehicle vehicle) {
				return vehicle.getType().getCostInformation().getPerTimeUnit();
	        }


	        private double getDistanceParameter(CarrierVehicle vehicle) {
				return vehicle.getType().getCostInformation().getPerDistanceUnit();
	        }


	        private CarrierVehicle getVehicle(Id<Vehicle> vehicleId) {
				if(carrier.getCarrierCapabilities().getCarrierVehicles().containsKey(vehicleId)){
					return carrier.getCarrierCapabilities().getCarrierVehicles().get(vehicleId);
				}
				log.error("Vehicle with Id does not exists", new IllegalStateException("vehicle with id " + vehicleId + " is missing"));
				return null;
	        }

	        @Override
	        public void handleLeg(Leg leg) {
	            if(leg.getRoute() instanceof NetworkRoute){
	                NetworkRoute nRoute = (NetworkRoute) leg.getRoute();
	                Id<Vehicle> vehicleId = nRoute.getVehicleId();
	                CarrierVehicle vehicle = getVehicle(vehicleId);
	                if(vehicle == null) throw new IllegalStateException("vehicle with id " + vehicleId + " is missing");
	                if(!employedVehicles.contains(vehicle)){
	                    employedVehicles.add(vehicle);
	                }
	                double distance = 0.0;

	                if(leg.getRoute() instanceof NetworkRoute){
	                    Link startLink = network.getLinks().get(leg.getRoute().getStartLinkId());
	                    distance += startLink.getLength();
	                    for(Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()){
	                        distance += network.getLinks().get(linkId).getLength();

	                    }
	                    distance += network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();

	                }

	                double distanceCosts = distance*getDistanceParameter(vehicle);
	                assert distanceCosts >= 0.0 : "distanceCosts must be positive";
	                score += (-1) * distanceCosts;
	                double timeCosts = leg.getTravelTime().seconds()*getTimeParameter(vehicle);
	                assert timeCosts >= 0.0 : "timeCosts must be positive";
	                score += (-1) * timeCosts;

	            }
	        }

	    }

	    @Override
	    public ScoringFunction createScoringFunction(Carrier carrier) {
	        SumScoringFunction sf = new SumScoringFunction();
	        DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
	        VehicleEmploymentScoring vehicleEmployment = new VehicleEmploymentScoring(carrier);
			DriversActivityScoring actScoring = new DriversActivityScoring();
	        sf.addScoringFunction(driverLegScoring);
	        sf.addScoringFunction(vehicleEmployment);
			sf.addScoringFunction(actScoring);
	        return sf;
	    }
}

