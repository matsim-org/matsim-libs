package org.matsim.contrib.freight.mobsim;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.ActivityScoring;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;

@Ignore
public class TimeScoringFunctionFactoryForTests implements CarrierScoringFunctionFactory{

	 static class DriverLegScoring implements BasicScoring, LegScoring{
			
			private double score = 0.0;

			private final Network network;
			
			private final Carrier carrier;
			
			private Set<CarrierVehicle> employedVehicles;
			
			private Leg currentLeg = null;
			
			private double currentLegStartTime;
			
			public DriverLegScoring(Carrier carrier, Network network) {
				super();
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
						score += (-1)*getFixEmploymentCost(vehicle);
					}
					double distance = 0.0;
					double toll = 0.0;
					if(currentLeg.getRoute() instanceof NetworkRoute){
						distance += network.getLinks().get(currentLeg.getRoute().getStartLinkId()).getLength();
						for(Id linkId : ((NetworkRoute) currentLeg.getRoute()).getLinkIds()){
							distance += network.getLinks().get(linkId).getLength();
							toll += getToll(linkId, vehicle, null);
						}
						distance += network.getLinks().get(currentLeg.getRoute().getEndLinkId()).getLength();
						toll += getToll(currentLeg.getRoute().getEndLinkId(), vehicle, null);
					}
					score += (-1)*(time-currentLegStartTime)*getTimeParameter(vehicle,null);
					score += (-1)*distance*getDistanceParameter(vehicle,null);
					score += (-1)*toll;
				}
				
			}
			
			private double getFixEmploymentCost(CarrierVehicle vehicle) {
				return 0.0;
			}

			private double getToll(Id linkId, CarrierVehicle vehicle, Person driver) {
				return 0;
			}

			private double getDistanceParameter(CarrierVehicle vehicle, Person driver) {
				return 0.0;
			}

			private double getTimeParameter(CarrierVehicle vehicle, Person driver) {
				return 1.0;
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
	
	 static class DriverActScoring implements BasicScoring, ActivityScoring{

		 boolean firstEnd = true;
		 
		 double startTime;
		 
		 double startTimeOfEnd; 
		 
		 double timeAtActivities = 0.0;
		 
		 double amountPerHour = 20.0;
		 
		 double startCurrentAct;
		 
		 double time_at_activities = 0.0;
		 
		@Override
		public void startActivity(double time, Activity act) {
			if(!act.getType().equals(FreightConstants.END)){
				System.out.println("act_start="+ Time.writeTime(time)+" act="+act);
				startCurrentAct = time;				
			}
		}

		@Override
		public void endActivity(double time, Activity act) {
			if(!act.getType().equals("start")){
				System.out.println("act_end="+Time.writeTime(time)+" act="+act);
				time_at_activities += time - startCurrentAct;
				System.out.println("time_at_activities="+time_at_activities);
			}		
		}

		@Override
		public void finish() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public double getScore() {
			return Math.round((-1)*(time_at_activities));
		}

		@Override
		public void reset() {
			startTime = 0.0;
			startTimeOfEnd = 0.0;
			time_at_activities = 0.0;
			firstEnd = true;
		}
		 
	 }
	 
	static class NumberOfToursAward implements BasicScoring{

		private Carrier carrier;
		
		public NumberOfToursAward(Carrier carrier) {
			super();
			this.carrier = carrier;
		}

		@Override
		public void finish() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public double getScore() {
			if(carrier.getSelectedPlan().getScheduledTours().size() > 1){
				return 10000.0;
			}
			return 0;
		}

		@Override
		public void reset() {
			// TODO Auto-generated method stub
			
		}
		
	}
	 
	@Inject private Network network;
	
	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		ScoringFunctionAccumulator sf = new ScoringFunctionAccumulator();
		DriverLegScoring driverLegScoring = new DriverLegScoring(carrier, network);
//		sf.addScoringFunction(driverLegScoring);
//		sf.addScoringFunction(new NumberOfToursAward(carrier));
		sf.addScoringFunction(new DriverActScoring());
		return sf;
	}

}
