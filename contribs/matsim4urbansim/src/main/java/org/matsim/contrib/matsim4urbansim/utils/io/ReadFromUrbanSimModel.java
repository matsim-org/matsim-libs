package org.matsim.contrib.matsim4urbansim.utils.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matsim4urbansim.config.M4UConfigUtils;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.utils.helperobjects.SpatialReferenceObject;
import org.matsim.contrib.matsim4urbansim.utils.io.misc.RandomLocationDistributor;
import org.matsim.contrib.matsim4urbansim.utils.io.writer.AnalysisPopulationCSVWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

/**
 * improvements aug'12
 * - In AggregateObject2NearestNode: the euclidian distance between an opportunity and its nearest 
 *   node on the network is measured and stored in the jobClusterArray. This is used to determine 
 *   the total costs cij in the accessibility measure
 * 
 * 
 * @author nagel
 * @author thomas
 */
public class ReadFromUrbanSimModel {

	// Logger
	private static final Logger log = Logger.getLogger(ReadFromUrbanSimModel.class);
	// year of current urbansim run
	private final int year;
	
	private PopulationCounter cnt;
	private String shapefile = null;
	private double radius 	 = 0.;
	private UrbanSimParameterConfigModuleV3 module;
	
	/**
	 * constructor
	 * @param year of current run
	 * @param config TODO
	 */
	public ReadFromUrbanSimModel ( final int year, Config config ) {
		this.year 	= year;
		this.cnt 	= new PopulationCounter();
		this.shapefile = null;
		this.radius	= 0.;
		this.module =  M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(config);
	}

	public ReadFromUrbanSimModel(final int year, final String shapefile,
			final double radius, Config config ) {
		this.year	= year;
		this.cnt	= new PopulationCounter();
		this.shapefile = shapefile;
		this.radius	= radius;
		this.module =  M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(config);
	}

	/**
	 * reads the parcel data set table from urbansim and creates ActivityFacilities
	 *
	 * @param parcels ActivityFacilitiesImpl
	 * @param zones ActivityFacilitiesImpl
	 */
	public void readFacilitiesZones(final ActivityFacilitiesImpl zones) {
		// (these are simply defined as those entities that have x/y coordinates in UrbanSim)
		String filename = module.getMATSim4OpusTemp() + InternalConstants.URBANSIM_ZONE_DATASET_TABLE + this.year + InternalConstants.FILE_TYPE_TAB;
		
		log.info( "Starting to read urbansim zones table from " + filename );

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			// read header of facilities table
			String line = reader.readLine();

			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, InternalConstants.TAB_SEPERATOR );
			final int indexXCoodinate 	= idxFromKey.get( InternalConstants.X_COORDINATE );
			final int indexYCoodinate 	= idxFromKey.get( InternalConstants.Y_COORDINATE );
			final int indexZoneID 		= idxFromKey.get( InternalConstants.ZONE_ID );

			// temporary variables, needed to construct parcels and zones
			Id<ActivityFacility> zone_ID;
			Coord coord;
			String[] parts;

			while ( (line = reader.readLine()) != null ) {
				parts = line.split(InternalConstants.TAB_SEPERATOR);

				// get zone ID, UrbanSim sometimes writes IDs as floats!
				long zoneIdAsLong = (long) Double.parseDouble( parts[ indexZoneID ] );
				zone_ID = Id.create( zoneIdAsLong , ActivityFacility.class) ;

				// get the coordinates of that parcel
				coord = new Coord(Double.parseDouble(parts[indexXCoodinate]), Double.parseDouble(parts[indexYCoodinate]));

				// create a facility (within the parcels) object at this coordinate with the correspondent parcel ID
				ActivityFacilityImpl facility = zones.createAndAddFacility(zone_ID,coord);
				facility.setDesc( "urbansim location" ) ;

				// set custom attributes, these are needed to compute zone2zone trips
				Map<String, Object> customFacilityAttributes = facility.getCustomAttributes();
				customFacilityAttributes.put(InternalConstants.ZONE_ID, zone_ID);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit( -1 );
		} catch (IOException e) {
			e.printStackTrace();
			System.exit( -1 );
		}
		
