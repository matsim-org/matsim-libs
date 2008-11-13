package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facilities;
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
import org.matsim.utils.io.IOUtils;
import org.matsim.world.Location;

import playground.kai.IdBuilder;

public class Test {
	private static final Logger log = Logger.getLogger(Test.class);

	public static final String ACT_HOME = "home" ;
	public static final String ACT_WORK = "work" ;
	
	static Map<Id,Id> buildingFromHousehold = new HashMap<Id,Id>() ;
//	static Map<Id,Id> parcelFromJob = new HashMap<Id,Id>() ;
	static Map<Id,Id> parcelFromBuilding = new HashMap<Id,Id>() ;
	static Map<Id,Id> buildingFromJob = new HashMap<Id,Id>() ;
	static Facilities parcels = new Facilities("urbansim parcels", Facilities.FACILITIES_NO_STREAMING) ;

	
	public static double xx_min = Double.POSITIVE_INFINITY ;
	public static double xx_max = Double.NEGATIVE_INFINITY ;
	public static double yy_min = Double.POSITIVE_INFINITY ;
	public static double yy_max = Double.NEGATIVE_INFINITY ;
	
	static void checkMax( double xx, double yy ) {
		if ( xx > xx_max ) { xx_max = xx ; }
		if ( xx < xx_min ) { xx_min = xx ; }
		if ( yy > yy_max ) { yy_max = yy ; }
		if ( yy < yy_min ) { yy_min = yy ; }
	}
	static void checkMax( String xxStr, String yyStr ) {
		double xx = Double.parseDouble(xxStr) ;
		double yy = Double.parseDouble(yyStr) ;
		checkMax( xx, yy ) ;
	}
	
	static final int ll = 33 ;
	static final int llll = ll*ll ;
	
	static int binFromXY( double xx, double yy ) {
		int xInt = (int) ((xx-xx_min)/(xx_max-xx_min)*ll) ;
		int yInt = (int) ((yy-yy_min)/(yy_max-yy_min)*ll) ;
		return xInt*ll + yInt ;
	}
	
	static void printArray ( double[][] array ) {
		log.info( "============================================ ") ;
		log.info( "note: following may only be part of gravity matrix" ) ;
		for ( int ii=0 ; ii<Math.min(20,llll) ; ii++ ) {
			for ( int jj=0 ; jj<Math.min(20,llll) ; jj++ ) {
				System.out.print( " " + array[ii][jj] ) ;
			}
			System.out.println();
		}
		log.info( "============================================ ") ;
	}

	static Map<String,Integer> createIdxFromKey( String line ) {
		String[] keys = line.split("[ \t\n]+") ;

		Map<String,Integer> idxFromKey = new HashMap<String, Integer>() ;
		for ( int ii=0 ; ii<keys.length ; ii++ ) {
			idxFromKey.put(keys[ii], ii ) ;
		}
		return idxFromKey ;
	}

