package freightKt;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.scoring.FreightActivity;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.Vehicle;

/**
 * Defines example carrier scoring function (factory).
 *
 * @author Kt, based on stefan
 * from sschroeder: package org.matsim.contrib.freight.usecases.chessboard ;
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
		private List<ScheduledTour> correctedTours = new ArrayList<ScheduledTour>(); //TODO: Einbauen

		ActivityScoring(Carrier carrier) {
			super();
			this.carrier = carrier;
		}
    	
    	//Added Activity Writer to log the Activities
		WriteActivities activityWriter = new WriteActivities(new File(TEMP_DIR + "#ActivitiesForScoringInfor.txt")); //KT
		WriteActivitiesInclScore activityWriterInclScore = new WriteActivitiesInclScore(new File(TEMP_DIR + "#ActivitiesForScoringInforInclScore.txt")); //KT
		
		@Override
		public void finish() {
			activityWriter.writeCarrierLine(carrier);
			activityWriter.writeTextLineToFile("Activity Utils per s: " +"\t"+ margUtlOfTime_s +"\t"+ "Activity Utils per h: " +"\t"+ margUtlOfTime_s*3600);
			activityWriter.writeActsToFile();
			activityWriter.writeTextLineToFile(System.getProperty("line.separator"));
			
			activityWriterInclScore.writeCarrierLine(carrier);
			activityWriterInclScore.writeTextLineToFile("Activity Utils per s: " +"\t"+ margUtlOfTime_s +"\t"+ "Activity Utils per h: " +"\t"+ margUtlOfTime_s*3600);
			activityWriterInclScore.writeActsToFile();
			activityWriterInclScore.writeTextLineToFile(System.getProperty("line.separator"));
		}

		@Override
		public double getScore() {
			return this.score;
		}

		@Override
		public void handleFirstActivity(Activity act) {
			activityWriter.addFirstActToWriter(act); 
			activityWriterInclScore.addFirstActToWriter(act, 0.0);
//			handleActivity(act);
			//Am Start geschieht nichts; ggf kann man hier noch Bewertung für Beladung des Fzgs einfügen, KT 14.04.15
			
		}

		@Override
		public void handleActivity(Activity activity) {
			double actCosts = 0;
			if (activity instanceof FreightActivity) {
				FreightActivity act = (FreightActivity) activity;
				
				actCosts = calcActCosts(act);		//costs for whole Activity, inkl waiting.
				//Identify the first serviceActivity on tour and correct costs 
				
				//TODO: Sicherstellen, dass man richtigen Servie erwischt: Bisher nur grobe Zuordnung via Location, Zeitfenster und Act-Type.
				//TODO: So umbauen, dass je ScheduledTour wirklich nur ein Element berücksichtigt wird. -> bisher nimmt er durch die Konstruktion jedes Element, da immer neue Abfrage :(
				boolean isfirstAct = isFirstServiceAct(act);
				if (isfirstAct){
					actCosts -= correctFirstService(act);  //Ziehe die zuviel berechneten Kosten ab. 
				}
				
				score += (-1) * actCosts;
				activityWriter.addActToWriter(activity);
				activityWriterInclScore.addActToWriter(activity, actCosts);
			} else {
				log.warn("Carrier activities which are not FreightActivities are not scored here: " + activity.toString()) ;
			}			
		}

		private boolean isFirstServiceAct(FreightActivity act) {
			boolean isfirstAct = false;
			
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours() ){
				if (!correctedTours.contains(tour)){
					for (TourElement te : tour.getTour().getTourElements()) {
						if (te instanceof  ServiceActivity){
							ServiceActivity sa = (ServiceActivity) te;
							if (sa.getLocation() == act.getLinkId()
									&& sa.getTimeWindow() == act.getTimeWindow()
									&& sa.getActivityType() == act.getType()){
								isfirstAct = true;
								correctedTours.add(tour);
							}
						}
					}
				}
			}
			return isfirstAct;
		}

		//Costs für Zeit von Begin bis Ende der Aktivität (enthält aktuell jun '15 auch Wartezeit bis Service beginnt)
		private double calcActCosts(FreightActivity act) {
				// deduct score for the time spent at the facility:
				final double actStartTime = act.getStartTime();
				final double actEndTime = act.getEndTime();
				return (actEndTime - actStartTime) * this.margUtlOfTime_s ;
		}
		
		//Korrigiert den Score bei der ersten Service-Aktivität (Wartezeit, da bereits zu Beginn der Depotöffnung losgefahren)
		//indem diese Zeit wieder mit einem positiven Wert gegengerechnet wird
		private double correctFirstService(FreightActivity act){
			final double actStartTime = act.getStartTime();
			final double windowStartTime = act.getTimeWindow().getStart();
				if ( actStartTime < windowStartTime ) {	//Fahrzeug vor Öffnungszeit angekommen.
					return ( windowStartTime - actStartTime ) * this.margUtlOfTime_s ;
				}
				else {
					return 0.0;
			} 
		}

		@Override
		public void handleLastActivity(Activity act) {
			activityWriter.addLastActToWriter(act); 	
			activityWriterInclScore.addLastActToWriter(act, null);
			handleActivity(act);
			// no penalty for everything that is after the last act (people don't work)		
		}
		
		
    	
    }  //End Class ActivityScoring
    
    //TODO: Sollte TollScoring übernehmen, funktioniert jedoch nicht, da keine amounts generiert werden.
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
    
    //Alternatives TollScoring von Schroeder {@see org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImp.TollScoring}
    static class TollScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ArbitraryEventScoring {

        private double score = 0.;
        private Carrier carrier;
        private Network network;

        private VehicleTypeDependentRoadPricingCalculator roadPricing;

        public TollScoring(Carrier carrier, Network network, VehicleTypeDependentRoadPricingCalculator roadPricing) {
            this.carrier = carrier;
            this.roadPricing = roadPricing;
            this.network = network;
        }

        @Override
        public void handleEvent(Event event) {
            if(event instanceof LinkEnterEvent){
                CarrierVehicle carrierVehicle = getVehicle(((LinkEnterEvent) event).getVehicleId());
                if(carrierVehicle == null) throw new IllegalStateException("carrier vehicle missing");
                double toll = roadPricing.getTollAmount(carrierVehicle.getVehicleType().getId(),network.getLinks().get(((LinkEnterEvent) event).getLinkId()),event.getTime());
                if(toll > 0.) System.out.println("bing: vehicle " + carrierVehicle.getVehicleId() + " paid toll " + toll + "");
                score += (-1) * toll;
            }
        }

        private CarrierVehicle getVehicle(Id<Vehicle> vehicleId) {
            for(CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles()){
                if(v.getVehicleId().equals(vehicleId)){
                    return v;
                }
            }
            return null;
        }

        @Override
        public void finish() {

        }

        @Override
        public double getScore() {
            return score;
        }
    }
    
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