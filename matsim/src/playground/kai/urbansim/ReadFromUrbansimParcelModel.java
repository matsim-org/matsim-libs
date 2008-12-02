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
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.geometry.Coord;
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
	
	private final String parcelfile = "tmp/parcels-cleaned.tab" ;
	
	/* (non-Javadoc)
	 * @see playground.kai.urbansim.ReadFromUrbansim#readFacilities(org.matsim.facilities.Facilities)
	 */
	public void readFacilities(Facilities facilities) {
		// (these are simply defined as those entities that have x/y coordinates in urbansim)
		try {
			// FIXME: hack to remove unparsable ^M characters from urbansim output
			log.error("My example of parcels.tab contains ^M characters, which screws up the java readline().") ;
			log.error("The following attempts to run a hack to solve this problem.  But it has many hard-coded pathnames in it,");
			log.error("and it probably only works on unix.") ;
			ExeRunner.run("sh -e "+PATH_TO_OPUS_MATSIM+"bin/remove_weird_characters.sh", PATH_TO_OPUS_MATSIM+"tmp/remove_weird_characters.log", 3600) ;

			String filename = PATH_TO_OPUS_MATSIM+parcelfile ;
			log.info( "Starting to read urbansim parcels from " + filename ) ;
			
			BufferedReader reader = IOUtils.getBufferedReader( filename ) ;

			String line = reader.readLine() ;
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( line ) ;

			while ( (line = reader.readLine()) != null ) {
				String[] parts = line.split("[\t]+");

				Id id = new IdImpl( parts[idxFromKey.get("parcel_id:i4")] ) ;

				Coord coord = new CoordImpl( parts[idxFromKey.get("x_coord_sp:f4")],parts[idxFromKey.get("y_coord_sp:f4")] ) ;

				Facility facility = facilities.createFacility(id,coord) ;
				facility.setDesc("urbansim location") ;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info( "DONE with reading urbansim parcels" ) ;
	}

	class PseudoZone {
		double sumx = 0. ;
		double sumy = 0. ;
		long cnt = 0 ;
	}
	
	public void readZones ( Facilities zones, Layer parcels ) {
		Map<Id,Id> zoneFromParcel = new HashMap<Id,Id>() ;
		Utils.readKV( zoneFromParcel,"parcel_id:i4", new ParcelIdFactory(), "zone_id:i4", new ZoneIdFactory() ,
				PATH_TO_OPUS_MATSIM+parcelfile ) ;
		
		// summing the coordinates of all participating parcels into the zones
		Map<Id,PseudoZone> pseudoZones = new HashMap<Id,PseudoZone>() ;
		for ( Entry<Id,Id> entry : zoneFromParcel.entrySet() ) {
			ParcelId parcelId = (ParcelId) entry.getKey();
			ZoneId   zoneId   = (ZoneId)   entry.getValue() ;

			Location parcel = parcels.getLocation(parcelId) ;
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

	private static long foundCnt = 0 ;
	private static long notFoundCnt = 0 ;
	/* (non-Javadoc)
	 * @see playground.kai.urbansim.ReadFromUrbansim#readPersons(org.matsim.basic.v01.BasicPopulation, org.matsim.facilities.Facilities, double)
	 */
	public void readPersons(Population oldPop, Population newPop, Facilities facilities, NetworkLayer network, double fraction) {
		// to get from buildings to locations (which have coordinates) ...
		Map<Id,Id> locationFromBuilding = new HashMap<Id,Id>() ;
		Utils.readKV( locationFromBuilding, "building_id:i4", new BldIdFactory(), "parcel_id:i4", new LocationIdFactory(), 
				PATH_TO_OPUS_MATSIM+"tmp/buildings.tab" ) ;

		// to get from households to buildings ...
		Map<Id,Id> buildingFromHousehold = new HashMap<Id,Id>() ;
		Utils.readKV( buildingFromHousehold, "household_id:i4", new HHIdFactory(), "building_id:i4", new BldIdFactory(), 
				PATH_TO_OPUS_MATSIM+"tmp/households.tab" ) ;
		
		// similarly, to get from jobs to buildings ...
		Map<Id,Id> buildingFromJob = new HashMap<Id,Id>() ;
		Utils.readKV( buildingFromJob, "job_id:i4", new JobIdFactory(), "building_id:i4", new BldIdFactory(), 
				PATH_TO_OPUS_MATSIM+"tmp/jobs.tab" ) ;
		
//		MatsimRandom.random.setSeed(4711) ; // fix seed so that the persons are always the same (not really needed any more)

		try {
			String filename = PATH_TO_OPUS_MATSIM+"tmp/persons.tab" ;
			log.info( "Starting to read persons from " + filename ) ;
			BufferedReader reader = IOUtils.getBufferedReader( filename );

			String line = reader.readLine();
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( line ) ;

			// We consider two cases:
			// (1) We have an old population.  Then we look for those people who have the same id.  We assume that the old pop 
			//     has the correct size.
			// (2) We do not have an old population.  Then we draw randomly.

			long homeParcelIdNullCnt = 0 ;
			long jobBuildingIdNullCnt = 0 ;
			boolean flag = false ;
			while ( (line=reader.readLine()) != null ) {

				// if there is no pre-existing population, check for the fraction:
				if ( oldPop==null && MatsimRandom.random.nextDouble() > fraction && !flag ) {
					continue ;
				}
				flag = false ;

				// construct the person id
				String[] parts = line.split("[\t\n]+");

				int idx = idxFromKey.get("person_id:i4") ;
				Id personId = new IdImpl( parts[idx] ) ;

				// if there is a pre-existing pop, check if the person is in there:
				Person oldPerson = null ;
				if ( oldPop!=null && (oldPerson=oldPop.getPerson(personId))==null ) {
					if ( notFoundCnt == 0 ) {
						notFoundCnt++ ; log.info("Person from urbansim NOT found in pre-exising pop file." 
								+ " This is normal if you are running matsim on a sample. " + Gbl.ONLYONCE ) ;
					}
					continue ;
				}
				if ( oldPop!=null && foundCnt == 0 ) {
					foundCnt++ ; log.info("Found person from urbansim in pre-existing pop file." + Gbl.ONLYONCE ) ;
				}

				// continue constructing the new person.  If there is a pre-existing pop, we need it to look for changes
				Person person = new PersonImpl( personId ) ;

				person.setAge( Integer.parseInt( parts[idxFromKey.get("age:i4")] ) ) ;

				HHId hhId = new HHId( parts[idxFromKey.get("household_id:i4")] ) ; 
				BldId buildingId = (BldId) buildingFromHousehold.get( hhId ) ;
				assert( buildingId != null ) ;
				LocationId homeParcelId = (LocationId) locationFromBuilding.get( buildingId ) ;
				if ( homeParcelId==null ) {
					if ( homeParcelIdNullCnt < 1 ) {
						homeParcelIdNullCnt++ ; 
						log.warn( "homeParcelId==null; personId: " + personId.toString() + " hhId: " + hhId.toString() 
								+ " buildingId: " + buildingId.toString() + ' ' + this ) ; log.info(Gbl.ONLYONCE) ;
					}
					continue ;
				}
				Location homeLocation = facilities.getLocation( homeParcelId ) ;
				if ( homeLocation==null ) {
					log.warn( "homeLocation==null; personId: " + personId + " hhId: " + hhId 
							+ " buildingId: " + buildingId + " parcelId: " + homeParcelId + ' ' + this ) ;
					continue ;
				}
				Coord homeCoord = homeLocation.getCenter() ;
				if ( homeCoord==null ) {
					log.warn( "homeCoord==null; personId: " + personId.toString() + " hhId: " + hhId.toString() 
							+ " buildingId: " + buildingId.toString() + this ) ;
					continue ;
				}
				
				Plan plan = person.createPlan(true);
				plan.setSelected(true) ;
				Utils.makeHomePlan(plan, homeCoord) ;

				idx = idxFromKey.get("job_id:i4") ;
				if ( parts[idx].equals("-1") ) {
					person.setEmployed("no") ;
				} else {
					person.setEmployed("yes") ;
					JobId jobId = new JobId( parts[idx] ) ;
					buildingId = (BldId) buildingFromJob.get( jobId ) ;
					if ( buildingId == null ) {
						if ( jobBuildingIdNullCnt < 1 ) {
							jobBuildingIdNullCnt++ ;
							log.warn( "jobBuildingId==null, probably out of area. person_id: " + personId.toString() 
									+ " job_id: " + jobId.toString() + Gbl.ONLYONCE ) ;
							log.info("(Will re-try until I find someone whoose job is in the area.)") ;
						}
						flag = true ; continue ;
					}
					assert( buildingId != null ) ;
					LocationId jobParcelId = (LocationId) locationFromBuilding.get( buildingId ) ;
					assert( jobParcelId != null ) ;
					Location jobLocation = facilities.getLocation( jobParcelId ) ;
					assert( jobLocation != null ) ;
					Coord workCoord = jobLocation.getCenter() ;
					
					Utils.completePlanToHwh(plan, workCoord) ;
				}

				// at this point, we have a full "new" person.  
				if ( oldPop==null ) {
					// W/o a pre-existing population, just add it:
					newPop.addPerson(person) ;
				} else {
					// otherwise, compare if something has changed:
					while ( true ) { // loop from which we can "break"
						if ( !oldPerson.getEmployed().equals( person.getEmployed() ) ) {
//							log.info("employment status changed") ;
							break ;
						}
						Act oldHomeAct = oldPerson.getSelectedPlan().getFirstActivity();  // FIXME: awkward
						Act newHomeAct =    person.getSelectedPlan().getFirstActivity() ; // FIXME: awkward
						if ( actHasChanged ( oldHomeAct, newHomeAct, network ) ) {
//							log.info( "something has changed with home act" ) ;
							break ;
						}
						if ( oldPerson.getEmployed().equals("no") ) {
							// nothing more to test; use old person
							person = oldPerson ; // TODO: means that age does not increase
							break ;
						}

						Act oldWorkAct = (Act) oldPerson.getSelectedPlan().getActsLegs().get(2) ; // FIXME: awkward
						Act newWorkAct = (Act)    person.getSelectedPlan().getActsLegs().get(2) ; // FIXME: awkward
						if ( actHasChanged ( oldWorkAct, newWorkAct, network ) ) {
//							log.info( "something has changed with work act" ) ;
							break ;
						}

						person = oldPerson ; // TODO: means that age does not increase
						break ;
					}

					// add person:
//					log.info("adding person") ;
					newPop.addPerson(person) ;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
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
