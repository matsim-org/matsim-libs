/**
 *
 */
package playground.tnicolai.matsim4opus.utils.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobsObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.WorkplaceObject;
import playground.tnicolai.matsim4opus.utils.ids.ZoneId;

/**
 * @author nagel
 *
 */
public class ReadFromUrbansimParcelModel {

	// Logger
	private static final Logger log = Logger.getLogger(ReadFromUrbansimParcelModel.class);
	// year of current urbansim run
	private final int year;
	
	private PopulationCounter cnt;
	
	/**
	 * constructor
	 * @param year of current run
	 */
	public ReadFromUrbansimParcelModel ( final int year ) {
		this.year = year;
		this.cnt = new PopulationCounter();
	}

	/**
	 * reads the parcel data set table from urbansim and creates ActivityFacilities
	 *
	 * @param parcels ActivityFacilitiesImpl
	 * @param zones ActivityFacilitiesImpl
	 */
	public void readFacilities(final ActivityFacilitiesImpl parcels, final ActivityFacilitiesImpl zones) {
		// (these are simply defined as those entities that have x/y coordinates in urbansim)
		String filename = Constants.MATSIM_4_OPUS_TEMP + Constants.URBANSIM_PARCEL_DATASET_TABLE + this.year + Constants.FILE_TYPE_TAB;
		
		log.info( "Starting to read urbansim parcels table from " + filename );

		// temporary data structure in order to get coordinates for zones:
		// Map<Id,Id> zoneFromParcel = new HashMap<Id,Id>();
		Map<Id,PseudoZone> pseudoZones = new HashMap<Id,PseudoZone>();

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			// read header of facilities table
			String line = reader.readLine();

			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = UtilityCollection.createIdxFromKey( line, Constants.TAB_SEPERATOR );
			final int indexParcelID 	= idxFromKey.get( Constants.PARCEL_ID );
			final int indexXCoodinate 	= idxFromKey.get( Constants.X_COORDINATE );
			final int indexYCoodinate 	= idxFromKey.get( Constants.Y_COORDINATE );
			final int indexZoneID 		= idxFromKey.get( Constants.ZONE_ID );

			// temporary variables, needed to construct parcels and zones
			Id parcel_ID;
			Coord coord;
			ZoneId zone_ID;
			PseudoZone pseudoZone;
			String[] parts;

			//
			while ( (line = reader.readLine()) != null ) {
				parts = line.split(Constants.TAB_SEPERATOR);

				// urbansim sometimes writes IDs as floats!
				long parcelIdAsLong = (long) Double.parseDouble( parts[ indexParcelID ] );
				parcel_ID = new IdImpl( parcelIdAsLong ) ;

				// get the coordinates of that parcel
				coord = new CoordImpl( parts[ indexXCoodinate ],parts[ indexYCoodinate ] );

				// create a facility (within the parcels) object at this coordinate with the correspondent parcel ID
				ActivityFacilityImpl facility = parcels.createFacility(parcel_ID,coord);
				facility.setDesc( Constants.FACILITY_DESCRIPTION ) ;
				
				// get zone ID
				long zoneIdAsLong = (long) Double.parseDouble( parts[ indexZoneID ] );
				zone_ID = new ZoneId( zoneIdAsLong );

				// set custom attributes, these are needed to compute zone2zone trips
				Map<String, Object> customFacilityAttributes = facility.getCustomAttributes();
				customFacilityAttributes.put(Constants.ZONE_ID, zone_ID);
				customFacilityAttributes.put(Constants.PARCEL_ID, parcel_ID);

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
				pseudoZone.count ++ ; // tnicolai: wights from urbansim parcels for homes and workplaces
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit( Constants.EXCEPTION_OCCURED );
		} catch (IOException e) {
			e.printStackTrace();
			System.exit( Constants.EXCEPTION_OCCURED );
		}

		// create urbansim zones from the intermediate pseudo zones
		constructZones ( zones, pseudoZones) ;
	}