	public static void readNetwork( NetworkLayer network ) {
		network.setCapacityPeriod(3600.) ;
		network.setEffectiveLaneWidth(3.75) ;
//		network.setEffectiveCellSize(7.5) ;
		
		// read emme3 network
		try {
			BufferedReader reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/net1.out" ) ;

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
//						checkMax( xxStr, yyStr ) ;
					} else {
						Node fromNode = network.getNode(parts[1]) ;
						Node   toNode = network.getNode(parts[2]);
						double length = 1600 * Double.parseDouble( parts[3] ) ; // probably miles
//						String type = parts[5] ;

						double permlanes = Double.parseDouble( parts[6] ) ;
						if ( permlanes <= 0 ) { permlanes = 0.5 ; }

						double capacity = permlanes * Double.parseDouble( parts[8] ) ;
						if ( capacity <= 500 ) { capacity = 500. ; }
						
						double freespeed = Double.parseDouble( parts[9] ) ; // mph
						if ( freespeed < 10. ) { freespeed = 10. ; }
						freespeed *= 1600./3600. ;

						Id id = new IdImpl( linkCnt ) ; linkCnt++ ;

						network.createLink(id, fromNode, toNode, length, freespeed, capacity, permlanes );
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
	}

	public static void readParcels ( Facilities parcels ) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/parcels-cleaned.tab" ) ;
			System.err.println("My example of parcels.tab contains ^M characters, which screws up the java readline().  I removed them manually ...") ;
			System.out.println(" cat filename | tr -d 'backslash r' > newfilename " ) ;

			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			String line = reader.readLine() ;
			while ( line != null ) {

				String[] parts = line.split("[\t]+");

				int idx = idxFromKey.get("parcel_id:i4") ;
				String idAsString = parts[idx] ;

				idx = idxFromKey.get("x_coord_sp:f4") ;
				String xxAsString = parts[idx] ;

				idx = idxFromKey.get("y_coord_sp:f4") ;
				String yyAsString = parts[idx];

				Facility facility = parcels.createFacility(idAsString, xxAsString, yyAsString) ;
				facility.setDesc("parcel") ;
				
				checkMax( xxAsString, yyAsString ) ;

				line = reader.readLine() ;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readKV( Map<Id,Id> yFromX, String key, IdBuilder keyIdBuilder, String value, IdBuilder valueIdBuilder, String filename ) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename ) ;

			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			String line = reader.readLine() ;
			while ( line != null ) {
				String[] parts = line.split("[\t\n]+");

				int idx = idxFromKey.get(value) ;
//				Id valueId = new IdImpl( parts[idx] ) ;
				Id valueId = valueIdBuilder.createId( parts[idx] ) ;

				idx = idxFromKey.get(key) ;
//				Id keyId = new IdImpl( parts[idx] ) ;
				Id keyId = keyIdBuilder.createId( parts[idx] ) ;

				yFromX.put(keyId, valueId) ;

				line = reader.readLine() ;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void readParcelsFromJobs ( Map<Id,Id> parcelFromJob ) {
		ParcelIdBuilder parcelIdBuilder = new ParcelIdBuilder() ;
		JobIdBuilder jobIdBuilder = new JobIdBuilder() ;
		readKV( parcelFromJob, "job_id:i4", jobIdBuilder, "parcel_id:i4", parcelIdBuilder, "/home/nagel/tmp/tab/jobs.tab" ) ;
	}
	
	public static void readBuildingsFromJobs( Map<Id,Id> buildingFromJob ) {
		BldIdBuilder bldIdBuilder = new BldIdBuilder() ;
		JobIdBuilder jobIdBuilder = new JobIdBuilder() ;
		readKV( buildingFromJob, "job_id:i4", jobIdBuilder, "building_id:i4", bldIdBuilder, "/home/nagel/tmp/tab/jobs.tab" ) ;
	}

	public static void readBuildingsFromHouseholds ( Map<Id,Id> buildingFromHH ) {
		BldIdBuilder bldIdBuilder = new BldIdBuilder() ;
		HHIdBuilder  hhIdBuilder  = new HHIdBuilder() ;
		readKV( buildingFromHH, "household_id:i4", hhIdBuilder, "building_id:i4", bldIdBuilder, "/home/nagel/tmp/tab/households.tab" ) ;
	}

	public static void readParcelsFromBuildings ( Map<Id,Id> parcelFromBuilding ) {
		IdBuilder parcelIdBuilder = new ParcelIdBuilder() ;
		BldIdBuilder bldIdBuilder = new BldIdBuilder() ;
		readKV( parcelFromBuilding, "building_id:i4", bldIdBuilder, "parcel_id:i4", parcelIdBuilder, "/home/nagel/tmp/tab/buildings.tab" ) ;
	}
	
	static void readPersons ( Population population, double fraction ) {

		try {
			BufferedReader reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/persons.tab");

			String header = reader.readLine();
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			String line = reader.readLine();

			while (line != null) {
				String[] parts = line.split("[\t\n]+");

				if ( Math.random() < fraction ) {

					int idx = idxFromKey.get("person_id:i4") ;
					Id personId = new IdImpl( parts[idx] ) ;

					Person person = new PersonImpl( personId ) ;

					idx = idxFromKey.get("age:i4") ;
					person.setAge( Integer.parseInt( parts[idx] ) ) ;

//					idx = idxFromKey.get("employment_status:i4") ;
//					if ( parts[idx].equals("0") ) {
//						person.setEmployed("no") ;
//					} else {
//						person.setEmployed("yes") ;
//					}

					idx = idxFromKey.get("household_id:i4") ;
					HHIdImpl hhId = new HHIdImpl( parts[idx] ) ; 
					BldIdImpl buildingId = (BldIdImpl) buildingFromHousehold.get( hhId ) ;
					assert( buildingId != null ) ;
					ParcelIdImpl homeParcelId = (ParcelIdImpl) parcelFromBuilding.get( buildingId ) ;
					if ( homeParcelId==null ) {
						System.err.println( " personId: " + personId.toString() + " hhId: " + hhId.toString() + " buildingId: " + buildingId.toString() ) ;
						continue ;
					}
					assert( homeParcelId != null ) ;
					Location homeLocation = parcels.getLocation( homeParcelId ) ;
					assert( homeLocation != null ) ;
					Coord homeCoord = homeLocation.getCenter() ;

					// add person only after it's clear that he/she has a home location:
					population.addPerson(person) ;

					Plan plan = person.createPlan(true);
					plan.setSelected(true) ;
					plan.createAct(ACT_HOME, homeCoord ) ;
					
					idx = idxFromKey.get("job_id:i4") ;
					if ( parts[idx].equals("-1") ) {
						person.setEmployed("no") ;
					} else {
						person.setEmployed("yes") ;
						JobIdImpl jobId = new JobIdImpl( parts[idx] ) ;
						buildingId = (BldIdImpl) buildingFromJob.get( jobId ) ;
						if ( buildingId == null ) {
							System.err.println ( " person_id: " + personId.toString() + " job_id: " + jobId.toString() ) ;
							continue ;
						}
						assert( buildingId != null ) ;
						ParcelIdImpl jobParcelId = (ParcelIdImpl) parcelFromBuilding.get( buildingId ) ;
						assert( jobParcelId != null ) ;
						Location jobLocation = parcels.getLocation( jobParcelId ) ;
						assert( jobLocation != null ) ;
						Coord workCoord = jobLocation.getCenter() ;

						Act act = plan.getFirstActivity();
						act.setEndTime( 7.*3600. ) ;
						
						plan.createLeg(Mode.car);
						act = plan.createAct(ACT_WORK, workCoord ) ;
						act.setDuration( 8.*3600. ) ;

						plan.createLeg(Mode.car) ;
						plan.createAct(ACT_HOME, homeCoord ) ;				
					}

				}

				line = reader.readLine(); // next line
			}


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void runPseudoGravityModel( Population population ) {
//		// marginals
//		List<PseudoCell> pseudoCells = new ArrayList<PseudoCell>(llll) ;
//		for ( int ii=0 ; ii<llll ; ii++ ) {
//			PseudoCell pc = new PseudoCell() ;
//			pseudoCells.add(pc) ;
//		}
//
//		// for every job, add it to the pseudoCell
//		for ( Iterator<Id> it = parcelFromJob.values().iterator(); it.hasNext(); ) {
//			ParcelIdImpl parcelId = (ParcelIdImpl) it.next();
//			Location parcel = parcels.getLocation( parcelId ) ;
//			Coord cc = parcel.getCenter() ;
//			int bin = binFromXY( cc.getX() , cc.getY() ) ;
//			PseudoCell pc = pseudoCells.get(bin) ;
//			pc.addJob( cc ) ;
//		}
//		
//		// for every worker, add it to the pseudoCell
//		System.err.println( "the following adds ALL people to the job market, even those who are not employed" ) ;
//		for ( Person pp : population.getPersons().values() ) {
////			if ( !pp.getEmployed().equals("0") ) { // FIXME: not robust
//				Coord cc = pp.getSelectedPlan().getFirstActivity().getCoord(); // awkward
//				int bin = binFromXY( cc.getX(), cc.getY() ) ;
//				PseudoCell pc = pseudoCells.get(bin) ;
////				System.out.println ( "adding a worker" ) ;
//				pc.addWorker( cc ) ;
////			}
//		}
//		
//		final double EPS=0.01 ;
//		
//		double[][] zone2zone = new double[llll][llll] ;
//		for ( int ii=0 ; ii<llll ; ii++ ) {
//			for ( int jj=0 ; jj<llll ; jj++ ) {
//				double tmp = 1. ; // FIXME: initialize w/ impedance function!
//				tmp *= pseudoCells.get(ii).getNWorkers() ;
//				tmp *= pseudoCells.get(jj).getNJobs() ;
//				if ( tmp < 1 ) { tmp = EPS ; } // FIXME: "epsilon" initialization; maybe too large?? maybe use impedance fct?
//				zone2zone[ii][jj] = tmp ;
//			}
//		}
//		
//		printArray( zone2zone ) ;
//
//		log.info( "Running IPF" ) ;
//		for ( int kk=0 ; kk<100 ; kk++ ) {
//			System.out.print(".") ;
//			// IPF: fix each row (ii)
//			for ( int ii=0 ; ii<llll ; ii++ ) {
//				// sum up:
//				double sum = 0. ;
//				for ( int jj=0 ; jj<llll ; jj++ ) {
//					sum += zone2zone[ii][jj] ;
//				}
//				if ( sum==0 ) { sum = EPS ; }
//				// re-scale:
//				double factor = pseudoCells.get(ii).getNWorkers() / sum ;
////				System.out.println ( " nWorkers: " + pseudoCells.get(ii).getNWorkers() + " sum: " + sum + " factor: " + factor ) ;
//				for ( int jj=0 ; jj<llll ; jj++ ) {
//					zone2zone[ii][jj] *= factor ;
//				}
//			}
//
//			// IPF: fix each column (jj)
//			for ( int jj=0 ; jj<llll ; jj++ ) {
//				// sum up:
//				double sum = 0. ;
//				for ( int ii=0 ; ii<llll ; ii++ ) {
//					sum += zone2zone[ii][jj] ;
//				}
//				if ( sum == 0 ) { sum = EPS ; }
//				// re-scale:
//				double factor = pseudoCells.get(jj).getNJobs() / sum ;
////				System.out.println ( " nJobs: " + pseudoCells.get(jj).getNJobs() + " sum: " + sum + " factor: " + factor ) ;
//				for ( int ii=0 ; ii<llll ; ii++ ) {
//					zone2zone[ii][jj] *= factor ;
//				}
//			}
//
//		}
//		System.out.println();
//		log.info("done with IPF") ;
//		printArray( zone2zone ) ;
//		
//		for ( Person pp : population.getPersons().values() ) {
//			// pull plan:
//			Plan plan = pp.getSelectedPlan();
//
//			// get home coordinates:
//			Coord homeCoord = pp.getSelectedPlan().getFirstActivity().getCoord(); // awkward
//			
//			// get relevant bin:
//			int homeBin = binFromXY( homeCoord.getX(), homeCoord.getY() ) ;
////			PseudoCell HomePc = pseudoCells.get(homeBin) ;
//			
//			// compute row sum:
//			double sum = 0. ;
//			for ( int jj=0 ; jj<llll; jj++ ) {
//				sum += zone2zone[homeBin][jj] ;
//			}
//			
//			// draw destination bin:
//			double rnd = Math.random();
//			double ssum = 0. ;
//			int workBin = 0 ;
//			for ( int jj=0 ; jj<llll ; jj++ ) {
//				ssum += zone2zone[homeBin][jj]/sum ;
//				if ( rnd < ssum ) {
//					workBin = jj ;
//					break ;
//				}
//			}
//			Coord workCoord = pseudoCells.get(workBin).getCoords() ;
//			// (note that this does NOT enforce constraints once more here)
//			// (it also does not really allocate the workplaces)
//			
//			// create work plan in destination pseudoCell:
//			Act act = plan.getFirstActivity();
//			act.setEndTime( 7.*3600. ) ;
//			
//			plan.createLeg(Mode.car);
//			act = plan.createAct(ACT_WORK, workCoord ) ;
//			act.setDur( 8.*3600. ) ;
//
//			plan.createLeg(Mode.car) ;
//			plan.createAct(ACT_HOME, homeCoord ) ;
//
//		}
	}

	public static void main ( String[] args ) {
		
		// read emme3 network:
		NetworkLayer network = new NetworkLayer() ;
		readNetwork(network) ;
		
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		
//		NetworkWriter nwWriter = new NetworkWriter(network,"/home/nagel/tmp/net.xml.gz") ;
//		nwWriter.write() ;

		// read urbansim parcels:
		readParcels(parcels) ;

//		FacilitiesWriter facWriter = new FacilitiesWriter(parcels,"/home/nagel/tmp/parcels.xml.gz") ;
//		facWriter.write();

		// read urbansim jobs:
//		readParcelsFromJobs( parcelFromJob ) ;
		readBuildingsFromJobs( buildingFromJob ) ;

		// read urbansim buildings
		readParcelsFromBuildings( parcelFromBuilding ) ;

		// read urbansim households:
		readBuildingsFromHouseholds( buildingFromHousehold ) ;

		// read urbansim persons
		Population population = new Population(Population.NO_STREAMING);
		readPersons( population, 0.01 ) ;
		
//		// run pseudoGravity model:
//		runPseudoGravityModel( population ) ;
		
		
		PopulationWriter popWriter = new PopulationWriter(population,"/home/nagel/tmp/pop.xml.gz","v4",1) ;
		popWriter.write();

		System.out.println("### DONE with demand generation from urbansim ###") ;
		
		buildingFromHousehold.clear();
		parcelFromBuilding.clear() ;
		buildingFromJob.clear();
		System.gc();
		
		Config config = Gbl.createConfig(new String[] {"/home/nagel/tmp/myconfig.xml"});
		
		Controler controler = new Controler(config,network,population) ;
		controler.setOverwriteFiles(true) ;
		controler.run() ;

	}
}
