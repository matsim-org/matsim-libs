/**
 *
 */
package playground.kai.urbansim;

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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.world.Location;

import playground.kai.urbansim.ids.ZoneId;

/**
 * @author nagel
 *
 */
public class ReadFromUrbansimParcelModel {
	private static final Logger log = Logger.getLogger(ReadFromUrbansimParcelModel.class);

	private final String PATH_TO_OPUS_MATSIM = Matsim4Urbansim.PATH_TO_OPUS_MATSIM ;

	private final int year ;

	public ReadFromUrbansimParcelModel ( final int year ) {
		this.year = year ;
	}

	public void readFacilities(final ActivityFacilitiesImpl parcels, final ActivityFacilitiesImpl zones) {
		// (these are simply defined as those entities that have x/y coordinates in urbansim)
		String filename = this.PATH_TO_OPUS_MATSIM+"tmp/parcel__dataset_table__exported_indicators__" + this.year + ".tab" ;
		log.info( "Starting to read urbansim parcels from " + filename ) ;

		// temporary data structure in order to get coordinates for zones:
		Map<Id,Id> zoneFromParcel = new HashMap<Id,Id>() ;

		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename ) ;

			String line = reader.readLine() ;
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( line ) ;

			while ( (line = reader.readLine()) != null ) {
				String[] parts = line.split("[\t]+");

				// Urbansim sometimes writes IDs as floats!
				long parcelIdAsLong = (long) Double.parseDouble( parts[idxFromKey.get("parcel_id")] ) ;
				Id parcelId = new IdImpl( parcelIdAsLong ) ;

				Coord coord = new CoordImpl( parts[idxFromKey.get("x_coord_sp")],parts[idxFromKey.get("y_coord_sp")] ) ;

				ActivityFacilityImpl facility = parcels.createFacility(parcelId,coord) ;
				facility.setDesc("urbansim location") ;

				// Can't add info (in this case zone ID) to facilities, so put into separate data structure:
				long zoneIdAsLong = (long) Double.parseDouble( parts[idxFromKey.get("zone_id")] ) ;
				ZoneId zoneId = new ZoneId( zoneIdAsLong ) ;
				zoneFromParcel.put( parcelId, zoneId ) ;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info( "DONE with reading urbansim parcels" ) ;

		log.info( "Starting to construct urbansim zones (for the impedance matrix)" ) ;
		constructZones ( parcels, zones, zoneFromParcel ) ;
		log.info( "Done with constructing urbansim zones" ) ;

	}

	class PseudoZone {
		double sumx = 0. ;
		double sumy = 0. ;
		long cnt = 0 ;
	}

	public void constructZones ( final ActivityFacilitiesImpl parcels, final ActivityFacilitiesImpl zones, final Map<Id,Id> zoneFromParcel ) {

		// summing the coordinates of all participating parcels into the zones
		Map<Id,PseudoZone> pseudoZones = new HashMap<Id,PseudoZone>() ;
		for ( Entry<Id,Id> entry : zoneFromParcel.entrySet() ) {
			Id       parcelId =            entry.getKey();
			ZoneId   zoneId   = (ZoneId)   entry.getValue() ;

			Location parcel = parcels.getFacilities().get(parcelId) ;
			assert( parcel!= null ) ;
			Coord coord = parcel.getCoord();

			PseudoZone pseudoZone = pseudoZones.get(zoneId) ;
			if ( pseudoZone==null ) {
				pseudoZone = new PseudoZone() ;
				pseudoZones.put(zoneId, pseudoZone);
			}
			pseudoZone.sumx += coord.getX();
			pseudoZone.sumy += coord.getY() ;
			pseudoZone.cnt ++ ;
		}

		// constructing the zones from the pseudozones:
		for ( Entry<Id,PseudoZone> entry : pseudoZones.entrySet() ) {
			Id zoneId = entry.getKey();
			PseudoZone pz = entry.getValue() ;
			Coord coord = new CoordImpl( pz.sumx/pz.cnt , pz.sumy/pz.cnt ) ;
			zones.createFacility(zoneId, coord) ;
		}

	}

