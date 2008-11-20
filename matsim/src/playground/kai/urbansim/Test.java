package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.ScenarioData;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.ExeRunner;
import org.matsim.world.Location;


/**
 * Class that is meant to interface with urbansim.  As of nov08, only working for the urbansim "parcel"
 * models: Those models output persons with jobId, so the h2w-connection can be taken from urbansim.
 * This class then just produces hwh acts and starts iterating them.
 * 
 * Not all "parcel" models work, however.  For example, seattle_parcel in oct08 seemed to be "cut out" from
 * psrc_parcel, and care was not taken to make sure that the JobIds in the persons still point to something.
 * 
 * @author nagel
 *
 */
public class Test {
	private static final Logger log = Logger.getLogger(Test.class);
	
	private static final int U_CELL = 0 ;
	private static final int U_PARCEL = 1 ;
	private static final int U_MODEL_TYPE = U_PARCEL ; // configure!! (as of nov08, does NOT work for U_CELL)

	public static void main ( String[] args ) {
		log.info("Starting the matsim run from the urbansim interface.  This looks a little rough initially since 'normal' matsim" ) ;
		log.info("is not entered until later (after 'DONE with demand generation from urbansim')." ) ;
		
		ReadFromUrbansim readFromUrbansim ;
		if ( U_MODEL_TYPE==U_CELL ) {
//			readFromUrbansim = new ReadFromUrbansimCellModel() ;
		} else if ( U_MODEL_TYPE==U_PARCEL ) {
			readFromUrbansim = new ReadFromUrbansimParcelModel() ;
		} else {
			log.fatal("not implemented" ) ;	System.exit(-1);
		}
		
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		Facilities facilities = new Facilities("urbansim locations (gridcells _or_ parcels _or_ ...)", Facilities.FACILITIES_NO_STREAMING) ;
		readFromUrbansim.readFacilities( facilities ) ;

//		FacilitiesWriter facWriter = new FacilitiesWriter(facilities,ReadFromUrbansim.PATH_TO_OPUS_MATSIM+"tmp/locations.xml.gz") ;
//		facWriter.write();
		
		// read urbansim persons (possibly indirectly, e.g. via households).  Generates hwh acts as side effect
		Population population = new Population(Population.NO_STREAMING);
		readFromUrbansim.readPersons( population, facilities, 0.01 ) ;
				
//		PopulationWriter popWriter = new PopulationWriter(population,ReadFromUrbansim.PATH_TO_OPUS_MATSIM+"tmp/pop.xml.gz","v4",1) ;
//		popWriter.write();

		System.out.println("### DONE with demand generation from urbansim ###") ;
		
		Config config = Gbl.createConfig(args);
		ScenarioData scenarioData = new ScenarioData(config) ;
		NetworkLayer network = scenarioData.getNetwork() ;

		Controler controler = new Controler(config,network,population) ;
		controler.setOverwriteFiles(true) ;
		controler.run() ;

	}
}
