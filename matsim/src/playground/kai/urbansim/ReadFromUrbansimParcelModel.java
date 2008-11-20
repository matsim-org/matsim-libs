/**
 * 
 */
package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.ExeRunner;
import org.matsim.world.Location;

/**
 * @author nagel
 *
 */
public class ReadFromUrbansimParcelModel implements ReadFromUrbansim {
	private static final Logger log = Logger.getLogger(ReadFromUrbansimParcelModel.class);

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
			ExeRunner.run("sh "+PATH_TO_OPUS_MATSIM+"bin/remove_weird_characters.sh", PATH_TO_OPUS_MATSIM+"tmp/remove_weird_characters.log", 3600) ;

			BufferedReader reader = IOUtils.getBufferedReader(PATH_TO_OPUS_MATSIM+"tmp/parcels-cleaned.tab" ) ;

			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( header ) ;

			String line = reader.readLine() ;
			while ( line != null ) {

				String[] parts = line.split("[\t]+");

				int idx_id = idxFromKey.get("parcel_id:i4") ;
				Id id = new IdImpl( parts[idx_id] ) ;

				int idx_x = idxFromKey.get("x_coord_sp:f4") ;
				int idx_y = idxFromKey.get("y_coord_sp:f4") ;
				Coord coord = new CoordImpl( parts[idx_x], parts[idx_y] ) ;

				Facility facility = facilities.createFacility(id,coord) ;
				facility.setDesc("urbansim location") ;
				
				line = reader.readLine() ;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see playground.kai.urbansim.ReadFromUrbansim#readPersons(org.matsim.basic.v01.BasicPopulation, org.matsim.facilities.Facilities, double)
	 */
	public void readPersons(Population population, Facilities facilities, double fraction) {
		// to get from households to buildings ...
		Map<Id,Id> buildingFromHousehold = new HashMap<Id,Id>() ;
		Utils.readKV( buildingFromHousehold, "household_id:i4", new HHIdBuilder(), "building_id:i4", new BldIdBuilder(), 
				PATH_TO_OPUS_MATSIM+"tmp/households.tab" ) ;
		
		// similarly, to get from jobs to buildings ...
		Map<Id,Id> buildingFromJob = new HashMap<Id,Id>() ;
		Utils.readKV( buildingFromJob, "job_id:i4", new JobIdBuilder(), "building_id:i4", new BldIdBuilder(), 
				PATH_TO_OPUS_MATSIM+"tmp/jobs.tab" ) ;

		// to get from buildings to locations (which have coordinates) ...
		Map<Id,Id> locationFromBuilding = new HashMap<Id,Id>() ;
		Utils.readKV( locationFromBuilding, "building_id:i4", new BldIdBuilder(), "parcel_id:i4", new LocationIdBuilder(), 
				PATH_TO_OPUS_MATSIM+"tmp/buildings.tab" ) ;


		try {
			BufferedReader reader = IOUtils.getBufferedReader(PATH_TO_OPUS_MATSIM+"tmp/persons.tab");

			String header = reader.readLine();
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( header ) ;

			String line = reader.readLine();

//			while (line != null) {
			while ( (line=reader.readLine()) != null ) {
				String[] parts = line.split("[\t\n]+");

				if ( Math.random() > fraction ) {
//					line = reader.readLine(); // next line
					continue ;
				}

				int idx = idxFromKey.get("person_id:i4") ;
				Id personId = new IdImpl( parts[idx] ) ;

				Person person = new PersonImpl( personId ) ;

				idx = idxFromKey.get("age:i4") ;
				person.setAge( Integer.parseInt( parts[idx] ) ) ;

				idx = idxFromKey.get("household_id:i4") ;
				HHId hhId = new HHId( parts[idx] ) ; 
				BldId buildingId = (BldId) buildingFromHousehold.get( hhId ) ;
				assert( buildingId != null ) ;
				LocationId homeParcelId = (LocationId) locationFromBuilding.get( buildingId ) ;
				if ( homeParcelId==null ) {
					log.warn( " personId: " + personId.toString() + " hhId: " + hhId.toString() + " buildingId: " + buildingId.toString() ) ;
					continue ;
				}
				assert( homeParcelId != null ) ;
				Location homeLocation = facilities.getLocation( homeParcelId ) ;
				assert( homeLocation != null ) ;
				Coord homeCoord = homeLocation.getCenter() ;

				// add person only after it's clear that he/she has a home location:
				population.addPerson(person) ;

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
						System.err.println ( " person_id: " + personId.toString() + " job_id: " + jobId.toString() ) ;
						continue ;
					}
					assert( buildingId != null ) ;
					LocationId jobParcelId = (LocationId) locationFromBuilding.get( buildingId ) ;
					assert( jobParcelId != null ) ;
					Location jobLocation = facilities.getLocation( jobParcelId ) ;
					assert( jobLocation != null ) ;
					Coord workCoord = jobLocation.getCenter() ;
					
					Utils.completePlanToHwh(plan, workCoord) ;

				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