	public void readPersons(final Population oldPop, final Population newPop, final ActivityFacilitiesImpl facilities, final NetworkLayer network, final double samplingRate ) {
		String filename = this.PATH_TO_OPUS_MATSIM+"tmp/person__dataset_table__exported_indicators__" + this.year + ".tab" ;
		log.info( "Starting to read persons from " + filename ) ;

		Population backupPop = new ScenarioImpl().getPopulation() ;
		long NUrbansimPersons=0 ;
		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( line ) ;

			// We consider two cases:
			// (1) We have an old population.  Then we look for those people who have the same id.
			// (2) We do not have an old population.  Then we construct a new one.
			// In both cases we assume that the new population has the right size.

			long foundCnt = 0 ;
			long notFoundCnt = 0 ;
			long jobLocationIdNullCnt = 0 ;
			boolean flag = false ;
			while ( (line=reader.readLine()) != null ) {
				NUrbansimPersons++ ;
				String[] parts = line.split("[\t\n]+");

				Id personId = new IdImpl( parts[idxFromKey.get("person_id")] ) ;
				PersonImpl newPerson = new PersonImpl( personId ) ;

				if ( !( flag || MatsimRandom.getRandom().nextDouble() < samplingRate || (oldPop.getPersons().get( personId))!=null ) ) {
					continue ;
				}
				flag = false ;

				Id homeParcelId = new IdImpl( parts[idxFromKey.get("parcel_id_home")] ) ;
				Location homeLocation = facilities.getFacilities().get( homeParcelId ) ;
				if ( homeLocation==null ) {
					log.warn( "homeLocation==null; personId: " + personId + " parcelId: " + homeParcelId + ' ' + this ) ;
					continue ;
				}
				Coord homeCoord = homeLocation.getCoord() ;
				if ( homeCoord==null ) {
					log.warn( "homeCoord==null; personId: " + personId + " parcelId: " + homeParcelId + ' ' + this ) ;
					continue ;
				}

				PlanImpl plan = newPerson.createAndAddPlan(true);
				plan.setSelected(true) ;
				Utils.makeHomePlan(plan, homeCoord) ;

				int idx = idxFromKey.get("parcel_id_work") ;
				if ( parts[idx].equals("-1") ) {
					newPerson.setEmployed(Boolean.FALSE);
				} else {
					newPerson.setEmployed(Boolean.TRUE);
					Id workParcelId = new IdImpl( parts[idx] ) ;
					Location jobLocation = facilities.getFacilities().get( workParcelId ) ;
					if ( jobLocation == null ) {
						if ( jobLocationIdNullCnt < 1 ) {
							jobLocationIdNullCnt++ ;
							log.warn( "jobLocationId==null, probably out of area. person_id: " + personId
									+ " workp_prcl_id: " + workParcelId + Gbl.ONLYONCE ) ;
						}
						flag = true ;
						continue ;
					}
					Coord workCoord = jobLocation.getCoord() ;
					Utils.completePlanToHwh(plan, workCoord) ;
				}

				// at this point, we have a full "new" person.  Now check against pre-existing population ...

				while ( true ) { // loop from which we can "break":
					Person oldPerson ;
					if ( oldPop==null ) { // no pre-existing population.  Accept:
						newPop.addPerson(newPerson) ;
						break ;
					} else if ( (oldPerson=oldPop.getPersons().get(personId))==null ) { // did not find person.  Put in backup:
						backupPop.addPerson( newPerson) ;
						notFoundCnt++ ;
						break ;
					} else if ( ((PersonImpl) oldPerson).isEmployed() != newPerson.isEmployed() ) { // employment status changed.  Accept new person:
						newPop.addPerson(newPerson) ;
						break ;
					}
					Activity oldHomeAct = ((PlanImpl) oldPerson.getSelectedPlan()).getFirstActivity();
					Activity newHomeAct =    ((PlanImpl) newPerson.getSelectedPlan()).getFirstActivity() ;
					if ( actHasChanged ( oldHomeAct, newHomeAct, network ) ) { // act changed.  Accept new person:
						newPop.addPerson(newPerson) ;
						break ;
					}

					// check if new person works
					if ( !newPerson.isEmployed() ) { // person does not move; doesn't matter.  TODO fix this when other activities are considered
						newPop.addPerson(newPerson) ;
						break ;
					}

					// check if work act has changed:
					ActivityImpl oldWorkAct = (ActivityImpl) oldPerson.getSelectedPlan().getPlanElements().get(2) ;
					ActivityImpl newWorkAct = (ActivityImpl)    newPerson.getSelectedPlan().getPlanElements().get(2) ;
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

		log.info(" samplingRate: " + samplingRate + " oldPopSize: " + oldPop.getPersons().size() + " newPopSize: " + newPop.getPersons().size()
				+ " bakPopSize: " + backupPop.getPersons().size() + " NUrbansimPersons: " + NUrbansimPersons ) ;
		log.warn("why is bakPopSize not approx as large as samplingRate*NUrbansimPersons?" ) ;

		List<Person> bakPersons = new ArrayList<Person>( backupPop.getPersons().values() ) ; // Population data structure not needed!
		Collections.shuffle( bakPersons ) ;
		for ( Person person : bakPersons ) {
			if ( newPop.getPersons().size() >= samplingRate*NUrbansimPersons ) {
				break ;
			}
			newPop.addPerson( person ) ;
		}

		log.info(" samplingRate: " + samplingRate + " oldPopSize: " + oldPop.getPersons().size() + " newPopSize: " + newPop.getPersons().size()
				+ " bakPopSize: " + backupPop.getPersons().size() + " NUrbansimPersons: " + NUrbansimPersons ) ;

		log.info( "Done with reading persons." ) ;
	}

	private boolean actHasChanged ( final Activity oldAct, final Activity newAct, final NetworkLayer network ) {
		if ( !oldAct.getCoord().equals( newAct.getCoord() ) ) {
//			log.info( "act location changed" ) ;
			return true ;
		}
		if ( oldAct.getLinkId()==null || network.getLinks().get(oldAct.getLinkId()) == null ) { // careful: only the old activity has a link
//			log.info( "act link does not exist any more" ) ;
			return true ;
		}
		return false ;
	}

}
