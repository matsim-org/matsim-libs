/**
 *
 */
package playground.tnicolai.urbansim.utils.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.kai.urbansim.ids.ZoneId;
import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.CommonMATSimUtilities;

/**
 * @author nagel
 *
 */
public class ReadFromUrbansimParcelModel {

	// Logger
	private static final Logger log = Logger.getLogger(ReadFromUrbansimParcelModel.class);
	// year of current urbansim run
	private final int year;

	/**
	 * constructor
	 * @param year of current run
	 */
	public ReadFromUrbansimParcelModel ( final int year ) {
		this.year = year;
	}

	/**
	 * reads the parcel data set table from urbansim and creates ActivityFacilities
	 *
	 * @param parcels ActivityFacilitiesImpl
	 * @param zones ActivityFacilitiesImpl
	 */
	@SuppressWarnings("deprecation")
	public void readFacilities(final ActivityFacilitiesImpl parcels, final ActivityFacilitiesImpl zones) {
		// (these are simply defined as those entities that have x/y coordinates in urbansim)
		String filename = Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + Constants.URBANSIM_PARCEL_DATASET_TABLE + this.year + Constants.FILE_TYPE_TAB;
		log.info( "Starting to read urbansim parcels from " + filename );

		// temporary data structure in order to get coordinates for zones:
		// Map<Id,Id> zoneFromParcel = new HashMap<Id,Id>();
		Map<Id,PseudoZone> pseudoZones = new HashMap<Id,PseudoZone>();

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			// read header of facilities table
			String line = reader.readLine();

			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = CommonMATSimUtilities.createIdxFromKey( line, Constants.TAB_SEPERATOR );
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
				parts = line.split(Constants.TAB_SEPERATOR); //String[] parts = line.split("[\t]+");

				// Urbansim sometimes writes IDs as floats!
				long parcelIdAsLong = (long) Double.parseDouble( parts[ indexParcelID ] ) ;
				parcel_ID = new IdImpl( parcelIdAsLong ) ;

				// get the coordinates of that parcel
				coord = new CoordImpl( parts[ indexXCoodinate ],parts[ indexYCoodinate ] ) ;

				// create a facility (within the parcels) object at this coordinate with the correspondent parcel ID
				ActivityFacilityImpl facility = parcels.createFacility(parcel_ID,coord) ;
				facility.setDesc( Constants.FACILITY_DESCRIPTION ) ;

				// get zone ID
				long zoneIdAsLong = (long) Double.parseDouble( parts[ indexZoneID ] ) ;
				zone_ID = new ZoneId( zoneIdAsLong ) ;

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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit( Constants.EXCEPTION_OCCURED );
		} catch (IOException e) {
			e.printStackTrace();
			System.exit( Constants.EXCEPTION_OCCURED );
		}
		log.info( "DONE with reading urbansim parcels" ) ;

		// create urbansim zones from the intermediate pseudo zones
		constructZones ( zones, pseudoZones) ;
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
	 * this method creates the final zones from the merged parcels (with the same zone ID)
	 * by computing the average value of the coordinates for each zone ID.
	 *
	 * @param zones ActivityFacilitiesImpl
	 * @param pseudoZones Map<Id,PseudoZone>
	 */
	public void constructZones (final ActivityFacilitiesImpl zones, final Map<Id,PseudoZone> pseudoZones  ) {

		log.info( "Starting to construct urbansim zones (for the impedance matrix)" ) ;

		Id zone_ID;
		PseudoZone pz;
		Coord coord;

		// constructing the zones from the pseudoZones:
		for ( Entry<Id,PseudoZone> entry : pseudoZones.entrySet() ) {
			zone_ID = entry.getKey();
			pz = entry.getValue() ;
			// compute the average center of a zone
			coord = new CoordImpl( pz.sumXCoordinate/pz.count , pz.sumYCoordinate/pz.count ) ;
			zones.createFacility(zone_ID, coord) ;
		}
		log.info( "Finished constructing urbansim zones" ) ;
	}

