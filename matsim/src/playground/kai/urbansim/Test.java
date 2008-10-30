package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.Location;

public class Test {

	public static final String ACT_HOME = "home" ;
	public static final String ACT_WORK = "work" ;

	static Map<String,Integer> createIdxFromKey( String line ) {
		String[] keys = line.split("[ \t\n]+") ;

		Map<String,Integer> idxFromKey = new HashMap<String, Integer>() ;
		for ( int ii=0 ; ii<keys.length ; ii++ ) {
			idxFromKey.put(keys[ii], ii ) ;
		}
		return idxFromKey ;
	}

	public static void main ( String[] args ) {
		BufferedReader reader;

		// read emme2 network:

		NetworkLayer network = new NetworkLayer() ;
		try {
			reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/net1.out" ) ;

			boolean weAreReadingNodes = true ;
			long linkCnt = 0 ;

			String line = reader.readLine() ;
			while ( line != null ) {
				String[] parts = line.split("[ \t\n]+");

				if ( parts[0].equals("c") ) {
					// is a comment; ignore
				} else if ( parts[0].equals("t") ) {
					if ( parts[1].equals("links") ) {
						weAreReadingNodes = false ;
					} 
				} else if ( parts[0].equals("a") || parts[0].equals("a*") ) {
					if ( weAreReadingNodes ) {
						String idStr = parts[1] ;
						String xxStr = parts[2] ;
						String yyStr = parts[3] ;
						Node node = network.createNode(idStr,xxStr,yyStr,"unknownType") ;
						node.setOrigId(parts[7]);			
					} else {
						String fromNode = parts[1] ;
						String toNode = parts[2] ;
						String length = parts[3] ; // probably miles !!!
						String type = parts[5] ;
						String permlanes = parts[6] ;
						String capacity = parts[8] ;
						String freespeed = parts[9] ; // miles per hours !!!
						String origid = parts[1] + ',' + parts[2] ;
						String id = Long.toString(linkCnt) ; linkCnt++ ;

						Link link = network.createLink(id, fromNode, toNode, length, freespeed, capacity, permlanes, origid, type);
					}
				} else {
					// something else; do nothing
				}

				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		NetworkWriter nwWriter = new NetworkWriter(network,"/home/nagel/tmp/net.xml") ;
		nwWriter.write() ;

		// read urbansim grid cells:

		Facilities gridCells = new Facilities("urbansim grid cells",
				Facilities.FACILITIES_NO_STREAMING) ;

		try {
			reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/gridcells.tab" ) ;

			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			String line = reader.readLine() ;
			while ( line != null ) {
				String[] parts = line.split("[ \t\n]+");

				int idx = idxFromKey.get("grid_id:i4") ;
				String idAsString = parts[idx] ;

				idx = idxFromKey.get("relative_x:i4") ;
				String xxAsString = parts[idx] ;

				idx = idxFromKey.get("relative_y:i4") ;
				String yyAsString = parts[idx];

				Facility facility = gridCells.createFacility(idAsString, xxAsString, yyAsString) ;

//				idx = idxFromKey.get("industrial_sqft:i4") ;
//				double industrialSqft = Double.parseDouble( parts[idx] ) ;

//				idx = idxFromKey.get("industrial_sqft_per_job:i4") ;
//				double industrialSqftPerJob = Double.parseDouble( parts[idx] ) ;

//				double nIndustrialJobs = industrialSqft/industrialSqftPerJob ;

				Activity work = facility.createActivity(ACT_WORK) ;
				work.setCapacity( 0 ) ;

				line = reader.readLine() ;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// read urbansim jobs:

		try {
			reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/jobs.tab" ) ;

			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			String line = reader.readLine() ;
			while ( line != null ) {
				String[] parts = line.split("[ \t\n]+");

				int idx = idxFromKey.get("grid_id:i4") ;
				Id gridId = new IdImpl( parts[idx] ) ;
				Facility grid = (Facility) gridCells.getLocation(gridId) ;
				Activity work = grid.getActivity(ACT_WORK) ;
				work.setCapacity(work.getCapacity()+1) ;

				line = reader.readLine() ;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FacilitiesWriter facWriter = new FacilitiesWriter(gridCells,"/home/nagel/tmp/fac.xml") ;
		facWriter.write();


		Population population = new Population(Population.NO_STREAMING);

//		// read urbansim households and convert them into individual people ... but I don't need that
//		try {
//		reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/households.tab");

//		String header = reader.readLine();
//		Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

//		long counter=0 ;

//		String line = reader.readLine(); {}

//		while (line != null) {
//		String[] parts = line.split("[ \t\n]+");

//		int idx = idxFromKey.get("persons:i4") ;
//		int nPersons = Integer.parseInt( parts[idx] ) ;

//		idx = idxFromKey.get("workers:i4") ;
//		int nWorkers = Integer.parseInt( parts[idx] ) ;

//		idx = idxFromKey.get("cars:i4") ;
//		int nCars = Integer.parseInt( parts[idx] ) ;

//		for ( int ii=0 ; ii<nPersons ; ii++ ) {

//		Person person = new Person(new IdImpl(counter++));

//		idx = idxFromKey.get("age_of_head:i4") ;
//		person.setAge( Integer.parseInt( parts[idx] ) ) ;

//		if ( ii<nWorkers ) {
//		person.setEmployed("worker") ;
//		} else {
//		person.setEmployed(null) ;
//		}

//		if ( ii<nCars ) {
//		person.setCarAvail("car") ;
//		} else {
//		person.setCarAvail(null) ;
//		}

//		Plan plan = person.createPlan(true);
//		plan.setSelected(true) ;

//		idx = idxFromKey.get("grid_id:i4") ;
//		Id gridId = new IdImpl( parts[idx] ) ;
//		Facility facility = (Facility) gridCells.getLocation( gridId ) ;
//		plan.createAct(ACT_HOME, facility) ;

//		plan.createLeg(Mode.car);
//		plan.createAct(ACT_WORK, (Facility) null ) ;

//		plan.createLeg(Mode.car) ;
//		plan.createAct(ACT_HOME, facility ) ;

//		population.addPerson(person);
//		}

//		line = reader.readLine(); // next line
//		}
//		} catch (FileNotFoundException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//		} catch (IOException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//		}



		// read urbansim persons
		try {
			reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/persons.tab");

			String header = reader.readLine();
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			long counter=0 ;

			String line = reader.readLine(); {}

			while (line != null) {
				String[] parts = line.split("[ \t\n]+");

				int idx = idxFromKey.get("person_id:i4") ;
				Id id = new IdImpl( parts[idx] ) ;

				Person person = new Person( id ) ;

				idx = idxFromKey.get("age:i4") ;
				person.setAge( Integer.parseInt( parts[idx] ) ) ;

				idx = idxFromKey.get("employment_status:i4") ;
				person.setEmployed(parts[idx]) ;

				idx = idxFromKey.get("job_id:i4") ;



//				if ( ii<nCars ) {
//					person.setCarAvail("car") ;
//				} else {
//					person.setCarAvail(null) ;
//				}

				Plan plan = person.createPlan(true);
				plan.setSelected(true) ;

				idx = idxFromKey.get("grid_id:i4") ;
				Id gridId = new IdImpl( parts[idx] ) ;
				Facility facility = (Facility) gridCells.getLocation( gridId ) ;
				plan.createAct(ACT_HOME, facility) ;

				plan.createLeg(Mode.car);
				plan.createAct(ACT_WORK, (Facility) null ) ;

				plan.createLeg(Mode.car) ;
				plan.createAct(ACT_HOME, facility ) ;

				population.addPerson(person);
			}

			line = reader.readLine(); // next line

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// first implement singly constrained gravity model:
		// (matsim person algorithm??)

		// mir ist nicht klar, warum es diese Konstruktion tut:
		// liegt daran, dass population.getPersons eine normale Map zurŸckgibt. 
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan() ;

			Map<Id, ? extends Location> locations = gridCells.getLocations() ;

//			Collections.

//			locations.

		}

		PopulationWriter popWriter = new PopulationWriter(population,"/home/nagel/tmp/pop.xml","v4",1) ;
		popWriter.write();

		System.out.println("### DONE ###") ;

	}
}
