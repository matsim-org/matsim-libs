package parking;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;

public class ParkingSearchModule extends AbstractModule {
		public static final String CAR_PARKING_SEARCH_ACT = "car parkingSearch";
		private final ZonalLinkParkingInfo zoneToLinks;

		public ParkingSearchModule(ZonalLinkParkingInfo zoneToLinks) {
			this.zoneToLinks = zoneToLinks;
		}

		@Override
		public void install() {
	
	        getConfig().plansCalcRoute().setInsertingAccessEgressWalk(true);
	        PlanCalcScoreConfigGroup.ActivityParams carParking = new PlanCalcScoreConfigGroup.ActivityParams(CAR_PARKING_SEARCH_ACT);
	        carParking.setScoringThisActivityAtAll(false);
	        getConfig().planCalcScore().addActivityParams(carParking);
		    bind(ZonalLinkParkingInfo.class).toInstance(zoneToLinks);
		    addRoutingModuleBinding(TransportMode.car).toProvider(new ParkingSearchRoutingModuleProvider(TransportMode.car));
		}
	}