	/**
	 *
	 * @param oldPop
	 * @param newPop
	 * @param facilities
	 * @param network
	 * @param samplingRate
	 */
	public void readPersons(final Population oldPop, final Population newPop, final ActivityFacilitiesImpl facilities, final NetworkImpl network, final double samplingRate) {

		Population backupPop = new ScenarioImpl().getPopulation() ;
		long NUrbansimPersons=0 ;
		long notFoundCnt = 0 ;
		long jobLocationIdNullCnt = 0 ;
		boolean flag = false;

		String filename = Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + Constants.URBANSIM_PERSON_DATASET_TABLE + this.year + Constants.FILE_TYPE_TAB;
		log.info( "Starting to read persons from " + filename ) ;

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			// get columns for home, work and person id
			Map<String,Integer> idxFromKey = CommonMATSimUtilities.createIdxFromKey( line, Constants.TAB_SEPERATOR );
			final int indexParcelID_HOME 	= idxFromKey.get( Constants.PARCEL_ID_HOME );
			final int indexParcelID_WORK 	= idxFromKey.get( Constants.PARCEL_ID_WORK );
			final int indexPersonID			= idxFromKey.get( Constants.PERSON_ID );

			// We consider two cases:
			// (1) We have an old population.  Then we look for those people who have the same id.
			// (2) We do not have an old population.  Then we construct a new one.
			// In both cases we assume that the new population has the right size.

			while ( (line=reader.readLine()) != null ) {
				NUrbansimPersons++;
				String[] parts = line.split( Constants.TAB_SEPERATOR );

				// create new person
				Id personId = new IdImpl( parts[ indexPersonID ] ) ;
				PersonImpl newPerson = new PersonImpl( personId ) ;

				if ( !( flag || MatsimRandom.getRandom().nextDouble() < samplingRate || personExistsInOldPopulation(oldPop, personId) ) )
					continue ;

				flag = false ;

				// get home location id
				Id homeParcelId = new IdImpl( parts[ indexParcelID_HOME ] ) ;
				ActivityFacility homeLocation = facilities.getFacilities().get( homeParcelId ) ;
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
				CommonMATSimUtilities.makeHomePlan(plan, homeCoord) ;

				// determine employment status
				if ( parts[ indexParcelID_WORK ].equals("-1") )
					newPerson.setEmployed(Boolean.FALSE);
				else {
					newPerson.setEmployed(Boolean.TRUE);
					Id workParcelId = new IdImpl( parts[ indexParcelID_WORK ] ) ;
					ActivityFacility jobLocation = facilities.getFacilities().get( workParcelId ) ;
					if ( jobLocation == null ) {
						// show warning message only once
						if ( jobLocationIdNullCnt < 1 ) {
							jobLocationIdNullCnt++ ;
							log.warn( "jobLocationId==null, probably out of area. person_id: " + personId
									+ " workp_prcl_id: " + workParcelId + Gbl.ONLYONCE ) ;
						}
						flag = true ; // WHY?
						continue ;
					}
					// complete agent plan
					Coord workCoord = jobLocation.getCoord() ;
					CommonMATSimUtilities.completePlanToHwh(plan, workCoord) ;
				}

				// at this point, we have a full "new" person.  Now check against pre-existing population ...

				while ( true ) { // loop from which we can "break":

					Person oldPerson ;

					if ( oldPop==null ) { // no pre-existing population.  Accept:
						newPop.addPerson(newPerson);
						break;
					} else if ( (oldPerson=oldPop.getPersons().get(personId))==null ) { // did not find person.  Put in backup:
						backupPop.addPerson( newPerson );
						notFoundCnt++;
						break;
					} else if ( ((PersonImpl) oldPerson).isEmployed() != newPerson.isEmployed() ) { // employment status changed.  Accept new person:
						newPop.addPerson(newPerson);
						break;
					}
					Activity oldHomeAct = ((PlanImpl) oldPerson.getSelectedPlan()).getFirstActivity();
					Activity newHomeAct = ((PlanImpl) newPerson.getSelectedPlan()).getFirstActivity();
					if ( actHasChanged ( oldHomeAct, newHomeAct, network ) ) { // act changed.  Accept new person:
						newPop.addPerson(newPerson);
						break;
					}

					// check if new person works
					if ( !newPerson.isEmployed() ) { // person does not move; doesn't matter.  TODO fix this when other activities are considered
						newPop.addPerson(newPerson) ;
						break ;
					}

					// check if work act has changed:
					ActivityImpl oldWorkAct = (ActivityImpl) oldPerson.getSelectedPlan().getPlanElements().get(2) ;
					ActivityImpl newWorkAct = (ActivityImpl) newPerson.getSelectedPlan().getPlanElements().get(2) ;
					if ( actHasChanged ( oldWorkAct, newWorkAct, network ) ) {
						newPop.addPerson(newPerson) ;
						break ;
					}

					// no "break" up to here, so new person does the same as the old person.  Keep old person (including its
					// routes etc.)
					newPop.addPerson(oldPerson) ;
					break ;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1) ;
		} catch (IOException e) {
			e.printStackTrace();
		}

		int oldPopSize = oldPop==null ? 0 : oldPop.getPersons().size();

		log.info(" samplingRate: " + samplingRate + " oldPopSize: " + oldPopSize + " newPopSize: " + newPop.getPersons().size()
				+ " bakPopSize: " + backupPop.getPersons().size() + " NUrbansimPersons: " + NUrbansimPersons ) ;
		log.warn("why is bakPopSize not approx as large as samplingRate*NUrbansimPersons?" ) ;

		List<Person> bakPersons = new ArrayList<Person>( backupPop.getPersons().values() ) ; // Population data structure not needed!
		Collections.shuffle( bakPersons ) ;
		for ( Person person : bakPersons ) {
			if ( newPop.getPersons().size() >= samplingRate*NUrbansimPersons )
				break ;

			newPop.addPerson( person ) ;
		}

		log.info(" samplingRate: " + samplingRate + " oldPopSize: " + oldPopSize + " newPopSize: " + newPop.getPersons().size()
				+ " bakPopSize: " + backupPop.getPersons().size() + " NUrbansimPersons: " + NUrbansimPersons ) ;

		log.info( "Done with reading persons." ) ;
	}

