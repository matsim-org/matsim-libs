package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.matsim.basic.v01.IdImpl;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.io.IOUtils;

public class Test {

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
		
		

		try {
			reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/gridcells.tab" ) ;

			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;
			
			String line = reader.readLine() ;
			while ( line != null ) {
				String[] parts = line.split("[ \t\n]+");

				
				
				line = reader.readLine() ;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		Population population = new Population(Population.NO_STREAMING);

		try {
			reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/households.tab");

			String header = reader.readLine();
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			long counter=0 ;

			String line = reader.readLine(); {}

			while (line != null) {
				String[] parts = line.split("[ \t\n]+");

				int idx = idxFromKey.get("persons:i4") ;
				int nPersons = Integer.parseInt( parts[idx] ) ;

				idx = idxFromKey.get("workers:i4") ;
				int nWorkers = Integer.parseInt( parts[idx] ) ;

				idx = idxFromKey.get("cars:i4") ;
				int nCars = Integer.parseInt( parts[idx] ) ;

				for ( int ii=0 ; ii<nPersons ; ii++ ) {

					Person person = new Person(new IdImpl(counter++));

					idx = idxFromKey.get("age_of_head:i4") ;
					person.setAge( Integer.parseInt( parts[idx] ) ) ;

					if ( ii<nWorkers ) {
						person.setEmployed("worker") ;
					} else {
						person.setEmployed(null) ;
					}

					if ( ii<nCars ) {
						person.setCarAvail("car") ;
					} else {
						person.setCarAvail(null) ;
					}

					Plan plan = person.createPlan(true);
//					plan.createAct("home", fac); // use data from parts[x] here


					//					plan.createLeg(Mode.car);
//					plan.createAct(...);

					population.addPerson(person);
				}

				line = reader.readLine(); // next line
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
