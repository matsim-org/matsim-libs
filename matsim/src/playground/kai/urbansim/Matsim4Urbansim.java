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
 * Pathnames: One could use OPUS_HOME.  However, the matsim config system does not know anything about that. 
 * Instead, I assume that matsim is called from OPUS_HOME/opus_matsim
 * 
 *   cd ${OPUS_HOME}/opus_matsim ; java -cp jar/matsim.jar ...
 *   
 * If you want to debug inside eclipse, it makes sense to have OPUS_HOME/opus_matsim also at the root of the eclipse workspace 
 * (e.g. via a symbolic link),
 * so that, from the normal matsim root directory, ../opus_matsim points to OPUS_HOME.  (This is better than having
 * OPUS_HOME itself at the root, since OPUS_HOME may have different names for different people.)
 * 
 * @author nagel
 *
 */
public class Matsim4Urbansim {
	private static final Logger log = Logger.getLogger(Matsim4Urbansim.class);
	
	/**
	 * This path has this weird level of indirection (../opus_matsim) so that the package can also be called from within
	 * matsim if it is linked to the same hierarchy.  Useful for debugging. 
	 */
	public static final String PATH_TO_OPUS_MATSIM = "../opus_matsim/" ;
	
	public static void main ( String[] args ) {
		log.info("Starting the matsim run from the urbansim interface.  This looks a little rough initially since 'normal' matsim" ) ;
		log.info("is not entered until later (after 'DONE with demand generation from urbansim')." ) ;
		
		try { 
			Integer.parseInt( args[1] ) ;
		} catch ( Exception ee ) {
			log.fatal("Something wrong with second argument; should be a year; is: " + args[1] + " Aborting ..." ) ;
		}
		
		// parse the config arguments so we have a config.  generate scenario data from this
		Config config = Gbl.createConfig(args);
		ScenarioData scenarioData = new ScenarioData(config) ;
		
		log.warn("TODO: The following is a hack so that matsim calls at different urbansim years go into different iteration directories.");
		log.warn("      This will almost certainly mess up the `ModuleDisableAfterIteration' settings, so best do not use them.") ;
		log.warn("      (They don't make sense in that context anyways.  If you want to do detailed policy analysis, run matsim separate from urbansim.)") ;
		// (Still, would be better to do something like "ITERS" -> "ITERS.year".)
		int frstIt = config.controler().getFirstIteration();
		int lastIt = config.controler().getLastIteration() ;
		
		int year = Integer.parseInt( args[1] ) ;
		
		config.controler().setFirstIteration( year*1000 + frstIt ) ;  
		config.controler().setLastIteration( year*1000 + lastIt ) ;
		
		// get the network.  Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkLayer network = scenarioData.getNetwork() ;

		log.info("") ;
		log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		log.info("... finished cleaning network.") ;
		log.info("") ;

		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel() ;
		
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		Facilities facilities = new Facilities("urbansim locations (gridcells _or_ parcels _or_ ...)", Facilities.FACILITIES_NO_STREAMING) ;
		readFromUrbansim.readFacilities( facilities ) ;

//		FacilitiesWriter facWriter = new FacilitiesWriter(facilities,PATH_TO_OPUS_MATSIM+"tmp/locations.xml.gz") ;
//		facWriter.write();
		
		Population oldPop ;
		if ( config.plans().getInputFile() != null ) {
			log.warn("Population specified in matsim config file; assuming WARM start.");
			log.info("(I.e. keep only those agents from urbansim files that exist in pre-existing pop file.)");
			oldPop = scenarioData.getPopulation() ;
			log.warn("In spite of 'warm' start this will NOT 'continue' the iterations from one urbansim call to the next. :-(") ;
			log.warn("As of now, will ignore additions to the population (e.g. in-migration).") ;
		} else {
			log.warn("No population specified in matsim config file; assuming COLD start.");
			log.info("(I.e. generate new pop from urbansim files.)" );
			oldPop=null ;
		}
		
		Population newPop = new Population(Population.NO_STREAMING);
		// read urbansim persons (possibly indirectly, e.g. via households).  Generates hwh acts as side effect
		readFromUrbansim.readPersons( oldPop, newPop, facilities, network, 0.01 ) ;
		oldPop=null ;
		System.gc() ;
				
//		PopulationWriter popWriter = new PopulationWriter(newPop,PATH_TO_OPUS_MATSIM+"tmp/pop.xml.gz","v4",1) ;
//		popWriter.write();
		
		log.info("BEGIN constructing urbansim zones.") ;
		Facilities zones = new Facilities("urbansim zones", Facilities.FACILITIES_NO_STREAMING) ;
		readFromUrbansim.readZones( zones, facilities ) ;
		log.info("DONE with constructing urbansim zones.") ;

		System.out.println("### DONE with demand generation from urbansim ###") ;
		System.gc() ;
		
		config.controler().setOutputDirectory(PATH_TO_OPUS_MATSIM+"output") ;
		log.warn("matsim output path set to fixed value to make sure that it is at correct place for feedback to urbansim");

		Controler controler = new Controler(config,network,newPop) ;
		controler.setOverwriteFiles(true) ;

		// The following lines register what should be done _after_ the iterations were run:
		MyControlerListener myControlerListener = new MyControlerListener( zones ) ;
		controler.addControlerListener(myControlerListener);

		// run the iterations, including the postprocessing:
		controler.run() ;

	}
}
