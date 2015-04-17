package freightKt;

import java.io.File;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;

/**
 * Defines example carrier scoring function (factory).
 *
 * @author Kt, based on stefan
 * from sschroeder: package org.matsim.contrib.freight.usecases.chessboard;
 * 
 * TODO:  MoneyScoring, ActivityScoring anlegen 
 * 
 */
public class CarrierScoringFunctionFactoryImpl_KT implements CarrierScoringFunctionFactory{

    static class VehicleFixCostScoring implements SumScoringFunction.BasicScoring {

        private Carrier carrier;

        public VehicleFixCostScoring(Carrier carrier) {
            super();
            this.carrier = carrier;
        }

        @Override
        public void finish() {

        }

        @Override
        public double getScore() {
        	
        	double fixCosts = 0. ;
        	
        	CarrierPlan selectedPlan = carrier.getSelectedPlan();
        	if(selectedPlan != null) {
        		for(ScheduledTour tour : selectedPlan.getScheduledTours()){
        			if(!tour.getTour().getTourElements().isEmpty()){
        				fixCosts += (-1)*tour.getVehicle().getVehicleType().getVehicleCostInformation().fix;
        			}
        		}
        	}
			return fixCosts;
        }  
      
   } //End class 

    static class LegScoring implements SumScoringFunction.LegScoring {
    	
    	private static Logger log = Logger.getLogger(LegScoring.class);
    	
    	private double score = 0. ;
    	 private Carrier carrier;

         public LegScoring(Carrier carrier) {
             super();
             this.carrier = carrier;
         }
		
		WriteLegs legWriter = new WriteLegs(new File(scenario.getConfig().controler().getOutputDirectory()+ "/#LegsForScoringInformation.txt"), carrier); //KT



		private double getTimeParameter(CarrierVehicle vehicle) {
            return vehicle.getVehicleType().getVehicleCostInformation().perTimeUnit;
        }

        private double getDistanceParameter(CarrierVehicle vehicle) {
            return vehicle.getVehicleType().getVehicleCostInformation().perDistanceUnit;
        }

        private CarrierVehicle getVehicle(Id<?> vehicleId) {
            for(CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
                if(cv.getVehicleId().equals(vehicleId)){
                    return cv;
                }
            }
            return null;
        }

		@Override
		public void finish() {
			legWriter.writeCarrierLine(carrier);
			legWriter.writeLegsToFile(carrier);
			legWriter.writeTextLineToFile(System.getProperty("line.separator"));	
		}

		@Override
		public double getScore() {
			return this.score;
		}
		
		@Override
		public void handleLeg(Leg leg) {
			if(leg.getRoute() instanceof NetworkRoute){
                NetworkRoute nRoute = (NetworkRoute) leg.getRoute();
                Id<?> vehicleId = nRoute.getVehicleId();
                CarrierVehicle vehicle = getVehicle(vehicleId);
                if(vehicle == null) throw new IllegalStateException("vehicle with id " + vehicleId + " is missing");
                
                //Berechnung der TravelDistance (aus: org.matsim.population.algorithms.PersonPrepareForSim kopiert.) KT 08.01.15
				//Jedoch werden der Start- und Ziellink nicht mit einbezogen (weil nicht Teil der Route, sondern extra aufgeführt...!
                Double dist = null;
				if (leg.getRoute() instanceof NetworkRoute){
					dist = RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), scenario.getNetwork()); //Route selbst (ohne Start und Endlink
					dist += scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()).getLength();	//StartLink
					dist += scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()).getLength(); //EndLink
				}
				if (dist != null){
					leg.getRoute().setDistance(dist);
				}
				legWriter.addLegToWriter(leg);

                double distanceCosts = dist*getDistanceParameter(vehicle);
                assert distanceCosts >= 0.0 : "distanceCosts must be positive";
                score += (-1) * distanceCosts;
                
                double timeCosts = leg.getTravelTime()*getTimeParameter(vehicle);
                assert timeCosts >= 0.0 : "timeCosts must be positive";
                score += (-1) * timeCosts;
                
                leg.setMode(vehicle.getVehicleType().getId().toString());		//KT: 28.03.2015 Zuweisung des VehicleTxpes als Mode -> Sinnvoll? Zuminderst Besser als "car".
                
                legWriter.writeLegToFile(leg);
                legWriter.writeTextLineToFile("LegTimeCosts per s: \t"+ getTimeParameter(vehicle)+ "\t LegTimeCosts: "+ timeCosts  +  "\t LegDistanceCosts per m: "+ getDistanceParameter(vehicle) + "\t LegDistanceCosts: " + distanceCosts );
            } else {
				log.warn("Route not scored in LegScoring") ;
			}
			
		}
    	
    }
    
    
	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Scenario scenario;
	
    public CarrierScoringFunctionFactoryImpl_KT(Scenario scenario) {
        super();
        CarrierScoringFunctionFactoryImpl_KT.scenario = scenario;
    }
}