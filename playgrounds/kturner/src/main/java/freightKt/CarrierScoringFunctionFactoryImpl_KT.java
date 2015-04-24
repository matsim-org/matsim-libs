package freightKt;

import java.io.File;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.scoring.FreightActivity;
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
    	
    	private double score;
        private Carrier carrier;

        VehicleFixCostScoring(Carrier carrier) {
            super();
            this.carrier = carrier;
        }

        WriteMoney fixCostWriter = new WriteMoney(new File(TEMP_DIR + "#FixCostsForScoringInfor.txt"), carrier);
        
        @Override
        public void finish() {
			fixCostWriter.writeCarrierLine(carrier);
//			fixCostWriter.writeAmountsToFile();
			fixCostWriter.writeTextLineToFile(System.getProperty("line.separator"));	
        }

        @Override
        public double getScore() {
        	calcFixCosts();  //Geht so, da Fixkosten nur einmal auftreten und somit vor der finalen Abfrage des Scores berechnet werden können
			return score;
        }

		private void calcFixCosts() {
			CarrierPlan selectedPlan = carrier.getSelectedPlan();
        	if(selectedPlan != null) {
        		for(ScheduledTour tour : selectedPlan.getScheduledTours()){
        			if(!tour.getTour().getTourElements().isEmpty()){
        				double fixCosts= tour.getVehicle().getVehicleType().getVehicleCostInformation().fix;
        				fixCostWriter.addAmountToWriter(fixCosts);
        				fixCostWriter.writeMoneyToFile(fixCosts);
        				score += (-1)*fixCosts;
        			}
        		}
        	}
		}  
        
        
      
   } //End class  FixCosts

    static class LegScoring implements SumScoringFunction.LegScoring {
    	
    	private static Logger log = Logger.getLogger(LegScoring.class);
    	
    	private double score = 0. ;
    	private Carrier carrier;

        LegScoring(Carrier carrier) {
             super();
             this.carrier = carrier;
         }
		
		WriteLegs legWriter = new WriteLegs(new File(TEMP_DIR + "#LegsForScoringInfor.txt"), carrier); //KT

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
    	
    } // End Class LegScoring
    
    static class ActivityScoring implements SumScoringFunction.ActivityScoring {

    	private static Logger log = Logger.getLogger(ActivityScoring.class);

		private double score = 0. ;
		private final double margUtlOfTime_s = 0.008 ;  //Wert aus Schröder/Liedtke 2014
    	
		private Carrier carrier;

		ActivityScoring(Carrier carrier) {
			super();
			this.carrier = carrier;
		}
    	
    	//Added Activity Writer to log the Activities
		WriteActivities activityWriter = new WriteActivities(new File(TEMP_DIR + "#ActivitiesForScoringInfor.txt")); //KT
		
		@Override
		public void finish() {
			activityWriter.writeCarrierLine(carrier);
			activityWriter.writeTextLineToFile("Activity Utils per s: " +"\t"+ margUtlOfTime_s +"\t"+ "Activity Utils per h: " +"\t"+ margUtlOfTime_s*3600);
			activityWriter.writeActsToFile();
			activityWriter.writeTextLineToFile(System.getProperty("line.separator"));
		}

		@Override
		public double getScore() {
			return this.score;
		}

		@Override
		public void handleFirstActivity(Activity act) {
			activityWriter.addFirstActToWriter(act); 
//			handleActivity(act);
			//Am Start geschieht nichts; ggf kann man hier noch Bewertung für Beladung des Fzgs einfügen, KT 14.04.15
			
		}

		@Override
		public void handleActivity(Activity activity) {
			activityWriter.addActToWriter(activity); 

			// Entwurf von KN
			if (activity instanceof FreightActivity) {
				FreightActivity act = (FreightActivity) activity;
				// deduct score for the time spent at the facility:
				final double actStartTime = act.getStartTime();
				final double actEndTime = act.getEndTime();
				score -= (actEndTime - actStartTime) * this.margUtlOfTime_s ;

				//From KN: Penalty for missing TimeWindow --> (KT) Überarbeiten, überlegen ob und wie es bewertet wird.
				//KT: zu früh (Waiting-Costs) wird genauso bewertet (Lohnkosten des Fahrers) -> 0.008 EUR/s
				final double windowStartTime = act.getTimeWindow().getStart();
//				final double windowEndTime = act.getTimeWindow().getEnd();   //aktuell nicht verwendet, KT 14.04.15

				final double penalty = this.margUtlOfTime_s; // per second!
				if ( actStartTime < windowStartTime ) {
					score -= penalty * ( windowStartTime - actStartTime ) ;
					// mobsim could let them wait ... but this is also not implemented for regular activities. kai, nov'13
					//aktuell warten Sie die Zeit "vor dem Tor" ab und beginnen Service dann pünktlich mit der Öffnung.
				}
//				if ( windowEndTime < actEndTime ) { //Doppelbewertung der Zeit (weitere Strafe) zunächst raus, da bei Schroeder/Lietdke nicht vorgesehen. KT, 14.04.15
//					score -= penalty * ( actEndTime - windowEndTime ) ;
//				}
				// (note: provide penalties that work with a gradient to help the evol algo. kai, nov'13)

			} else {
				log.warn("Carrier activities which are not FreightActivities are not scored here") ;
			}
		}

		@Override
		public void handleLastActivity(Activity act) {
			activityWriter.addLastActToWriter(act); 			
			handleActivity(act);
			// no penalty for everything that is after the last act (people don't work)		
		}
    	
    }  //End Class ActivityScoring
    
    static class MoneyScoring implements SumScoringFunction.MoneyScoring {

    	private double score = 0.;
    	private Carrier carrier; 
    	
    	MoneyScoring (Carrier carrier){
    		super();
    		this.carrier = carrier;
    	}
    	
     	WriteMoney moneyWriter = new WriteMoney(new File(TEMP_DIR + "#MoneyForScoringInfor.txt"), carrier); //KT
     	
		@Override
		public void finish() {	
			moneyWriter.writeCarrierLine(carrier);
//			moneyWriter.writeAmountsToFile();
			moneyWriter.writeTextLineToFile("finish aufgerufen");
			moneyWriter.writeTextLineToFile(System.getProperty("line.separator"));
		}

		@Override
		public double getScore() {
			moneyWriter.writeTextLineToFile("get Score aufgerufen");
			return this.score;
		}

		@Override
		public void addMoney(double amount) {
			moneyWriter.writeTextLineToFile("add-Money aufgerufen");
			moneyWriter.writeMoneyToFile(amount);
//			moneyWriter.addAmountToWriter(amount); 
			score += (-1)* amount;			
		}
    	
    } // End class MoneyScoring
    
	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Scenario scenario;
	private static String TEMP_DIR;
	
    public CarrierScoringFunctionFactoryImpl_KT(Scenario scenario, String tempDir) {
        super();
        CarrierScoringFunctionFactoryImpl_KT.scenario = scenario;
        CarrierScoringFunctionFactoryImpl_KT.TEMP_DIR = tempDir;
    }
}