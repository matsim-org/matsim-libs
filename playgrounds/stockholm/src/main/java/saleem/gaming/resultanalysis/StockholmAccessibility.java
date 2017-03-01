package saleem.gaming.resultanalysis;

/**
 * A class to calculate accessibility measure for gaming scenarios.
 * The idea was later dropped since, the accessibility functionality was available for Car traffic only.
 * 
 * 
 * @author Mohammad Saleem
 */
public class StockholmAccessibility {
//	private static final Logger log = Logger.getLogger(RunAccessibilityExample.class);
//	public static void main(String[] args) {
//		String path = "./ihop2/matsim-input/config.xml";
//		Config config = ConfigUtils.loadConfig(path);
//		final Scenario scenario = ScenarioUtils.loadScenario(config);
//		 
//		double samplesize = config.qsim().getStorageCapFactor();
//		
//		// Changing vehicle and road capacity according to sample size
//		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
//		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
//		
//			
//		final AccessibilityConfigGroup acg = new AccessibilityConfigGroup();
//		acg.setCellSizeCellBasedAccessibility(1000);
//		acg.setTimeOfDay(28800.0);
//		config.addModule( acg);
//		
//		ActivityFacilities opportunities = scenario.getActivityFacilities();
//		for ( Link link : scenario.getNetwork().getLinks().values() ) {
//			Id<ActivityFacility> id = Id.create(link.getId(), ActivityFacility.class);
//			Coord coord = link.getCoord();
//			ActivityFacility facility = opportunities.getFactory().createActivityFacility(id, coord);
//			{
//				ActivityOption option = new ActivityOptionImpl("h") ;
//				facility.addActivityOption(option);
//		}
//				opportunities.addActivityFacility(facility);
//		}
//		run(scenario);
//	
//	}
//	public static void run(final Scenario scenario) {
//		
//		final List<String> activityTypes = new ArrayList<String>() ;
//		final ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;
//		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
//			for ( ActivityOption option : fac.getActivityOptions().values() ) {
//				// figure out all activity types
//				if ( !activityTypes.contains(option.getType()) ) {
//					activityTypes.add( option.getType() ) ;
//				}
//				// figure out where the homes are
//				if ( option.getType().equals("h") ) {
//					homes.addActivityFacility(fac);
//				}
//			}
//		}
//		
//		log.warn( "found the following activity types: " + activityTypes ); 
//		
//		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some other algorithms,
//		// the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
//		
//		final Controler controler = new Controler(scenario) ;
//		controler.getConfig().controler().setOverwriteFileSetting(
//				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
//
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				for ( final String actType : activityTypes ) {
//
//					final ActivityFacilities opportunities = FacilitiesUtils.createActivityFacilities() ;
//					for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
//						for ( ActivityOption option : fac.getActivityOptions().values() ) {
//							if ( option.getType().equals(actType) ) {
//								opportunities.addActivityFacility(fac);
//							}
//						}
//					}
//					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
//
//						@Inject Map<String, TravelTime> travelTimes;
//						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
//
//						@Override
//						public ControlerListener get() {
//							Double cellSizeForCellBasedAccessibility = Double.parseDouble(scenario.getConfig().getModule("accessibility").getValue("cellSizeForCellBasedAccessibility"));
//							Config config = scenario.getConfig();
//							if (cellSizeForCellBasedAccessibility <= 0) {
//								throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
//							}
//							BoundingBox bb = BoundingBox.createBoundingBox(scenario.getNetwork());
//							AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario);
////							accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSizeForCellBasedAccessibility));
//							accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(675631.77274, 6580814.47442, 686631.77274, 6690814.47442, cellSizeForCellBasedAccessibility));
//							
//							
//							GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, opportunities, null, config, scenario, travelTimes, travelDisutilityFactories,bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSizeForCellBasedAccessibility);
//
//							
//							// define the modes that will be considered
//							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
//							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
//							
//							// add additional facility data to an additional column in the output
//							// here, an additional population density column is used
//							listener.addAdditionalFacilityData(homes) ;
//							listener.writeToSubdirectoryWithName(actType);
//							return listener;
//						}
//					});
//				}
//			}
//		});
//		Network network = scenario.getNetwork();
//		TransitSchedule schedule = scenario.getTransitSchedule();
//		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
////		controler.addControlerListener(new FareControlListener());
//		controler.run();
//	}

}