	/**
	 * determine if a person already exists
	 * @param oldPop
	 * @param personId
	 * @return true if a person already exists in an old poulation
	 */
	private boolean personExistsInOldPopulation(Population oldPop, Id personId){

		if(oldPop == null)
			return false;
		else
			return oldPop.getPersons().get( personId) != null;
	}

	/**
	 *
	 * @return number of persons determined by person id
	 */
	public int countPersons(){
		String filename = Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + Constants.URBANSIM_PERSON_DATASET_TABLE + this.year + Constants.FILE_TYPE_TAB;
		log.info( "Starting to read persons from " + filename ) ;
		// counter
		int persons = 0;

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			// get columns for home, work and person id
			Map<String,Integer> idxFromKey = CommonMATSimUtilities.createIdxFromKey( line, Constants.TAB_SEPERATOR );
			final int indexPersonID			= idxFromKey.get( Constants.PERSON_ID );

			while ( (line=reader.readLine()) != null ) {
				String[] parts = line.split( Constants.TAB_SEPERATOR );

				try{
					// make sure that column contains an id
					Integer.parseInt( parts[ indexPersonID ] );
					persons ++;
				}
				catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			}
			return persons;
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 *
	 * @param oldAct
	 * @param newAct
	 * @param network
	 * @return
	 */
	private boolean actHasChanged ( final Activity oldAct, final Activity newAct, final NetworkImpl network ) {
		if ( !oldAct.getCoord().equals( newAct.getCoord() ) ) {
			log.info( "act location changed" ) ;
			return true ;
		}
		if ( oldAct.getLinkId()==null || network.getLinks().get(oldAct.getLinkId()) == null ) { // careful: only the old activity has a link
			log.info( "act link does not exist any more" ) ;
			return true ;
		}
		return false ;
	}

}