	/**
	 * this method creates the final zones from the merged parcels (with the same zone ID)
	 * by computing the average value of the coordinates for each zone ID.
	 *
	 * @param zones ActivityFacilitiesImpl
	 * @param pseudoZones Map<Id,PseudoZone>
	 */
	void constructZones (final ActivityFacilitiesImpl zones, final Map<Id,PseudoZone> pseudoZones  ) {

		log.info( "Starting to construct urbansim zones (for the impedance matrix)" ) ;

		Id zone_ID;
		PseudoZone pz;
		Coord coord;

		// constructing the zones from the pseudoZones:
		for ( Entry<Id,PseudoZone> entry : pseudoZones.entrySet() ) {
			zone_ID = entry.getKey();
			pz = entry.getValue();
			// compute the average center of a zone
			coord = new CoordImpl( pz.sumXCoordinate/pz.count , pz.sumYCoordinate/pz.count );
			zones.createFacility(zone_ID, coord);
		}
		log.info( "Done with constructing urbansim zones" ) ;
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
	 * @param parcels
	 * @param network
	 * @param samplingRate
	 */
	public Population readPersons(Population oldPop, final ActivityFacilitiesImpl parcels, final NetworkImpl network, final double samplingRate) {
		
		Population mergePop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation(); // will contain all persons from UrbanSim Persons table
		Population backupPop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation(); // will contain only new persons (id) that don't exist in warm start pop file
		cnt = new PopulationCounter();
		
		boolean compensationFlag = false;

		String filename = Constants.MATSIM_4_OPUS_TEMP + Constants.URBANSIM_PERSON_DATASET_TABLE + this.year + Constants.FILE_TYPE_TAB;
		
		log.info( "Starting to read persons table from " + filename );

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			// get columns for home, work and person id
			Map<String,Integer> idxFromKey = UtilityCollection.createIdxFromKey( line, Constants.TAB_SEPERATOR );
			final int indexParcelID_HOME 	= idxFromKey.get( Constants.PARCEL_ID_HOME );
			final int indexParcelID_WORK 	= idxFromKey.get( Constants.PARCEL_ID_WORK );
			final int indexPersonID			= idxFromKey.get( Constants.PERSON_ID );

			// We consider two cases:
			// (1) We have an old population.  Then we look for those people who have the same id.
			// (2) We do not have an old population.  Then we construct a new one.
			// In both cases we assume that the new population has the right size.

			while ( (line=reader.readLine()) != null ) {
				cnt.NUrbansimPersons++;
				String[] parts = line.split( Constants.TAB_SEPERATOR );

				// create new person
				Id personId = new IdImpl( parts[ indexPersonID ] );

				if ( !( compensationFlag || MatsimRandom.getRandom().nextDouble() < samplingRate || personExistsInOldPopulation(oldPop, personId)) )
					continue ;

				compensationFlag = false;
				
				PersonImpl newPerson = new PersonImpl( personId ) ;

				// get home location id
				Id homeParcelId = new IdImpl( parts[ indexParcelID_HOME ] ) ;
				ActivityFacility homeLocation = parcels.getFacilities().get( homeParcelId ) ;
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
				PlanImpl plan = newPerson.createAndAddPlan(true);
				UtilityCollection.makeHomePlan(plan, homeCoord, homeLocation) ;

				// determine employment status
				if ( parts[ indexParcelID_WORK ].equals("-1") )
					newPerson.setEmployed(Boolean.FALSE);
				else {
					newPerson.setEmployed(Boolean.TRUE);
					Id workParcelId = new IdImpl( parts[ indexParcelID_WORK ] ) ;
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
					UtilityCollection.completePlanToHwh(plan, workCoord, jobLocation) ;
				}

				// at this point, we have a full "new" person.  Now check against pre-existing population ...
				mergePopulation(oldPop, mergePop, network, backupPop, cnt, personId, newPerson);
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
		long targetPopSize = Math.round(samplingRate*cnt.NUrbansimPersons);
		
		if(mergePop.getPersons().size() == targetPopSize){
			log.info("Size of new population (" + mergePop.getPersons().size() + ") fits the target population size with samplingRate*NumberUrbansimPersons (" + targetPopSize + ")." );
			printDetailedPopulationInfo(cnt);
		}
		// check if newPop contains less people than our target population size (less than sampleRate*NUrbansimPersons) and add additional persons from backupPop
		else if(mergePop.getPersons().size() < targetPopSize){
			log.info("Size of new population (" + mergePop.getPersons().size() + ") is smaller than samplingRate*NumberUrbansimPersons (" + targetPopSize + "). Adding persons, stored in bakPopSize ...");
			List<Person> backupPopList = new ArrayList<Person>( backupPop.getPersons().values() ); // Population data structure not needed!
			Collections.shuffle( backupPopList );	// pick random person
			
			for ( Person person : backupPopList ) {
				if ( mergePop.getPersons().size() >= samplingRate*cnt.NUrbansimPersons )
					break ;
				cnt.fromBackupCnt++;
				mergePop.addPerson( person ) ;
			}
			log.info(" ... done adding persons from backupPopulation!");
			
			printDetailedPopulationInfo(cnt);
		}
		// Check if newPop contain too many people than our target population size and remove persons from newPop if needed
		else if(mergePop.getPersons().size() > targetPopSize){
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
				+ " backupPopulation: " + backupPop.getPersons().size() + " NumberUrbansimPersons: " + cnt.NUrbansimPersons );
//		log.warn("why is bakPopSize not approx as large as samplingRate*NumberUrbansimPersons?" );
	}

	/**
	 * @param oldPop
	 * @param newPop
	 * @param network
	 * @param backupPop
	 * @param cnt
	 * @param personId
	 * @param newPerson
	 */
	private void mergePopulation(final Population oldPop,
			final Population newPop, final NetworkImpl network,
			Population backupPop, PopulationCounter cnt, Id personId, PersonImpl newPerson) {
		while ( true ) { // loop from which we can "break":

			Person oldPerson ;
			cnt.populationMergeTotal++;

			if ( oldPop==null ) { // no pre-existing population.  Accept:
				newPop.addPerson(newPerson);
				break;
			} else if ( (oldPerson=oldPop.getPersons().get(personId))==null ) { // person not found in old population. Store temporarily in backup.
				backupPop.addPerson( newPerson );
				cnt.newPersonCnt++;
				break;
			} else if ( ((PersonImpl) oldPerson).isEmployed() != newPerson.isEmployed() ) { // employment status changed. Accept new person:
				newPop.addPerson(newPerson);
				cnt.employmentChangedCnt++;
				break;
			}
			Activity oldHomeAct = ((PlanImpl) oldPerson.getSelectedPlan()).getFirstActivity();
			Activity newHomeAct = ((PlanImpl) newPerson.getSelectedPlan()).getFirstActivity();
			if ( actHasChanged ( oldHomeAct, newHomeAct, network ) ) { // if act changed. Accept new person:
				newPop.addPerson(newPerson);
				cnt.homelocationChangedCnt++;
				break;
			}

			// check if new person works
			if ( !newPerson.isEmployed() ) { // person does not move; doesn't matter. TODO fix this when other activities are considered
				newPop.addPerson(newPerson);
				cnt.unemployedCnt++;
				break ;
			}

			// check if work act has changed:
			ActivityImpl oldWorkAct = (ActivityImpl) oldPerson.getSelectedPlan().getPlanElements().get(2) ;
			ActivityImpl newWorkAct = (ActivityImpl) newPerson.getSelectedPlan().getPlanElements().get(2) ;
			if ( actHasChanged ( oldWorkAct, newWorkAct, network ) ) {
				newPop.addPerson(newPerson);
				cnt.worklocationChangedCnt++;
				break ;
			}

			// no "break" up to here, so new person does the same as the old person. Keep old person (including its
			// routes etc.)
			newPop.addPerson(oldPerson);
			cnt.identifiedCnt++;
			break ;
		}
	}
	
	/**
	 * Reads in the job table from urbansim that contains for every "job_id" the corresponded "parcel_id_work" and "zone_id_work"
	 * and returns an HashMap with the number of job for each zone.
	 * 
	 * This is usually used for aggregated accessibility computations, e. g. zone-to-zone impedance matrix 
	 * 
	 * @return HashMap
	 */
	public Map<Id,WorkplaceObject> readZoneBasedWorkplaces(){
		
		String filename = Constants.MATSIM_4_OPUS_TEMP + Constants.URBANSIM_JOB_DATASET_TABLE + this.year + Constants.FILE_TYPE_TAB;
		
		log.info( "Starting to read jobs table from " + filename );

		Map<Id,WorkplaceObject> numberOfWorkplacesPerZone = new HashMap<Id,WorkplaceObject>();
		
		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );
			
			// reading header
			String line = reader.readLine();
			// get columns for home, work and person id
			Map<String,Integer> idxFromKey = UtilityCollection.createIdxFromKey( line, Constants.TAB );
			final int indexZoneID_WORK	   = idxFromKey.get( Constants.ZONE_ID_WORK );
			
			ZoneId zone_ID;
			WorkplaceObject workObj;
			
			while ( (line=reader.readLine()) != null ) {
				String[] parts = line.split( Constants.TAB );
				
				// get zone ID
				long zoneIdAsLong = (long) Double.parseDouble( parts[ indexZoneID_WORK ] );
				zone_ID = new ZoneId( zoneIdAsLong );
				
				// each zone ID indicates a job
				workObj = numberOfWorkplacesPerZone.get(zone_ID);
				if( workObj == null){
					workObj = new WorkplaceObject();
					numberOfWorkplacesPerZone.put(zone_ID, workObj);
				}
				workObj.counter++;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return numberOfWorkplacesPerZone;
	}
	
	/**
	 * Reads in the job table from urbansim and returns a HashMap with all jobs containing the following attributes:
	 * - job ID
	 * - parcel ID
	 * - zone ID
	 * - workplace coordinate
	 * 
	 * This is usually used for (disagregated) 1-point accessibility measures 
	 * 
	 * @param parcels
	 * @return HashMap
	 */
	public JobClusterObject[] readAndBuildJobsObject(final ActivityFacilitiesImpl parcels, double jobSample){
		
		// readJobs creates a hash map of job with key = job id
		// this hash map includes jobs according to job sample size
		List<JobsObject> jobSampleList = readJobs(parcels, jobSample);
		assert( jobSampleList != null );
		// this hash map aggregates jobs belonging to the same parcel (key = parcel id)
		JobClusterObject[] jobClusterArray = aggregateJobsWithSameParcelID(jobSampleList);
				
		return jobClusterArray;		
	}

	/**
	 * Aggregates jobs with the same parcel ID 
	 * 
	 * @param jobObjectMap
	 */
	public JobClusterObject[] aggregateJobsWithSameParcelID(List<JobsObject> jobSampleList) {
		
		log.info("Aggregating Job with identical parcel ID ...");
		Map<Id, JobClusterObject> jobClusterMap = new HashMap<Id, JobClusterObject>();
		
		ProgressBar bar = new ProgressBar( jobSampleList.size() );
		
		for(int i = 0; i < jobSampleList.size(); i++){
			bar.update();
			JobsObject jo = jobSampleList.get( i );
			
			if( jobClusterMap.containsKey( jo.getParcelID() ) ){
				JobClusterObject jco = jobClusterMap.get( jo.getParcelID() );
				jco.addJob( jo.getJobID() );
			}
			else
				jobClusterMap.put( jo.getParcelID(), new JobClusterObject(jo.getJobID(),
																		  jo.getZoneID(),
																		  jo.getCoord() ) );
		}
		
		JobClusterObject jobClusterArray []  = new JobClusterObject[ jobClusterMap.size() ];
		Iterator<JobClusterObject> jobClusterIterator = jobClusterMap.values().iterator();

		for(int i = 0; jobClusterIterator.hasNext(); i++)
			jobClusterArray[i] = jobClusterIterator.next();
		
		log.info("Aggregated number of jobs from " + jobSampleList.size() + " to " + jobClusterArray.length);
		
		return jobClusterArray;
	}

	/**
	 * reads jobs from UrbanSim output and return a list of jobs according
	 * to the specified sample size 
	 * 
	 * @param parcels
	 * @param jobSample
	 * @return
	 */
	private List<JobsObject> readJobs(final ActivityFacilitiesImpl parcels,
			double jobSample) {
		Id jobID, parcelID, zoneID;
		Coord coord;
		JobCounter cnt = new JobCounter();
		
		List<JobsObject> jobSampleList = new ArrayList<JobsObject>();
		List<JobsObject> backupList = new ArrayList<JobsObject>();
		
		
		String filename = Constants.MATSIM_4_OPUS_TEMP + Constants.URBANSIM_JOB_DATASET_TABLE + this.year + Constants.FILE_TYPE_TAB;
		
		if(parcels != null){
			
			log.info( "Starting to read jobs table from " + filename );
			
			Map<Id, ActivityFacility> parcelsMap = parcels.getFacilities();
			
			try{
				BufferedReader reader = IOUtils.getBufferedReader( filename );
				
				// reading header
				String line = reader.readLine();
				// get columns for job, parcel and zone id
				Map<String,Integer> idxFromKey = UtilityCollection.createIdxFromKey( line, Constants.TAB );
				final int indexJobID = idxFromKey.get( Constants.JOB_ID );
				final int indexParcelID = idxFromKey.get( Constants.PARCEL_ID_WORK );
				final int indexZoneID =idxFromKey.get( Constants.ZONE_ID_WORK );
				
				boolean isBackup = false;
				
				while ( (line=reader.readLine()) != null ) {
					cnt.NUrbansimJobs++;
					isBackup = false;
					
					if(MatsimRandom.getRandom().nextDouble() > jobSample)
						isBackup = true;
					
					String[] parts = line.split( Constants.TAB );
					
					jobID = new IdImpl( parts[indexJobID] );
					assert( jobID != null);
					
					if( Integer.parseInt( parts[indexParcelID] ) >= 0 ){
					
						parcelID = new IdImpl( parts[indexParcelID] );
						assert( parcelID != null );
						zoneID = new IdImpl( parts[indexZoneID] );
						assert( zoneID != null );
						coord = parcelsMap.get( parcelID ).getCoord();
						assert( coord != null );
						
						if(isBackup){
							backupList.add( new JobsObject(jobID, parcelID, zoneID, coord) );
							cnt.backupJobs++;
						}
						else
							jobSampleList.add( new JobsObject(jobID, parcelID, zoneID, coord) );
					}
					else
						cnt.skippedJobs++;	// counting number of skipped jobs ...
				}
				
				reader.close();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		log.info("Reading Jobs done!");
		
		checkAndAdjustJobSampleSize(jobSample, cnt, jobSampleList, backupList);
		
		return jobSampleList;
	}

	/**
	 * @param jobSample
	 * @param cnt
	 * @param jobObjectMap
	 * @param backupList
	 */
	private void checkAndAdjustJobSampleSize(double jobSample, JobCounter cnt, List<JobsObject> jobSampleList, List<JobsObject> backupList) {
		
		cnt.allowedJobs = Math.round( jobSample*cnt.NUrbansimJobs );
		
		log.info(" samplingRate: " + jobSample + "; total number of jobs: " + cnt.NUrbansimJobs + "; number jobs schould count: " + cnt.allowedJobs +
				 "; actual number jobs: " + jobSampleList.size() + "; number of jobs in backupList: " +cnt.backupJobs + "; skipped jobs (without parcel id):" + cnt.skippedJobs);
		
		// if job sample size is too low add jobs from backup list
		if(cnt.allowedJobs > jobSampleList.size()){
			log.info("Size of actual added jobs (" + jobSampleList.size() + ") is smaller than samplingRate*NumberUrbansimJobs (" + cnt.allowedJobs + "). Adding jobs, stored in backupList ... ");
			Collections.shuffle( backupList );
			
			for(JobsObject jObject : backupList){
				if(cnt.allowedJobs > jobSampleList.size()){
					jobSampleList.add( jObject );
					cnt.addedJobsFromBackup++;
				}
				else break;
			}
			log.info("... done!");
		}
		// otherwise if too many jobs were added remove jobs
		else if(cnt.allowedJobs < jobSampleList.size()){
			log.info("Actual size of jobs (" + jobSampleList.size() + ") is larger than samplingRate*NumberUrbansimJobs (" + cnt.allowedJobs + "). Removing jobs... ");
			
			Random randomGenerator = new Random();
			
			// removes jobs randomly
			while(cnt.allowedJobs < jobSampleList.size()){
				
				int index = randomGenerator.nextInt( jobSampleList.size() );
				jobSampleList.remove( index );
			}
		}
		
		log.info(" samplingRate: " + jobSample + "; total number of jobs: " + cnt.NUrbansimJobs + "; number jobs schould count: " + cnt.allowedJobs +
				 "; actual number jobs: " + jobSampleList.size() + "; number of jobs in backupList: " +cnt.backupJobs + "; skipped jobs (without parcel id):" + cnt.skippedJobs +
				 "; number of jobs added from backupList: " + cnt.addedJobsFromBackup);
	}

	/**
	 * determine if a person already exists
	 * @param oldPop
	 * @param personId
	 * @return true if a person already exists in an old population
	 */
	private boolean personExistsInOldPopulation(Population oldPop, Id personId){

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
	private boolean actHasChanged ( final Activity oldAct, final Activity newAct, final NetworkImpl network ) {
		
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

	public class PopulationCounter{
		
		public long NUrbansimPersons = 0;		// total number of UrbanSim Persons	
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
	
	private class JobCounter{
		public long NUrbansimJobs = 0;
		public long skippedJobs = 0;
		public long backupJobs = 0;
		public long allowedJobs = 0;
		public long addedJobsFromBackup = 0;
	}
	
}
