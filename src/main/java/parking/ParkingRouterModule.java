package parking;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.functions.ScoringParameters;

import parking.analysis.ParkingAnalysisListener;
import parking.analysis.ParkingOccupancyEventHandler;
import parking.capacityCalculation.LinkLengthBasedCapacityCalculator;
import parking.capacityCalculation.LinkParkingCapacityCalculator;
import parking.capacityCalculation.UseParkingCapacityFromNetwork;

public class ParkingRouterModule extends AbstractModule {
		public static final String CAR_PARKING_SEARCH_ACT = "car parkingSearch";

		@Override
		public void install() {
			
			ParkingRouterConfigGroup prc = ParkingRouterConfigGroup.get(getConfig());
			switch (prc.getCapacityCalculationMethod()) {
				case lengthbased:
					bind(LinkParkingCapacityCalculator.class).to(LinkLengthBasedCapacityCalculator.class).asEagerSingleton();
					break;
				case useFromNetwork:
					bind(LinkParkingCapacityCalculator.class).to(UseParkingCapacityFromNetwork.class).asEagerSingleton();
					break;
				default:
					throw new RuntimeException("invalid parking calculation method");
					
			}
			
	        getConfig().plansCalcRoute().setInsertingAccessEgressWalk(true);
	        PlanCalcScoreConfigGroup.ActivityParams carParking = new PlanCalcScoreConfigGroup.ActivityParams(CAR_PARKING_SEARCH_ACT);
	        carParking.setScoringThisActivityAtAll(false);
	        
	        for (ScoringParameterSet s : getConfig().planCalcScore().getScoringParametersPerSubpopulation().values()){
	        	     	s.addActivityParams(carParking);
	        }
		    bind(ZonalLinkParkingInfo.class).asEagerSingleton();;
		    addRoutingModuleBinding(TransportMode.car).toProvider(new ParkingRouterRoutingModuleProvider(TransportMode.car));
		    bind(ParkingOccupancyEventHandler.class).asEagerSingleton();
		    addControlerListenerBinding().to(ParkingAnalysisListener.class).asEagerSingleton();
		}
	}