		log.info("Done reading urbansim parcels. Found " + zones.getFacilities().size() + " zones.");
	}
	
	/**
	 * reads the parcel data set table from UrbanSim and creates ActivityFacilities
	 *
	 * @param parcels ActivityFacilitiesImpl
	 * @param zones ActivityFacilitiesImpl
	 */
	public void readFacilitiesParcel(final ActivityFacilitiesImpl parcels, final ActivityFacilitiesImpl zones) {
		// (these are simply defined as those entities that have x/y coordinates in UrbanSim)
		String filename = module.getMATSim4OpusTemp() + InternalConstants.URBANSIM_PARCEL_DATASET_TABLE + this.year + InternalConstants.FILE_TYPE_TAB;
		
		log.info( "Starting to read urbansim parcels table from " + filename );

		// temporary data structure in order to get coordinates for zones:
		// Map<Id,Id> zoneFromParcel = new HashMap<Id,Id>();
		Map<Id<ActivityFacility>, PseudoZone> pseudoZones = new ConcurrentHashMap<>();

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			// read header of facilities table
			String line = reader.readLine();

			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, InternalConstants.TAB_SEPERATOR );
			final int indexParcelID 	= idxFromKey.get( InternalConstants.PARCEL_ID );
			final int indexXCoodinate 	= idxFromKey.get( InternalConstants.X_COORDINATE_SP );
			final int indexYCoodinate 	= idxFromKey.get( InternalConstants.Y_COORDINATE_SP );
			final int indexZoneID 		= idxFromKey.get( InternalConstants.ZONE_ID );

			// temporary variables, needed to construct parcels and zones
			Id<ActivityFacility> parcel_ID;
			Coord coord;
			Id<ActivityFacility> zone_ID;
			PseudoZone pseudoZone;
			String[] parts;

			//
			while ( (line = reader.readLine()) != null ) {
				parts = line.split(InternalConstants.TAB_SEPERATOR);

				// urbansim sometimes writes IDs as floats!
				long parcelIdAsLong = (long) Double.parseDouble( parts[ indexParcelID ] );
				parcel_ID = Id.create( parcelIdAsLong, ActivityFacility.class ) ;

				// get the coordinates of that parcel
				coord = new Coord(Double.parseDouble(parts[indexXCoodinate]), Double.parseDouble(parts[indexYCoodinate]));

				// create a facility (within the parcels) object at this coordinate with the correspondent parcel ID
				ActivityFacilityImpl facility = parcels.createAndAddFacility(parcel_ID,coord);
				facility.setDesc( "urbansim location" ) ;
				
				// get zone ID
				long zoneIdAsLong = (long) Double.parseDouble( parts[ indexZoneID ] );
				zone_ID = Id.create( zoneIdAsLong, ActivityFacility.class );

				// set custom attributes, these are needed to compute zone2zone trips
				Map<String, Object> customFacilityAttributes = facility.getCustomAttributes();
				customFacilityAttributes.put(InternalConstants.ZONE_ID, zone_ID);
				customFacilityAttributes.put(InternalConstants.PARCEL_ID, parcel_ID);

				// the pseudoZones (HashMap) is a temporary data structure to create zones.
				// this intermediate step is needed to make sure that every zone id just exists once.
				// in the case that there are more than one data sets (parcels) with the same zone ID they are merged.
				// that means the coordinates are added and the count increases.
				// later (in "construct zones") this temporary data structure is needed to get the the average value of the
				// coordinates for each zone ID.
				pseudoZone = pseudoZones.get(zone_ID);
				if ( pseudoZone==null ) {
					pseudoZone = new PseudoZone() ;
					pseudoZones.put(zone_ID, pseudoZone);
				}
				pseudoZone.sumXCoordinate += coord.getX();
				pseudoZone.sumYCoordinate += coord.getY() ;
				pseudoZone.count ++ ;
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit( -1 );
		} catch (IOException e) {
			e.printStackTrace();
			System.exit( -1 );
		}
		
		log.info("Done reading urbansim parcels. Found " + parcels.getFacilities().size() + " parcels.");
		// create urbansim zones from the intermediate pseudo zones
		constructZones ( zones, pseudoZones);
	}

	/**
	 * this method creates the final zones from the merged parcels (with the same zone ID)
	 * by computing the average value of the coordinates for each zone ID.
	 *
	 * @param zones ActivityFacilitiesImpl
	 * @param pseudoZones Map<Id,PseudoZone>
	 */
	void constructZones (final ActivityFacilitiesImpl zones, final Map<Id<ActivityFacility>,PseudoZone> pseudoZones  ) {

		log.info( "Starting to construct urbansim zones (for the impedance matrix)" ) ;

		Id<ActivityFacility> zone_ID;
		PseudoZone pz;
		Coord coord;

		// constructing the zones from the pseudoZones:
		for ( Entry<Id<ActivityFacility>,PseudoZone> entry : pseudoZones.entrySet() ) {
			zone_ID = entry.getKey();
			pz = entry.getValue();
			// compute the average center of a zone
			coord = new Coord(pz.sumXCoordinate / pz.count, pz.sumYCoordinate / pz.count);
			zones.createAndAddFacility(zone_ID, coord);
		}
		log.info( "Done with constructing urbansim zones. Constucted " + zones.getFacilities().size() + " zones.");
	}
	
	/**
	 * pseudo zone is a temporary data structure. it can contain the summation of x and y coordinates
	 * of different parcels with the same zone ID. the attribute "count" counts the number of parcels
	 * with the the same zone ID. its used as a denominator to get the average x and y coordinate of
	 * a zone.
	 *
	 * @author nagel
	 *
	 */
	class PseudoZone {

			protected double sumXCoordinate = 0.0;
			protected double sumYCoordinate = 0.0;
			protected long count = 0;	// denominator of x and y coordinate
	}
	

	/**
	 *
	 * @param oldPop
	 * @param zones
	 * @param network
	 * @param samplingRate
	 */
	public Population readPersonsZone(Population oldPop, final ActivityFacilitiesImpl zones, final Network network, final double samplingRate) {
		
		Population mergePop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation(); // will contain all persons from UrbanSim Persons table
		Population backupPop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation(); // will contain only new persons (id) that don't exist in warm start pop file
		cnt = new PopulationCounter();
		RandomLocationDistributor rld = new RandomLocationDistributor(this.shapefile, this.radius);
		
		boolean compensationFlag = false;
		ZoneLocations currentZoneLocations = null;

		String filename = module.getMATSim4OpusTemp() + InternalConstants.URBANSIM_PERSON_DATASET_TABLE + this.year + InternalConstants.FILE_TYPE_TAB;
		
		log.info( "Starting to read persons table from " + filename );

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			// get columns for home, work and person id
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, InternalConstants.TAB_SEPERATOR );
			final int indexZoneID_HOME 	   = idxFromKey.get( InternalConstants.ZONE_ID_HOME );
			final int indexZoneID_WORK 	   = idxFromKey.get( InternalConstants.ZONE_ID_WORK );
			final int indexPersonID		   = idxFromKey.get( InternalConstants.PERSON_ID );

			// We consider two cases:
			// (1) We have an old population.  Then we look for those people who have the same id.
			// (2) We do not have an old population.  Then we construct a new one.
			// In both cases we assume that the new population has the right size.

			while ( (line=reader.readLine()) != null ) {
				cnt.numberOfUrbanSimPersons++;
				String[] parts = line.split( InternalConstants.TAB_SEPERATOR );

				// create new person
				Id<Person> personId = Id.create( parts[ indexPersonID ], Person.class );

				if ( !( compensationFlag || MatsimRandom.getRandom().nextDouble() < samplingRate || personExistsInOldPopulation(oldPop, personId)) )
					continue;
				
				// see reason of this flag below
				compensationFlag = false;
				currentZoneLocations	 = new ZoneLocations();
				
				Person newPerson = PersonImpl.createPerson(personId);

				// get home location id
				Id<ActivityFacility> homeZoneId = Id.create( parts[ indexZoneID_HOME ], ActivityFacility.class );
				ActivityFacility homeLocation = zones.getFacilities().get( homeZoneId ); // the home location is the zone centroid
				currentZoneLocations.setHomeZoneIDAndZoneCoordinate(homeZoneId, homeLocation);
				if ( homeLocation==null ){
					log.warn( "homeLocation==null; personId: " + personId + " zoneId: " + homeZoneId + ' ' + this );
					continue;
				}
				// Coord homeCoord = homeLocation.getCoord(); // old version
				// randomize home location within zone
				Coord homeCoord = rld.getRandomLocation(homeZoneId, homeLocation.getCoord());
				if ( homeCoord==null ){
					log.warn( "homeCoord==null; personId: " + personId + " zoneId: " + homeZoneId + ' ' + this );
					continue;
				}

				// add home location to plan
				PlanImpl plan = PersonUtils.createAndAddPlan(newPerson, true);
				CreateHomeWorkHomePlan.makeHomePlan(plan, homeCoord, homeLocation) ;

				// determine employment status
				if ( parts[ indexZoneID_WORK ].equals("-1") )
					PersonUtils.setEmployed(newPerson, Boolean.FALSE);
				else {
					PersonUtils.setEmployed(newPerson, Boolean.TRUE);
					Id<ActivityFacility> workZoneId = Id.create( parts[ indexZoneID_WORK ], ActivityFacility.class );
					ActivityFacility jobLocation = zones.getFacilities().get( workZoneId );
					currentZoneLocations.setWorkZoneIDAndZoneCoordinate(workZoneId, jobLocation);
					if ( jobLocation == null ) {
						// show warning message only once
						if ( cnt.jobLocationIdNullCnt < 1 ) {
							cnt.jobLocationIdNullCnt++ ;
							log.warn( "jobLocationId==null, probably out of area. person_id: " + personId
									+ " work_zone_id: " + workZoneId + Gbl.ONLYONCE ) ;
						}
						compensationFlag = true ;
						// can't remember.  The way this reads to me is that this is set to true if a working person is encountered
						// that does not have a workplace.  Because it does not have a workplace, we discard it.  If this happens
						// with too many persons, we end up having much less than our target sample size.  With the seattle_parcel
						// this was a problem since this happened routinely with all persons having their workplaces outside the
						// (reduced) simulation area ... i.e., it happened A LOT.  So I guess that this flag compensates
						// for that situation.  kai, mar'11

						continue ;
					}
					// complete agent plan
					//Coord workCoord = jobLocation.getCoord(); // old version
					Coord workCoord = rld.getRandomLocation(workZoneId, jobLocation.getCoord());
					CreateHomeWorkHomePlan.completePlanToHwh(plan, workCoord, jobLocation);
				}

				// at this point, we have a full "new" person.  Now check against pre-existing population ...
				mergePopulation(oldPop, mergePop, newPerson, backupPop, network, cnt, false, currentZoneLocations, rld);
			}
			log.info("Done with merging population ...");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
		printBasicPopulationInfo(oldPop, mergePop, samplingRate, backupPop, cnt);
		Population newPop = fitNewPopulationToSamplingRate(oldPop, mergePop, samplingRate, backupPop, cnt);
		printBasicPopulationInfo(oldPop, newPop, samplingRate, backupPop, cnt);
		
		return newPop;
	}
	
	/**
	 *
	 * @param oldPop
	 * @param parcels
	 * @param network
	 * @param samplingRate
	 */
	public Population readPersonsParcel(Population oldPop, final ActivityFacilitiesImpl parcels, final Network network, final double samplingRate) {
		
		Population mergePop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation(); // will contain all persons from UrbanSim Persons table
		Population backupPop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation(); // will contain only new persons (id) that don't exist in warm start pop file
		cnt = new PopulationCounter();
		
		boolean compensationFlag = false;

		String filename = module.getMATSim4OpusTemp() + InternalConstants.URBANSIM_PERSON_DATASET_TABLE + this.year + InternalConstants.FILE_TYPE_TAB;
		
		log.info( "Starting to read persons table from " + filename );

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			// get columns for home, work and person id
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, InternalConstants.TAB_SEPERATOR );
			final int indexParcelID_HOME 	= idxFromKey.get( InternalConstants.PARCEL_ID_HOME );
			final int indexParcelID_WORK 	= idxFromKey.get( InternalConstants.PARCEL_ID_WORK );
			final int indexPersonID			= idxFromKey.get( InternalConstants.PERSON_ID );

			// We consider two cases:
			// (1) We have an old population.  Then we look for those people who have the same id.
			// (2) We do not have an old population.  Then we construct a new one.
			// In both cases we assume that the new population has the right size.

			while ( (line=reader.readLine()) != null ) {
				cnt.numberOfUrbanSimPersons++;
				String[] parts = line.split( InternalConstants.TAB_SEPERATOR );

				// create new person
				Id<Person> personId = Id.create( parts[ indexPersonID ], Person.class );

				if ( !( compensationFlag || MatsimRandom.getRandom().nextDouble() < samplingRate || personExistsInOldPopulation(oldPop, personId)) )
					continue ;
				
				// see reason of this flag below
				compensationFlag = false;
				
				Person newPerson = PersonImpl.createPerson(personId);

				// get home location id
				Id<ActivityFacility> homeParcelId = Id.create( parts[ indexParcelID_HOME ], ActivityFacility.class );
				ActivityFacility homeLocation = parcels.getFacilities().get( homeParcelId );
				if ( homeLocation==null ) {
					log.warn( "homeLocation==null; personId: " + personId + " parcelId: " + homeParcelId + ' ' + this ) ;
					continue ;
				}
				Coord homeCoord = homeLocation.getCoord() ;
				if ( homeCoord==null ) {
					log.warn( "homeCoord==null; personId: " + personId + " parcelId: " + homeParcelId + ' ' + this ) ;
					continue ;
				}

				// add home location to plan
				PlanImpl plan = PersonUtils.createAndAddPlan(newPerson, true);
				CreateHomeWorkHomePlan.makeHomePlan(plan, homeCoord, homeLocation) ;

				// determine employment status
				if ( parts[ indexParcelID_WORK ].equals("-1") )
					PersonUtils.setEmployed(newPerson, Boolean.FALSE);
				else {
					PersonUtils.setEmployed(newPerson, Boolean.TRUE);
					Id<ActivityFacility> workParcelId = Id.create( parts[ indexParcelID_WORK ], ActivityFacility.class ) ;
					ActivityFacility jobLocation = parcels.getFacilities().get( workParcelId ) ;
					if ( jobLocation == null ) {
						// show warning message only once
						if ( cnt.jobLocationIdNullCnt < 1 ) {
							cnt.jobLocationIdNullCnt++ ;
							log.warn( "jobLocationId==null, probably out of area. person_id: " + personId
									+ " workp_prcl_id: " + workParcelId + Gbl.ONLYONCE ) ;
						}
						compensationFlag = true ; // WHY?
						// can't remember.  The way this reads to me is that this is set to true if a working person is encountered
						// that does not have a workplace.  Because it does not have a workplace, we discard it.  If this happens
						// with too many persons, we end up having much less than our target sample size.  With the seattle_parcel
						// this was a problem since this happened routinely with all persons having their workplaces outside the
						// (reduced) simulation area ... i.e., it happened A LOT.  So I guess that this flag compensates
						// for that situation.  kai, mar'11

						continue ;
					}
					// complete agent plan
					Coord workCoord = jobLocation.getCoord() ;
					CreateHomeWorkHomePlan.completePlanToHwh(plan, workCoord, jobLocation) ;
				}

				// at this point, we have a full "new" person.  Now check against pre-existing population ...
				mergePopulation(oldPop, mergePop, newPerson, backupPop, network, cnt, true, null, null);
			}
			log.info("Done with merging population ...");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
		printBasicPopulationInfo(oldPop, mergePop, samplingRate, backupPop, cnt);
		Population newPop = fitNewPopulationToSamplingRate(oldPop, mergePop, samplingRate, backupPop, cnt);
		printBasicPopulationInfo(oldPop, newPop, samplingRate, backupPop, cnt);
		
		return newPop;
	}

	/**
	 * @param oldPop
	 * @param mergePop
	 * @param samplingRate
	 * @param backupPop
	 * @param cnt
	 */
	private Population fitNewPopulationToSamplingRate(final Population oldPop, Population mergePop, final double samplingRate, Population backupPop, PopulationCounter cnt) {
		
		// target population size
		long targetPopSize = Math.round(samplingRate*cnt.numberOfUrbanSimPersons);
		
		if(mergePop.getPersons().size() == targetPopSize){
			log.info("Size of new population (" + mergePop.getPersons().size() + ") fits the target population size with samplingRate*NumberUrbansimPersons (" + targetPopSize + ")." );
			printDetailedPopulationInfo(cnt);
		}
		// check if newPop contains too few people than our target population size (less than sampleRate*NUrbansimPersons). In this case add additional persons from backupPop
		else if( mergePop.getPersons().size() < targetPopSize && !backupPop.getPersons().isEmpty() ){
			log.info("Size of new population (" + mergePop.getPersons().size() + ") is smaller than samplingRate*NumberUrbansimPersons (" + targetPopSize + "). Adding persons, stored in bakPopSize ...");
			List<Person> backupPopList = new ArrayList<Person>( backupPop.getPersons().values() ); // Population data structure not needed!
			Collections.shuffle( backupPopList );	// pick random person
			
			for ( Person person : backupPopList ) {
				if ( mergePop.getPersons().size() >= samplingRate*cnt.numberOfUrbanSimPersons )
					break ;
				cnt.fromBackupCnt++;
				mergePop.addPerson( person ) ;
			}
			log.info(" ... done adding persons from backupPopulation!");
			
			printDetailedPopulationInfo(cnt);
		}
		// Check if newPop contain too many people than our target population size and remove persons from newPop if needed
		else if( mergePop.getPersons().size() > targetPopSize ){
			printDetailedPopulationInfo(cnt);
			log.info("Size of new population (" +  mergePop.getPersons().size() + ") is larger than samplingRate*NumberUrbansimPersons (" + targetPopSize + "). Removing persons from newPop ...");
			// Since no person can be removed from newPop a new, down sampled population is created ...
			Population downSampledPop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
			
			List<Person> newPopList = new ArrayList<Person>( mergePop.getPersons().values() ); // Population data structure not needed!
			Collections.shuffle( newPopList );	// pick random person
			
			for(int i = 0; i <  newPopList.size(); i++){
				if(downSampledPop.getPersons().size() >= targetPopSize)
					break;
				downSampledPop.addPerson( newPopList.get( i ) );
				
			}
			mergePop = downSampledPop;
			cnt.reset();
			cnt.populationMergeTotal = downSampledPop.getPersons().size();
			
			log.info(" ... done removing persons from newPop!");
		}
		
		return mergePop;
	}

	/**
	 * @param cnt
	 */
	private void printDetailedPopulationInfo(PopulationCounter cnt) {
		log.info("================================================================================");
		log.info("Population merge overview:");
		log.info("Total population = " + cnt.populationMergeTotal);
		log.info("Re-identified persons from old population = " + cnt.identifiedCnt);
		log.info("Composition of population:");
		log.info("		Not re-identified persons in old population (backup) = " + cnt.newPersonCnt + ". " + cnt.fromBackupCnt + " of them are taken into the new Population.");
		log.info("		Persons employment status changed = " + cnt.employmentChangedCnt);
		log.info("		Persons home location changed = " + cnt.homelocationChangedCnt);
		log.info("		Persons work location changed = " + cnt.worklocationChangedCnt);
		log.info("		Persons unemployed (considered as new) = " + cnt.unemployedCnt);
		log.info("================================================================================");
	}

	/**
	 * @param oldPop
	 * @param newPop
	 * @param samplingRate
	 * @param backupPop
	 * @param cnt
	 * @return
	 */
	private void printBasicPopulationInfo(final Population oldPop,
			final Population newPop, final double samplingRate, final Population backupPop,
			final PopulationCounter cnt) {
		// get size of old population
		int oldPopSize = oldPop==null ? 0 : oldPop.getPersons().size();

		log.info(" samplingRate: " + samplingRate + " oldPopSize: " + oldPopSize + " newPopSize: " + newPop.getPersons().size()
				+ " backupPopulation: " + backupPop.getPersons().size() + " NumberUrbansimPersons: " + cnt.numberOfUrbanSimPersons );
//		log.warn("why is bakPopSize not approx as large as samplingRate*NumberUrbansimPersons?" );
	}

	/**
	 * @param oldPop
	 * @param newPop
	 * @param newPerson
	 * @param backupPop
	 * @param network
	 * @param cnt
	 * @param currentZoneLocations
	 * @param rld TODO
	 */
	private void mergePopulation(final Population oldPop,
			final Population newPop, Person newPerson,
			Population backupPop, final Network network, 
			PopulationCounter cnt, boolean isParcel, 
			ZoneLocations currentZoneLocations, RandomLocationDistributor rld) {
		
		while ( true ) { // loop from which we can "break":

			Person oldPerson ;
			cnt.populationMergeTotal++;

			if ( oldPop==null ) { // no pre-existing population.  Accept:
				newPop.addPerson(newPerson);
				break;
			} else if ( (oldPerson=oldPop.getPersons().get( newPerson.getId() )) == null ) { // person not found in old population. Store temporarily in backup.
				backupPop.addPerson( newPerson );
				cnt.newPersonCnt++;
				break;
			} else if ( PersonUtils.isEmployed(oldPerson) != PersonUtils.isEmployed(newPerson) ) { // employment status changed. Accept new person:
				newPop.addPerson(newPerson);
				cnt.employmentChangedCnt++;
				break;
			}
			
			// get home location from old plans file and current UrbanSim data
			Activity oldHomeAct = ((PlanImpl) oldPerson.getSelectedPlan()).getFirstActivity();
			Activity newHomeAct = ((PlanImpl) newPerson.getSelectedPlan()).getFirstActivity();
			
			// for parcels check if activity location has changed. if true accept as new person
			if ( isParcel && actHasChanged ( oldHomeAct, newHomeAct, network ) ) { // for parcels
				newPop.addPerson(newPerson);
				cnt.homelocationChangedCnt++;
				break;
			}
			// for zones
			else if( !isParcel ){ // for zones
				
				Coord oldHomeCoord = oldHomeAct.getCoord();
				
				if(currentZoneLocations == null || rld == null){
					log.warn("No warm or hot start possible. All UrbanSim persons are handeld as new agents and thus obtain new plans!");
					newPop.addPerson(newPerson);
					cnt.homelocationChangedCnt++;
					break;
				}
				else if(this.shapefile != null){ // check home location via shape file
					Id<ActivityFacility> newHomeZoneID = currentZoneLocations.getHomeZoneID();
					if(!rld.coordinateInZone(newHomeZoneID, oldHomeCoord)){
						// home location changed
						newPop.addPerson(newPerson);
						cnt.homelocationChangedCnt++;
						break;
					}
				}
				else{							// check home location via radius
					Coord newZoneCoord = currentZoneLocations.getHomeZoneCoord();
					// testing if the previous home location lies within the 
					// specified radius located at the current zone centroid/coordinate
					if(!rld.coordinatesInRadius(oldHomeCoord, newZoneCoord)){
						// home location changed
						newPop.addPerson(newPerson);
						cnt.homelocationChangedCnt++;
						break;
					}
				}
			}

			// check if new person works
			if ( !PersonUtils.isEmployed(newPerson) ) { // person does not move; doesn't matter. fix this when other activities are considered
				newPop.addPerson(newPerson);
				cnt.unemployedCnt++;
				break ;
			}

			// check if work act has changed:
			ActivityImpl oldWorkAct = (ActivityImpl) oldPerson.getSelectedPlan().getPlanElements().get(2);
			ActivityImpl newWorkAct = (ActivityImpl) newPerson.getSelectedPlan().getPlanElements().get(2);
			
			// for parcels check if activity location has changed. if true accept as new person
			if ( isParcel && actHasChanged ( oldWorkAct, newWorkAct, network ) ) { // for parcels
				newPop.addPerson(newPerson);
				cnt.worklocationChangedCnt++;
				break ;
			}
			// for zones
			else if( !isParcel ){ // for zones
				
				Coord oldWorkCoord = oldWorkAct.getCoord();
				
				if(currentZoneLocations == null || rld == null){
					log.warn("No warm or hot start possible. All UrbanSim persons are handeld as new agents and thus obtain new plans!");
					newPop.addPerson(newPerson);
					cnt.worklocationChangedCnt++;
					break;
				}
				else if(this.shapefile != null){ // check work location via shape file
					Id<ActivityFacility> newWorkZoneID = currentZoneLocations.getWorkZoneID();
					if(!rld.coordinateInZone(newWorkZoneID, oldWorkCoord)){
						// work location changed
						newPop.addPerson(newPerson);
						cnt.worklocationChangedCnt++;
						break;
					}
				}
				else{							// check work location via radius
					Coord newZoneCoord = currentZoneLocations.getWorkZoneCoord();
					// testing if the previous work location lies within the 
					// specified radius located at the current zone centroid/coordinate
					if(!rld.coordinatesInRadius(oldWorkCoord, newZoneCoord)){
						// work location changed
						newPop.addPerson(newPerson);
						cnt.worklocationChangedCnt++;
						break;
					}
				}
			}

			// no "break" up to here, so new person does the same as the old person. Keep old person (including its
			// routes etc.)
			newPop.addPerson(oldPerson);
			cnt.identifiedCnt++;
			break ;
		}
	}

	/**
	 * reading jobs and creating activity facilities
	 * @param opportunities
	 * @param parcelsOrZones
	 * @param isParcel
	 */
	public void readJobs(ActivityFacilitiesImpl opportunities, final ActivityFacilitiesImpl parcelsOrZones, final boolean isParcel){
		
		String filename = module.getMATSim4OpusTemp() + InternalConstants.URBANSIM_JOB_DATASET_TABLE + this.year + InternalConstants.FILE_TYPE_TAB;
		
		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilityMap = parcelsOrZones.getFacilities();
		
		try{
			BufferedReader reader = IOUtils.getBufferedReader( filename );
			// reading header
			String line = reader.readLine();
			// get columns for job, parcel and zone id
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, InternalConstants.TAB );
			int indexJobID = idxFromKey.get( InternalConstants.JOB_ID );
			int indexParcelID = -1; // see below
			int indexZoneID = idxFromKey.get( InternalConstants.ZONE_ID_WORK );
			if(isParcel)
				indexParcelID = idxFromKey.get( InternalConstants.PARCEL_ID_WORK );
			
			while ( (line=reader.readLine()) != null ) {
				
				String[] parts = line.split( InternalConstants.TAB );
				Id<ActivityFacility> jobId = Id.create(parts[indexJobID], ActivityFacility.class);
				
				if(isParcel){
					
					Id<ActivityFacility> parcelId = Id.create(parts[indexParcelID], ActivityFacility.class);
					ActivityFacility parcel = facilityMap.get(parcelId);
					opportunities.createAndAddFacility(jobId, parcel.getCoord());
				}
				else{// zones
					
					Id zoneId = Id.create(parts[indexZoneID], ActivityFacility.class);
					ActivityFacility zone = facilityMap.get(zoneId);
					opportunities.createAndAddFacility(jobId, zone.getCoord());
				}
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
	}

//	private void createJobParcel(final JobCounter cnt,
//			final List<SpatialReferenceObject> jobSampleList,
//			final List<SpatialReferenceObject> backupList,
//			final Map<Id, ActivityFacility> facilityMap, 
//			final int indexJobID,
//			final int indexParcelID, 
//			final int indexZoneID,
//			final boolean isBackup, 
//			final String[] parts) {
//		
//		Id jobID;
//		Id parcelID;
//		Id zoneID;
//		Coord coord;
//		if( Integer.parseInt( parts[indexParcelID] ) >= 0 ){
//			
//			jobID = Id.create( parts[indexJobID] );
//			assert( jobID != null);
//			parcelID = Id.create( parts[indexParcelID] );
//			assert( parcelID != null );
//			zoneID = Id.create( parts[indexZoneID] );
//			assert( zoneID != null );
//			coord = facilityMap.get( parcelID ).getCoord();
//			assert( coord != null );
//			
//			if(isBackup){
//				backupList.add( new SpatialReferenceObject(jobID, parcelID, zoneID, coord) );
//				cnt.backupJobs++;
//			}
//			else
//				jobSampleList.add( new SpatialReferenceObject(jobID, parcelID, zoneID, coord) );
//		}
//		else
//			cnt.skippedJobs++;	// counting number of skipped jobs ...
//	}
//	
//	private void createJobZone(final JobCounter cnt,
//			final List<SpatialReferenceObject> jobSampleList,
//			final List<SpatialReferenceObject> backupList,
//			final Map<Id, ActivityFacility> facilityMap, 
//			final int indexJobID,
//			final int indexZoneID,
//			final boolean isBackup, 
//			final String[] parts) {
//		
//		Id jobID;
//		Id zoneID;
//		Coord coord;
//		if( Integer.parseInt( parts[indexZoneID] ) >= 0 ){
//		
//			jobID = Id.create( parts[indexJobID] );
//			assert( jobID != null);
//			zoneID = Id.create( parts[indexZoneID] );
//			assert( zoneID != null );
//			coord = facilityMap.get( zoneID ).getCoord();
//			assert( coord != null );
//			
//			if(isBackup){
//				backupList.add( new SpatialReferenceObject(jobID, Id.create(-1), zoneID, coord) );
//				cnt.backupJobs++;
//			}
//			else
//				jobSampleList.add( new SpatialReferenceObject(jobID, Id.create(-1), zoneID, coord) );
//		}
//		else
//			cnt.skippedJobs++;	// counting number of skipped jobs ...
//	}
//
//	/**
//	 * @param jobSample
//	 * @param cnt
//	 * @param jobObjectMap
//	 * @param backupList
//	 */
//	private void checkAndAdjustJobSampleSize(double jobSample, JobCounter cnt, List<SpatialReferenceObject> jobSampleList, List<SpatialReferenceObject> backupList) {
//		
//		cnt.allowedJobs = Math.round( jobSample*cnt.numberOfUrbanSimJobs );
//		
//		log.info(" samplingRate: " + jobSample + "; total number of jobs: " + cnt.numberOfUrbanSimJobs + "; number jobs should count: " + cnt.allowedJobs +
//				 "; actual number jobs: " + jobSampleList.size() + "; number of jobs in backupList: " +cnt.backupJobs + "; skipped jobs (without parcel id):" + cnt.skippedJobs);
//		
//		// if job sample size is too low add jobs from backup list
//		if(cnt.allowedJobs > jobSampleList.size()){
//			log.info("Size of actual added jobs (" + jobSampleList.size() + ") is smaller than samplingRate*NumberUrbansimJobs (" + cnt.allowedJobs + "). Adding jobs, stored in backupList ... ");
//			Collections.shuffle( backupList );
//			
//			for(SpatialReferenceObject jObject : backupList){
//				if(cnt.allowedJobs > jobSampleList.size()){
//					jobSampleList.add( jObject );
//					cnt.addedJobsFromBackup++;
//				}
//				else break;
//			}
//			log.info("... done!");
//		}
//		// otherwise if too many jobs were added remove jobs
//		else if(cnt.allowedJobs < jobSampleList.size()){
//			log.info("Actual size of jobs (" + jobSampleList.size() + ") is larger than samplingRate*NumberUrbansimJobs (" + cnt.allowedJobs + "). Removing jobs... ");
//			
//			Random randomGenerator = MatsimRandom.getRandom();
//			
//			// removes jobs randomly
//			while(cnt.allowedJobs < jobSampleList.size()){
//				
//				int index = randomGenerator.nextInt( jobSampleList.size() );
//				jobSampleList.remove( index );
//			}
//		}
//		
//		log.info(" samplingRate: " + jobSample + "; total number of jobs: " + cnt.numberOfUrbanSimJobs + "; number jobs should count: " + cnt.allowedJobs +
//				 "; actual number jobs: " + jobSampleList.size() + "; number of jobs in backupList: " +cnt.backupJobs + "; skipped jobs (without parcel id):" + cnt.skippedJobs +
//				 "; number of jobs added from backupList: " + cnt.addedJobsFromBackup);
//	}

	/**
	 * determine if a person already exists
	 * @param oldPop
	 * @param personId
	 * @return true if a person already exists in an old population
	 */
	private boolean personExistsInOldPopulation(Population oldPop, Id<Person> personId){

		if(oldPop == null)
			return false;
		else
			return oldPop.getPersons().get( personId ) != null;
	}

	/**
	 *
	 * @param oldAct
	 * @param newAct
	 * @param network
	 * @return
	 */
	private boolean actHasChanged ( final Activity oldAct, final Activity newAct, final Network network ) {
		
		if( !( (oldAct.getCoord().getX() == newAct.getCoord().getX()) && 
			    oldAct.getCoord().getY() == newAct.getCoord().getY() ) ){		
//		if ( !oldAct.getCoord().equals( newAct.getCoord() ) ) {
//			log.info( "act location changed" ) ;
			return true ;
		}
		if ( oldAct.getLinkId()==null || network.getLinks().get(oldAct.getLinkId()) == null ) { // careful: only the old activity has a link
//			log.info( "act link does not exist any more" ) ;
			return true ;
		}
		return false ;
	}
	
	/**
	 * getter method for PopulationCounter
	 * @return
	 */
	public PopulationCounter getPopulationCounter(){
		return this.cnt;
	}
	
	/**
	 * getter method for year
	 * @return
	 */
	public int getYear(){
		return this.year;
	}
	
	/**
	 * dumps out raw and aggregated population data as csv
	 * @param parcels
	 * @param network
	 */
	public void readAndDumpPersons2CSV(final ActivityFacilitiesImpl parcels, final Network network){
		
		String filename = module.getMATSim4OpusTemp() + InternalConstants.URBANSIM_PERSON_DATASET_TABLE + this.year + InternalConstants.FILE_TYPE_TAB;
		log.info( "Starting to read persons table from " + filename );
		
		Map<Id, SpatialReferenceObject> personLocations = new ConcurrentHashMap<Id, SpatialReferenceObject>();
		Map<Id, AggregationObject> personClusterMap = new ConcurrentHashMap<Id, AggregationObject>();
		
		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			// get columns for home, work and person id
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, InternalConstants.TAB_SEPERATOR );
			final int indexParcelID_HOME 	= idxFromKey.get( InternalConstants.PARCEL_ID_HOME );
			final int indexPersonID			= idxFromKey.get( InternalConstants.PERSON_ID );
			
			while ( (line=reader.readLine()) != null ) {
				
				String[] parts = line.split( InternalConstants.TAB_SEPERATOR );
				
				// check if home location is available
				if( Integer.parseInt(parts[ indexParcelID_HOME ]) >=  0){
					// create new person
					Id<Person> personId = Id.create( parts[ indexPersonID ] , Person.class);
					// get home location id
					Id<ActivityFacility> homeParcelId = Id.create( parts[ indexParcelID_HOME ] , ActivityFacility.class);
					ActivityFacility homeLocation = parcels.getFacilities().get( homeParcelId );
					
					
					if(homeLocation != null){
						personLocations.put(personId, new SpatialReferenceObject(personId, homeParcelId, null, homeLocation.getCoord()));
						
						{ // aggregating persons to nearest network nodes
							assert( homeLocation.getCoord() != null );
							Node nearestNode = ((NetworkImpl)network).getNearestNode( homeLocation.getCoord() );
							assert( nearestNode != null );
	
							if( personClusterMap.containsKey( nearestNode.getId() ) ){
								AggregationObject co = personClusterMap.get( nearestNode.getId() );
								co.addObject( personId, 0. );
							} else {
								personClusterMap.put( nearestNode.getId(), 
										new AggregationObject( personId, homeParcelId, null, nearestNode, 0.) );
							}
						}
					}
				}
			}
			// dump population data
			AnalysisPopulationCSVWriter.writePopulationData2CSV( personLocations, module );
			// dump aggregated population data
			AnalysisPopulationCSVWriter.writeAggregatedPopulationData2CSV(personClusterMap, module);
			
		} catch (Exception e) {e.printStackTrace();}
	}

	//////////////////////////////
	// helper classes
	//////////////////////////////
	
	public class ZoneLocations{
		
		private Id<ActivityFacility> zoneIdHome = null;
		private Id<ActivityFacility> zoneIdWork = null;
		private Coord homeCoord = null;
		private Coord workCoord = null;
		
		public void setHomeZoneIDAndZoneCoordinate(Id<ActivityFacility> zoneId, ActivityFacility home){
			this.zoneIdHome = zoneId;
			this.homeCoord  = home.getCoord();
		}
		public void setWorkZoneIDAndZoneCoordinate(Id<ActivityFacility> zoneId, ActivityFacility work){
			this.zoneIdWork = zoneId;
			this.workCoord  = work.getCoord();
		}
		
		public Id<ActivityFacility> getHomeZoneID(){
			return this.zoneIdHome;
		}
		public Id<ActivityFacility> getWorkZoneID(){
			return this.zoneIdWork;
		}
		public Coord getHomeZoneCoord(){
			return this.homeCoord;
		}
		public Coord getWorkZoneCoord(){
			return this.workCoord;
		}
	}
	
	public class PopulationCounter{
		
		public long numberOfUrbanSimPersons = 0;// total number of UrbanSim Persons	
		public long newPersonCnt = 0;			// persons not exists in initial input plans file (new UrbanSim Persons), these are put into backupPop population
		public long identifiedCnt = 0;			// person exists in initial input plans file
		public long employmentChangedCnt = 0;	// person exists but employment status changed
		public long homelocationChangedCnt = 0;	// person exists but home location changed
		public long worklocationChangedCnt = 0;	// person exists but work location changed
		public long unemployedCnt = 0;			// person exists but is unemployed (i.e. is handeld as a new person)
		public long jobLocationIdNullCnt = 0 ;	// person exists but job not found (i.e. person is out and not considered anymore)
		public long fromBackupCnt = 0;			// counts how much of the backup population moved to newPopulation (to reach the sampleRate)
		public long populationMergeTotal = 0;	// counts the total number of person in newPopulation
		
		public void reset(){
			this.newPersonCnt = 0;
			this.identifiedCnt = 0;
			this.employmentChangedCnt = 0;
			this.homelocationChangedCnt = 0;
			this.worklocationChangedCnt = 0;
			this.unemployedCnt = 0;
			this.jobLocationIdNullCnt = 0;
			this.fromBackupCnt = 0;
			this.populationMergeTotal = 0;
		}
	}
//	
//	private class JobCounter{
//		public long numberOfUrbanSimJobs = 0;
//		public long skippedJobs = 0;
//		public long backupJobs = 0;
//		public long allowedJobs = 0;
//		public long addedJobsFromBackup = 0;
//	}
	
}
