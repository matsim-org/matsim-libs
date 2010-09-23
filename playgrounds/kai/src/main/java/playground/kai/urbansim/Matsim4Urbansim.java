package playground.kai.urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;

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
	 * Note that OPUS_HOME needs to be set; in eclipse as part of the run dialog.
	 * When called from urbansim, this is hopefully always set.
	 */
	public static final String PATH_TO_OPUS_MATSIM = System.getenv("OPUS_HOME")+'/' +"opus_matsim/" ;

	public static void main ( final String[] args ) {
		log.info("Starting the matsim run from the urbansim interface.  This looks a little rough initially since 'normal' matsim" ) ;
		log.info("is not entered until later (after 'DONE with demand generation from urbansim')." ) ;

		int year = 0 ;
		double samplingRate = 0.01 ;
		for ( int ii=1 ; ii<args.length ; ii++ ) { //args[0] is the config file
			log.info( " ii: " + ii + " args[ii]: " + args[ii] ) ;
			String[] parts = args[ii].split("=");
			if ( parts[0].equals("--year") ) {
				year = Integer.parseInt( parts[1] ) ;
			} else if ( parts[0].equals("--samplingRate") ) {
				samplingRate = Double.parseDouble( parts[1] ) ;
			}
		}
		log.info("year: " + year + " samplingRate: " + samplingRate ) ;
		for ( int ii=args.length-1 ; ii>=1 ; ii-- ) {
			args[ii] = "" ;
		}

		// parse the config arguments so we have a config.  generate scenario data from this
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(args[0]);
		Config config = loader.getScenario().getConfig();
		loader.loadScenario();
		ScenarioImpl scenarioData = loader.getScenario();


		// get the network.  Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkImpl network = scenarioData.getNetwork() ;

		log.info("") ;
		log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		log.info("... finished cleaning network.") ;
		log.info("") ;

		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( year ) ;

		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbansim.readFacilities( facilities, zones ) ;

		new FacilitiesWriter(facilities).write(PATH_TO_OPUS_MATSIM+"tmp/locations.xml.gz") ;

		Population oldPop ;
		if ( config.plans().getInputFile() != null ) {
			log.info("Population specified in matsim config file; assuming WARM start with pre-existing pop file.");
			log.info("Persons not found in pre-existing pop file are added; persons no longer in urbansim persons file are removed." ) ;
			oldPop = scenarioData.getPopulation() ;
			log.info("Note that the `continuation of iterations' will only work if you set this up via different config files for") ;
			log.info(" every year and know what you are doing.") ;
		} else {
			log.warn("No population specified in matsim config file; assuming COLD start.");
			log.info("(I.e. generate new pop from urbansim files.)" );
			oldPop=null ;
		}

		Population newPop = new ScenarioImpl().getPopulation();
		// read urbansim persons.  Generates hwh acts as side effect
		readFromUrbansim.readPersons( oldPop, newPop, facilities, network, samplingRate ) ;
		oldPop=null ;
		System.gc() ;

		new PopulationWriter(newPop,network).write(PATH_TO_OPUS_MATSIM+"tmp/pop.xml.gz");

		log.info("### DONE with demand generation from urbansim ###") ;

		scenarioData.setPopulation(newPop);
		Controler controler = new Controler(scenarioData) ;
		controler.setOverwriteFiles(true) ;

		// The following lines register what should be done _after_ the iterations were run:
		MyControlerListener myControlerListener = new MyControlerListener( zones ) ;
		controler.addControlerListener(myControlerListener);

		// run the iterations, including the post-processing:
		controler.run() ;

	}
}
