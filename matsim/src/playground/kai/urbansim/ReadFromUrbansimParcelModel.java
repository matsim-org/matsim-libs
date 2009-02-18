/**
 * 
 */
package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.ExeRunner;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.ZoneLayer;

import playground.kai.urbansim.ids.BldId;
import playground.kai.urbansim.ids.BldIdFactory;
import playground.kai.urbansim.ids.HHId;
import playground.kai.urbansim.ids.HHIdFactory;
import playground.kai.urbansim.ids.IdFactory;
import playground.kai.urbansim.ids.JobId;
import playground.kai.urbansim.ids.JobIdFactory;
import playground.kai.urbansim.ids.LocationId;
import playground.kai.urbansim.ids.LocationIdFactory;
import playground.kai.urbansim.ids.ParcelId;
import playground.kai.urbansim.ids.ParcelIdFactory;
import playground.kai.urbansim.ids.PersonIdFactory;
import playground.kai.urbansim.ids.ZoneId;
import playground.kai.urbansim.ids.ZoneIdFactory;

/**
 * @author nagel
 *
 */
public class ReadFromUrbansimParcelModel {
	private static final Logger log = Logger.getLogger(ReadFromUrbansimParcelModel.class);
	
	private final String PATH_TO_OPUS_MATSIM = Matsim4Urbansim.PATH_TO_OPUS_MATSIM ;
	
	private int year ;
	
	public ReadFromUrbansimParcelModel ( int year ) {
		this.year = year ;
	}
	
