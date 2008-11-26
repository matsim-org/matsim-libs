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
import org.matsim.network.Node;
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
import org.matsim.world.Layer;
import org.matsim.world.Location;

/**
 * Class that is meant to interface with urbansim.  As of nov08, only working for the urbansim "parcel"
 * models: Those models output persons with jobId, so the h2w-connection can be taken from urbansim.
 * This class then just produces hwh acts and starts iterating them.
 * 
 * Not all "parcel" models work, however.  For example, seattle_parcel in oct08 seemed to be "cut out" from
 * psrc_parcel, and care was not taken to make sure that the JobIds in the persons still point to something.
 * Or maybe I made a mistake on my side.
 * 
 * @author nagel
 *
 */
public class Test {
	private static final Logger log = Logger.getLogger(Test.class);
	
//	private static final int U_CELL = 0 ;
	private static final int U_PARCEL = 1 ;
	private static final int U_MODEL_TYPE = U_PARCEL ; // configure!! (as of nov08, does NOT work for U_CELL)

	public static void main ( String[] args ) {
		log.info("Starting the matsim run from the urbansim interface.  This looks a little rough initially since 'normal' matsim" ) ;
		log.info("is not entered until later (after 'DONE with demand generation from urbansim')." ) ;

		// parse the config arguments so we have a config.  generate scenario data from this
		Config config = Gbl.createConfig(args);
		ScenarioData scenarioData = new ScenarioData(config) ;

		// get the network.  Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkLayer network = scenarioData.getNetwork() ;

		log.info("") ;
		log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		log.info("... finished cleaning network.") ;
		log.info("") ;

		// define which urbansim reader to use.  Hard-coded right now to facilitate cross-linking of code.
//		ReadFromUrbansim readFromUrbansim ;
		ReadFromUrbansimParcelModel readFromUrbansim ;
		if ( U_MODEL_TYPE==U_PARCEL ) {
			readFromUrbansim = new ReadFromUrbansimParcelModel() ;
//		} else if ( U_MODEL_TYPE==U_CELL ) {
//			readFromUrbansim = new ReadFromUrbansimCellModel() ;
		} else {
			log.fatal("not implemented" ) ;	System.exit(-1);
		}
		
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		Facilities facilities = new Facilities("urbansim locations (gridcells _or_ parcels _or_ ...)", Facilities.FACILITIES_NO_STREAMING) ;
		readFromUrbansim.readFacilities( facilities ) ;

//		FacilitiesWriter facWriter = new FacilitiesWriter(facilities,ReadFromUrbansim.PATH_TO_OPUS_MATSIM+"tmp/locations.xml.gz") ;
//		facWriter.write();
		
		// read urbansim persons (possibly indirectly, e.g. via households).  Generates hwh acts as side effect
		Population oldPop = scenarioData.getPopulation() ;
		Population newPop = new Population(Population.NO_STREAMING);
		readFromUrbansim.readPersons( oldPop, newPop, facilities, network, 0.01 ) ;
		oldPop=null ;
		System.gc() ;
				
		PopulationWriter popWriter = new PopulationWriter(newPop,ReadFromUrbansim.PATH_TO_OPUS_MATSIM+"tmp/pop.xml.gz","v4",1) ;
		popWriter.write();
		
		log.info("BEGIN constructing urbansim zones.") ;
		Facilities zones = new Facilities("urbansim zones", Facilities.FACILITIES_NO_STREAMING) ;
		readFromUrbansim.readZones( zones, facilities ) ;
		log.info("DONE with constructing urbansim zones.") ;

		System.out.println("### DONE with demand generation from urbansim ###") ;
		System.gc() ;
		
		config.controler().setOutputDirectory(ReadFromUrbansim.PATH_TO_OPUS_MATSIM+"output") ;
		log.warn("matsim output path set to fixed value to make sure that it is at correct place for feedback to urbansim");

		Controler controler = new Controler(config,network,newPop) ;
		controler.setOverwriteFiles(true) ;

		MyControlerListener myControlerListener = new MyControlerListener( zones ) ;
		controler.addControlerListener(myControlerListener);

		controler.run() ;

//		for ( Iterator it = zones.getFacilities().values().iterator(); it.hasNext(); ) {
//			Facility fromZone = (Facility) it.next();
//			Node fromNode = network.getNearestNode(fromZone.getCenter()) ; 
//			for ( Iterator it2 = zones.getFacilities().values().iterator(); it.hasNext(); ) {
//				Facility toZone = (Facility) it.next();
//				Node toNode = network.getNearestNode( toZone.getCenter() ) ;
//			}
//		}
	}
}
