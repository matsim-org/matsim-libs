package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.interfaces.ZoneDataExchangeInterface;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.controler.Controler;
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
 * - taking disutilites directly from MATSim (controler.createTravelCostCalculator()), this 
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
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityControlerListenerV3.class);
	private final AccessibilityCalculator delegate = new AccessibilityCalculator();
	private UrbanSimZoneCSVWriterV2 urbanSimZoneCSVWriterV2;
	

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////
	
	public ZoneBasedAccessibilityControlerListenerV3(ActivityFacilitiesImpl measuringPoints,
			   ActivityFacilitiesImpl opportunities,
			   String matsim4opusTempDirectory,
			   Scenario scenario) {
		this(measuringPoints, opportunities, null, matsim4opusTempDirectory, scenario);
	}
	
	public ZoneBasedAccessibilityControlerListenerV3(ActivityFacilitiesImpl measuringPoints,
												   ActivityFacilitiesImpl opportunities,
												   PtMatrix ptMatrix,
												   String matsim4opusTempDirectory,
												   Scenario scenario) {
		
		log.info("Initializing ZoneBasedAccessibilityControlerListenerV3 ...");
		
		assert(measuringPoints != null);
		delegate.setMeasuringPoints(measuringPoints);
		assert(matsim4opusTempDirectory != null);
		delegate.setPtMatrix(ptMatrix); // this could be zero of no input files for pseudo pt are given ...
		assert(scenario != null);

		// writing accessibility measures continuously into "zone.csv"-file. Naming of this 
		// files is given by the UrbanSim convention importing a csv file into a identically named 
		// data set table. THIS PRODUCES URBANSIM INPUT
		urbanSimZoneCSVWriterV2 = new UrbanSimZoneCSVWriterV2(matsim4opusTempDirectory);
		delegate.addZoneDataExchangeListener(urbanSimZoneCSVWriterV2);

		delegate.initAccessibilityParameters(scenario.getConfig());

		// aggregating facilities to their nearest node on the road network
		delegate.aggregateOpportunities(opportunities, scenario.getNetwork());
		// yyyy ignores the "capacities" of the facilities. kai, mar'14
		
		
		log.info(".. done initializing ZoneBasedAccessibilityControlerListenerV3");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		delegate.initDefaultContributionCalculators( event.getControler() );
		
		// make sure that that at least one tranport mode is selected
		boolean problem = true ;
		for ( Boolean bool : delegate.getIsComputingMode().values() ) {
			if ( bool == true ) {
				problem = false ;
				break ;
			}
		}
		
		if( problem ) {
			log.error("No transport mode for accessibility calculation is activated! For this reason no accessibilities can be calculated!");
			log.info("Please activate at least one transport mode by using the corresponding method when initializing the accessibility listener to fix this problem:");
			log.info("- useFreeSpeedGrid()");
			log.info("- useCarGrid()");
			log.info("- useBikeGrid()");
			log.info("- useWalkGrid()");
			log.info("- usePtGrid()");
			return;
		}
		
		
		// get the controller and scenario
		Controler controler = event.getControler();
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			log.info(delegate.getMeasuringPoints().getFacilities().values().size() + " measurement points are now processing ...");
			delegate.computeAccessibilities(controler.getScenario());
			
			// finalizing/closing csv file containing accessibility measures
			String matsimOutputDirectory = event.getControler().getScenario().getConfig().controler().getOutputDirectory();
			urbanSimZoneCSVWriterV2.close(matsimOutputDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setComputingAccessibilityForMode(Modes4Accessibility mode, boolean val) {
		delegate.setComputingAccessibilityForMode(mode, val);
	}

	public void addZoneDataExchangeListener(ZoneDataExchangeInterface l) {
		delegate.addZoneDataExchangeListener(l);
	}

}
