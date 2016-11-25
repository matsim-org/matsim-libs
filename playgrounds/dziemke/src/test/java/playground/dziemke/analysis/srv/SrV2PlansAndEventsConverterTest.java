package playground.dziemke.analysis.srv;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import playground.dziemke.analysis.Trip;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * @author GabrielT on 07.11.2016.
 */
public class SrV2PlansAndEventsConverterTest {

	private static final Logger LOG = Logger.getLogger(SrV2PlansAndEventsConverterTest.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
//	private static final String NETWORK_FILE = "../../../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	private static final String TRIPS_FILENAME = "W2008_Berlin_Weekday_Sample.dat";

	@Test
	public void TestConvert() {

		File file = new File("");
		System.out.println(file.getAbsolutePath());

		List<Trip> trips = readTrips(utils.getInputDirectory() + TRIPS_FILENAME);

//		Network network = readNetwork(NETWORK_FILE);

		CoordinateTransformation ct = new IdentityTransformation();

		TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap = createPersonTripsMap(trips);
		SrV2PlansAndEventsConverter.convert(personTripsMap, //network, 
				ct, utils.getOutputDirectory() );

		long checksum_ref = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "correctPlans.xml");
		long checksum_run = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "plans.xml");
		Assert.assertEquals("MATSim plans files are different", checksum_ref, checksum_run);

		checksum_ref = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "correctEvents.xml");
		checksum_run = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "events.xml");
		Assert.assertEquals("MATSim plans files are different", checksum_ref, checksum_run);

//		TODO:
//		use checksum to compare events instead of EventsFileComparator because the events-file does not contain links
//		and the EventsFileComparator throws an exception

//		final String eventsFilenameReference = utils.getInputDirectory() + "correctEvents.xml";
//		final String eventsFilenameNew = utils.getOutputDirectory() + "events.xml";
//		Assert.assertEquals("different event files.", 0, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));

	}

	private static Network readNetwork( String inputFile ) {

		LOG.info("Reading network " + inputFile + ".");
		/* Get network, which is needed to calculate distances */
		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		networkReader.readFile(inputFile);
		LOG.info("Finished reading network.");
		return network;

	}

	private static List<Trip> readTrips( String inputFile ) {

		/* Parse trip file */
		LOG.info("Parsing trips " + inputFile + ".");
		SrV2008TripParser tripParser = new SrV2008TripParser();
		tripParser.parse(inputFile);
		LOG.info("Finished parsing trips.");

		Map<Id<Trip>, Trip> id2tripMap = tripParser.getTrips();
		return new ArrayList<>(id2tripMap.values());
	}

	private static TreeMap<Id<Person>, TreeMap<Double, Trip>> createPersonTripsMap( List<Trip> trips ) {
		TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap = new TreeMap<>();

		for ( Trip trip : trips ) {
			String personId = trip.getPersonId().toString();
			Id<Person> idPerson = Id.create(personId, Person.class);

			if (!personTripsMap.containsKey(idPerson)) {
				personTripsMap.put(idPerson, new TreeMap<>());
			}

			double departureTime_s = trip.getDepartureTime_s();
			if (personTripsMap.get(idPerson).containsKey(departureTime_s)) {
				throw new RuntimeException("Person cannot have two activites ending at the exact same time.");
			} else {
				personTripsMap.get(idPerson).put(departureTime_s, trip);
			}
		}
		return personTripsMap;
	}

}
