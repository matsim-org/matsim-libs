package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.ScenarioData;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.Location;


public class Test {
	private static final Logger log = Logger.getLogger(Test.class);
	
	private static final int U_CELL = 0 ;
	private static final int U_PARCEL = 1 ;
	
	private static final int U_MODEL_TYPE = U_CELL ; // configure!!

	public static final String ACT_HOME = "home" ;
	public static final String ACT_WORK = "work" ;
	
	static Map<Id,Id> buildingFromHousehold = new HashMap<Id,Id>() ;
	static Map<Id,Id> buildingFromJob = new HashMap<Id,Id>() ;
	static Map<Id,Id> locationFromJob = new HashMap<Id,Id>() ;

	static Map<Id,Id> locationFromBuilding = new HashMap<Id,Id>() ;
	static Map<Id,Id> locationFromHousehold = new HashMap<Id,Id>() ;

	static Facilities locations = new Facilities("urbansim locations (gridcells _or_ parcels _or_ ...)", Facilities.FACILITIES_NO_STREAMING) ;
	// these are simply the entities that have coordinates!

	
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
	static void checkMax( Coord coord ) {
		double xx = coord.getX();
		double yy = coord.getY();
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

	public static void readLocations ( Facilities locations ) {
		try {
			BufferedReader reader ;
			if ( U_MODEL_TYPE==U_PARCEL ) {
				System.err.println("My example of parcels.tab contains ^M characters, which screws up the java readline().  I removed them manually ...") ;
				System.out.println(" cat filename | tr -d 'backslash r' > newfilename " ) ;
				reader = IOUtils.getBufferedReader("opus_matsim/tmp/parcels-cleaned.tab" ) ;
			} else if ( U_MODEL_TYPE==U_CELL ) {
				reader = IOUtils.getBufferedReader("opus_matsim/tmp/gridcells.tab" ) ;
			} else {
				log.fatal("not implemented; abort") ;
				System.exit(-1) ;
			}

			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = createIdxFromKey( header ) ;

			String line = reader.readLine() ;
			while ( line != null ) {

				String[] parts = line.split("[\t]+");

				Id id ; 
				Coord coord ;
				if ( U_MODEL_TYPE==U_PARCEL ) {
					int idx_id = idxFromKey.get("parcel_id:i4") ;
					id = new IdImpl( parts[idx_id] ) ;

					int idx_x = idxFromKey.get("x_coord_sp:f4") ;
					int idx_y = idxFromKey.get("y_coord_sp:f4") ;
					coord = new CoordImpl( parts[idx_x], parts[idx_y] ) ;
				} else if ( U_MODEL_TYPE==U_CELL ) {
					int idx_id = idxFromKey.get("grid_id:i4") ;
					id = new IdImpl( parts[idx_id] ) ;

					int idx_x = idxFromKey.get("relative_x:i4") ;
					int idx_y = idxFromKey.get("relative_y:i4") ;
					coord = new CoordImpl( parts[idx_x], parts[idx_y] ) ;
				}

				Facility facility = locations.createFacility(id,coord) ;
				facility.setDesc("urbansim location") ;
				
				checkMax( coord ) ;

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
				Id valueId = valueIdBuilder.createId( parts[idx] ) ;

				idx = idxFromKey.get(key) ;
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
	
	public static void constructAFromC( Map<Id,Id> aFromC, Map<Id,Id> aFromB, Map<Id,Id> bFromC ) {
		for ( Iterator<Entry<Id,Id>> it = bFromC.entrySet().iterator() ; it.hasNext(); ) {
			Entry<Id,Id> entry = it.next();
			Id cc = entry.getKey() ;
			Id bb = entry.getValue();
			
			Id aa = aFromB.get(bb) ;
			
			aFromC.put(cc,aa) ;
		}
	}


	public static void readBuildingsFromJobs( Map<Id,Id> buildingFromJob ) {
		BldIdBuilder bldIdBuilder = new BldIdBuilder() ;
		JobIdBuilder jobIdBuilder = new JobIdBuilder() ;
		readKV( buildingFromJob, "job_id:i4", jobIdBuilder, "building_id:i4", bldIdBuilder, "opus_matsim/tmp/jobs.tab" ) ;
	}

	public static void readBuildingsFromHouseholds ( Map<Id,Id> buildingFromHH ) {
		BldIdBuilder bldIdBuilder = new BldIdBuilder() ;
		HHIdBuilder  hhIdBuilder  = new HHIdBuilder() ;
		readKV( buildingFromHH, "household_id:i4", hhIdBuilder, "building_id:i4", bldIdBuilder, "opus_matsim/tmp/households.tab" ) ;
	}

	public static void readParcelsFromBuildings ( Map<Id,Id> parcelFromBuilding ) {
		IdBuilder parcelIdBuilder = new LocationIdBuilder() ;
		BldIdBuilder bldIdBuilder = new BldIdBuilder() ;
		readKV( parcelFromBuilding, "building_id:i4", bldIdBuilder, "parcel_id:i4", parcelIdBuilder, "opus_matsim/tmp/buildings.tab" ) ;
	}

	public static void readGridcellsFromJobs ( Map<Id,Id> gridcellFromJob ) {
		LocationIdBuilder gridcellIdBuilder = new LocationIdBuilder() ;
		readKV( gridcellFromJob, "job_id:i4", new JobIdBuilder(), "grid_id:i4", gridcellIdBuilder, "opus_matsim/tmp/jobs.tab" ) ;
	}

	public static void readGridcellsFromHouseholds ( Map<Id,Id> gridcellFromJob ) {
		LocationIdBuilder gridcellIdBuilder = new LocationIdBuilder() ;
		HHIdBuilder hhIdBuilder = new HHIdBuilder() ;
		readKV( gridcellFromJob, "household_id:i4", hhIdBuilder, "grid_id:i4", gridcellIdBuilder, "opus_matsim/tmp/households.tab" ) ;
	}
	
	static void readPersons ( Population population, double fraction ) {

		try {
			BufferedReader reader = IOUtils.getBufferedReader("opus_matsim/tmp/tab/persons.tab");

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

					idx = idxFromKey.get("household_id:i4") ;
					HHId hhId = new HHId( parts[idx] ) ; 
					BldId buildingId = (BldId) buildingFromHousehold.get( hhId ) ;
					assert( buildingId != null ) ;
					LocationId homeParcelId = (LocationId) locationFromBuilding.get( buildingId ) ;
					if ( homeParcelId==null ) {
						System.err.println( " personId: " + personId.toString() + " hhId: " + hhId.toString() + " buildingId: " + buildingId.toString() ) ;
						continue ;
					}
					assert( homeParcelId != null ) ;
					Location homeLocation = locations.getLocation( homeParcelId ) ;
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
						JobId jobId = new JobId( parts[idx] ) ;
						buildingId = (BldId) buildingFromJob.get( jobId ) ;
						if ( buildingId == null ) {
							System.err.println ( " person_id: " + personId.toString() + " job_id: " + jobId.toString() ) ;
							continue ;
						}
						assert( buildingId != null ) ;
						LocationId jobParcelId = (LocationId) locationFromBuilding.get( buildingId ) ;
						assert( jobParcelId != null ) ;
						Location jobLocation = locations.getLocation( jobParcelId ) ;
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
			System.exit(-1) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void runPseudoGravityModel( Population population ) {
		// NOTE: For the time being, I need this model only for U_CELL, not for U_PARCEL.  So it does not need to be general.
		
		// marginals
		List<PseudoCell> pseudoCells = new ArrayList<PseudoCell>(llll) ;
		for ( int ii=0 ; ii<llll ; ii++ ) {
			PseudoCell pc = new PseudoCell() ;
			pseudoCells.add(pc) ;
		}

		// for every job, add it to the pseudoCell
		for ( Iterator<Id> it = locationFromJob.values().iterator(); it.hasNext(); ) {
			LocationId parcelId = (LocationId) it.next();
			Location parcel = locations.getLocation( parcelId ) ;
			Coord cc = parcel.getCenter() ;
			int bin = binFromXY( cc.getX() , cc.getY() ) ;
			PseudoCell pc = pseudoCells.get(bin) ;
			pc.addJob( cc ) ;
		}
		
		// for every worker, add it to the pseudoCell
		System.err.println( "the following adds ALL people to the job market, even those who are not employed" ) ;
		for ( Person pp : population.getPersons().values() ) {
//			if ( !pp.getEmployed().equals("0") ) { // FIXME: not robust
				Coord cc = pp.getSelectedPlan().getFirstActivity().getCoord(); // awkward
				int bin = binFromXY( cc.getX(), cc.getY() ) ;
				PseudoCell pc = pseudoCells.get(bin) ;
//				System.out.println ( "adding a worker" ) ;
				pc.addWorker( cc ) ;
//			}
		}
		
		final double EPS=0.01 ;
		
		double[][] zone2zone = new double[llll][llll] ;
		for ( int ii=0 ; ii<llll ; ii++ ) {
			for ( int jj=0 ; jj<llll ; jj++ ) {
				double tmp = 1. ; // FIXME: initialize w/ impedance function!
				tmp *= pseudoCells.get(ii).getNWorkers() ;
				tmp *= pseudoCells.get(jj).getNJobs() ;
				if ( tmp < 1 ) { tmp = EPS ; } // FIXME: "epsilon" initialization; maybe too large?? maybe use impedance fct?
				zone2zone[ii][jj] = tmp ;
			}
		}
		
		printArray( zone2zone ) ;

		log.info( "Running IPF" ) ;
		for ( int kk=0 ; kk<100 ; kk++ ) {
			System.out.print(".") ;
			// IPF: fix each row (ii)
			for ( int ii=0 ; ii<llll ; ii++ ) {
				// sum up:
				double sum = 0. ;
				for ( int jj=0 ; jj<llll ; jj++ ) {
					sum += zone2zone[ii][jj] ;
				}
				if ( sum==0 ) { sum = EPS ; }
				// re-scale:
				double factor = pseudoCells.get(ii).getNWorkers() / sum ;
//				System.out.println ( " nWorkers: " + pseudoCells.get(ii).getNWorkers() + " sum: " + sum + " factor: " + factor ) ;
				for ( int jj=0 ; jj<llll ; jj++ ) {
					zone2zone[ii][jj] *= factor ;
				}
			}

			// IPF: fix each column (jj)
			for ( int jj=0 ; jj<llll ; jj++ ) {
				// sum up:
				double sum = 0. ;
				for ( int ii=0 ; ii<llll ; ii++ ) {
					sum += zone2zone[ii][jj] ;
				}
				if ( sum == 0 ) { sum = EPS ; }
				// re-scale:
				double factor = pseudoCells.get(jj).getNJobs() / sum ;
//				System.out.println ( " nJobs: " + pseudoCells.get(jj).getNJobs() + " sum: " + sum + " factor: " + factor ) ;
				for ( int ii=0 ; ii<llll ; ii++ ) {
					zone2zone[ii][jj] *= factor ;
				}
			}

		}
		System.out.println();
		log.info("done with IPF") ;
		printArray( zone2zone ) ;
		
		for ( Person pp : population.getPersons().values() ) {
			// pull plan:
			Plan plan = pp.getSelectedPlan();

			// get home coordinates:
			Coord homeCoord = pp.getSelectedPlan().getFirstActivity().getCoord(); // awkward
			
			// get relevant bin:
			int homeBin = binFromXY( homeCoord.getX(), homeCoord.getY() ) ;
//			PseudoCell HomePc = pseudoCells.get(homeBin) ;
			
			// compute row sum:
			double sum = 0. ;
			for ( int jj=0 ; jj<llll; jj++ ) {
				sum += zone2zone[homeBin][jj] ;
			}
			
			// draw destination bin:
			double rnd = Math.random();
			double ssum = 0. ;
			int workBin = 0 ;
			for ( int jj=0 ; jj<llll ; jj++ ) {
				ssum += zone2zone[homeBin][jj]/sum ;
				if ( rnd < ssum ) {
					workBin = jj ;
					break ;
				}
			}
			Coord workCoord = pseudoCells.get(workBin).getCoords() ;
			// (note that this does NOT enforce constraints once more here)
			// (it also does not really allocate the workplaces)
			
			// create work plan in destination pseudoCell:
			Act act = plan.getFirstActivity();
			act.setEndTime( 7.*3600. ) ;
			
			plan.createLeg(Mode.car);
			act = plan.createAct(ACT_WORK, workCoord ) ;
			act.setDuration( 8.*3600. ) ;

			plan.createLeg(Mode.car) ;
			plan.createAct(ACT_HOME, homeCoord ) ;

		}
	}

	public static void main ( String[] args ) {
		
		// read urbansim locations:
		readLocations(locations) ;

//		FacilitiesWriter facWriter = new FacilitiesWriter(locations,"opus_matsim/tmp/locations.xml.gz") ;
//		facWriter.write();
		
		if ( U_MODEL_TYPE==U_PARCEL ) {
			readBuildingsFromHouseholds( buildingFromHousehold ) ;
			readBuildingsFromJobs( buildingFromJob ) ;
			
			readParcelsFromBuildings( locationFromBuilding ) ;
			
			constructAFromC( locationFromHousehold, locationFromBuilding, buildingFromHousehold ) ; // problem: some HHs don't have buildings
			constructAFromC( locationFromJob      , locationFromBuilding, buildingFromJob ) ;       // problem: some Jobs don't have buildings
			
		} else if ( U_MODEL_TYPE==U_CELL ) {
			readGridcellsFromHouseholds( locationFromHousehold ) ;
			readGridcellsFromJobs( locationFromJob ) ;
		}

		// read urbansim households:

		// read urbansim persons
		Population population = new Population(Population.NO_STREAMING);
		readPersons( population, 0.01 ) ;
		
//		// run pseudoGravity model:
//		runPseudoGravityModel( population ) ;
		
		
		PopulationWriter popWriter = new PopulationWriter(population,"opus_matsim/tmp/pop.xml.gz","v4",1) ;
		popWriter.write();

		System.out.println("### DONE with demand generation from urbansim ###") ;
		
		buildingFromHousehold.clear();
		locationFromBuilding.clear() ;
		buildingFromJob.clear();
		System.gc();
		
		Config config = Gbl.createConfig(args);
		ScenarioData scenarioData = new ScenarioData(config) ;
		NetworkLayer network = scenarioData.getNetwork() ;
		
		Controler controler = new Controler(config,network,population) ;
		controler.setOverwriteFiles(true) ;
		controler.run() ;

	}
}