	public void readFacilities(Facilities parcels, Facilities zones) {
		// (these are simply defined as those entities that have x/y coordinates in urbansim)
		String filename = PATH_TO_OPUS_MATSIM+"tmp/parcel__dataset_table__exported_indicators__" + year + ".tab" ;
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

				Facility facility = parcels.createFacility(parcelId,coord) ;
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
	
	public void constructZones ( Facilities parcels, Facilities zones, Map<Id,Id> zoneFromParcel ) {
		
		// summing the coordinates of all participating parcels into the zones
		Map<Id,PseudoZone> pseudoZones = new HashMap<Id,PseudoZone>() ;
		for ( Entry<Id,Id> entry : zoneFromParcel.entrySet() ) {
			Id       parcelId =            entry.getKey();
			ZoneId   zoneId   = (ZoneId)   entry.getValue() ;

			Location parcel = parcels.getLocation(parcelId) ;
			assert( parcel!= null ) ;
			Coord coord = parcel.getCenter();
			
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

	public void readPersons(Population oldPop, Population newPop, Facilities facilities, NetworkLayer network, double samplingRate ) {
		String filename = PATH_TO_OPUS_MATSIM+"tmp/person__dataset_table__exported_indicators__" + year + ".tab" ;
		log.info( "Starting to read persons from " + filename ) ;

		Population backupPop = new Population() ;
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
				Person newPerson = new PersonImpl( personId ) ;
				
				if ( !( flag || MatsimRandom.random.nextDouble() < samplingRate || (oldPop.getPerson( personId))!=null ) ) {
					continue ;
				}
				flag = false ;

				Id homeParcelId = new IdImpl( parts[idxFromKey.get("parcel_id_home")] ) ;
				Location homeLocation = facilities.getLocation( homeParcelId ) ;
				if ( homeLocation==null ) {
					log.warn( "homeLocation==null; personId: " + personId + " parcelId: " + homeParcelId + ' ' + this ) ;
					continue ;
				}
				Coord homeCoord = homeLocation.getCenter() ;
				if ( homeCoord==null ) {
					log.warn( "homeCoord==null; personId: " + personId + " parcelId: " + homeParcelId + ' ' + this ) ;
					continue ;
				}

				Plan plan = newPerson.createPlan(true);
				plan.setSelected(true) ;
				Utils.makeHomePlan(plan, homeCoord) ;

				int idx = idxFromKey.get("parcel_id_work") ;
				if ( parts[idx].equals("-1") ) {
					newPerson.setEmployed("no") ;
				} else {
					newPerson.setEmployed("yes") ;
					Id workParcelId = new IdImpl( parts[idx] ) ;
					Location jobLocation = facilities.getLocation( workParcelId ) ;
					if ( jobLocation == null ) {
						if ( jobLocationIdNullCnt < 1 ) {
							jobLocationIdNullCnt++ ;
							log.warn( "jobLocationId==null, probably out of area. person_id: " + personId
									+ " workp_prcl_id: " + workParcelId + Gbl.ONLYONCE ) ;
						}
						flag = true ;
						continue ;
					}
					Coord workCoord = jobLocation.getCenter() ;
					Utils.completePlanToHwh(plan, workCoord) ;
				}
				
				// at this point, we have a full "new" person.  Now check against pre-existing population ...

				while ( true ) { // loop from which we can "break":
					Person oldPerson ;
					if ( oldPop==null ) { // no pre-existing population.  Accept:
						newPop.addPerson(newPerson) ;
						break ;
					} else if ( (oldPerson=oldPop.getPerson(personId))==null ) { // did not find person.  Put in backup: 
						backupPop.addPerson( newPerson) ;
						notFoundCnt++ ;
						break ;
					} else if ( !oldPerson.getEmployed().equals( newPerson.getEmployed() ) ) { // employment status changed.  Accept new person:
						newPop.addPerson(newPerson) ;
						break ;
					}
					Act oldHomeAct = oldPerson.getSelectedPlan().getFirstActivity();  
					Act newHomeAct =    newPerson.getSelectedPlan().getFirstActivity() ; 
					if ( actHasChanged ( oldHomeAct, newHomeAct, network ) ) { // act changed.  Accept new person:
						newPop.addPerson(newPerson) ;
						break ;
					} 

					// check if new person works 
					if ( newPerson.getEmployed().equals("no") ) { // person does not move; doesn't matter.  TODO fix this when other activities are considered
						newPop.addPerson(newPerson) ;
						break ;
					}

					// check if work act has changed:
					Act oldWorkAct = (Act) oldPerson.getSelectedPlan().getActsLegs().get(2) ; 
					Act newWorkAct = (Act)    newPerson.getSelectedPlan().getActsLegs().get(2) ; 
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

		log.info(" samplingRate: " + samplingRate + " oldPopSize: " + oldPop.size() + " newPopSize: " + newPop.size()
				+ " bakPopSize: " + backupPop.size() + " NUrbansimPersons: " + NUrbansimPersons ) ;
		log.warn("why is bakPopSize not approx as large as samplingRate*NUrbansimPersons?" ) ;

		List<Person> bakPersons = new ArrayList<Person>( backupPop.getPersons().values() ) ; // Population data structure not needed!
		Collections.shuffle( bakPersons ) ;
		for ( Person person : bakPersons ) {
			if ( newPop.size() >= samplingRate*NUrbansimPersons ) {
				break ;
			}
			newPop.addPerson( person ) ;
		}

		log.info(" samplingRate: " + samplingRate + " oldPopSize: " + oldPop.size() + " newPopSize: " + newPop.size()
				+ " bakPopSize: " + backupPop.size() + " NUrbansimPersons: " + NUrbansimPersons ) ;

		log.info( "Done with reading persons." ) ;
	}
	
	private boolean actHasChanged ( Act oldAct, Act newAct, NetworkLayer network ) {
		if ( !oldAct.getCoord().equals( newAct.getCoord() ) ) {
//			log.info( "act location changed" ) ;
			return true ;
		}
		if ( oldAct.getLinkId()==null || network.getLink( oldAct.getLinkId() ) == null ) { // careful: only the old activity has a link
//			log.info( "act link does not exist any more" ) ;
			return true ;
		}
		return false ;
	}

}
