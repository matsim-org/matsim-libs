package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.facilities.ActivityFacilitiesImpl;

/**
 *  improvements feb'12
 *  - distance between zone centroid and nearest node on road network is considered in the accessibility computation
 *  as walk time of the euclidian distance between both (centroid and nearest node). This walk time is added as an offset 
 *  to each measured travel times
 *  - using walk travel times instead of travel distances. This is because of the betas that are utils/time unit. The walk time
 *  corresponds to distances since this is also linear.
 * 
 * This works for UrbanSim Zone and Parcel Applications !!! (march'12)
 * 
 *  improvements april'12
 *  - accessibility calculation uses configurable betas (coming from UrbanSim) for car/walk travel times, -distances and -costs
 *  
 * improvements / changes july'12 
 * - fixed error: used pre-factor (1/beta scale) in deterrence function instead of beta scale (fixed now!)
 * 
 * todo (sep'12):
 * - set external costs to opportunities within the same zone ...
 * 
 * improvements jan'13
 * - added pt for accessibility calculation
 * 
 * improvements april'13
 * - congested car modes uses TravelDisutility from MATSim
 * - taking disutilites directly from MATSim (services.createTravelCostCalculator()), this
 * also activates road pricing ...
 * 
 * improvements june'13
 * - removed zones as argument to ZoneBasedAccessibilityControlerListenerV3
 * - providing opportunity facilities (e.g. workplaces)
 * - reduced dependencies to MATSim4UrbanSim contrib: replaced ZoneLayer<Id> and Zone by standard MATSim ActivityFacilities
 * 
 * @author thomas
 *
 */
public final class ZoneBasedAccessibilityControlerListenerV3 implements ShutdownListener{
	// yyyy The zone based and the grid based accessibility controler listeners should be combined, since the coordinate points on which this is
	// computed are now external anyways.  There is probably one or the other grid dependency in the grid based accessibility controler
	// listener, but we wanted to remove that anyways.  kai, dec'16
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityControlerListenerV3.class);

	private final AccessibilityCalculator accessibilityCalculator;
	private final ActivityFacilitiesImpl opportunities;
	
	public ZoneBasedAccessibilityControlerListenerV3(AccessibilityCalculator accessibilityCalculator,
													 ActivityFacilitiesImpl opportunities,
													 String matsim4opusTempDirectory,
													 Scenario scenario) {
		
		this.accessibilityCalculator = accessibilityCalculator;
		log.info("Initializing ZoneBasedAccessibilityControlerListenerV3 ...");
		
		assert(matsim4opusTempDirectory != null);
		assert(scenario != null);

		this.opportunities = opportunities;
		// yyyy ignores the "capacities" of the facilities. kai, mar'14
		
		
		log.info(".. done initializing ZoneBasedAccessibilityControlerListenerV3");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
//		if(delegate.getIsComputingMode().isEmpty()) {
		if ( accessibilityCalculator.getModes().isEmpty() ) {
			log.error("No transport mode for accessibility calculation is activated! For this reason no accessibilities can be calculated!");
			return;
		}
		
		
		// get the controller and scenario
		MatsimServices controler = event.getServices();
		log.info("Computing and writing zone based accessibility measures ..." );

		AccessibilityConfigGroup accessibilityConfig = ConfigUtils.addOrGetModule( controler.getScenario().getConfig(), AccessibilityConfigGroup.class);


		accessibilityCalculator.computeAccessibilities(accessibilityConfig.getTimeOfDay(), opportunities);
	}

	public void addFacilityDataExchangeListener(FacilityDataExchangeInterface l) {
		accessibilityCalculator.addFacilityDataExchangeListener(l);
	}

}
