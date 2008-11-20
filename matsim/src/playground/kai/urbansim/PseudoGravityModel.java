/**
 * 
 */
package playground.kai.urbansim;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.facilities.Facilities;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.Location;

/**
 * @author nagel
 *
 */
public class PseudoGravityModel {
	private static final Logger log = Logger.getLogger(PseudoGravityModel.class);

	private Population population ;
	private Facilities facilities ;
	private Map<Id,Id> locationFromJob ;
	
	private Coord minCoord = new CoordImpl( Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY ) ;
	private Coord maxCoord = new CoordImpl( Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY ) ;

	static final int ll = 33 ;
	static final int llll = ll*ll ;
	
	private int binFromXY( double xx, double yy ) {
		int xInt = (int) (0.99*(xx-minCoord.getX())/(maxCoord.getX()-minCoord.getX())*ll) ;  // TODO: "0.99" is hack to avoid problems if xx==xx_max
		int yInt = (int) (0.99*(yy-minCoord.getY())/(maxCoord.getY()-minCoord.getY())*ll) ;
		int bin = xInt*ll + yInt ;
		return bin ;
	}
	
	private void printArray ( double[][] array ) {
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

	void checkMax( Coord coord ) {
		double xx = coord.getX();
		if ( xx < minCoord.getX() ) {
			minCoord.setX(xx) ;
		} else if ( xx > maxCoord.getX() ) {
			maxCoord.setX(xx) ;
		}
		double yy = coord.getY() ;
		if ( yy < minCoord.getY() ) {
			minCoord.setY(yy) ;
		} else if ( yy > maxCoord.getY() ) {
			maxCoord.setY(yy) ;
		}
	}
	
	// TODO: there should actually a ctor that constructs this w/o coords (i.e. computes them internally)
	public PseudoGravityModel ( Population population, Facilities facilities, Map<Id,Id> locationFromJob ) {
		this.population = population ;
		this.facilities = facilities ;
		this.locationFromJob = locationFromJob ;
	}
	
	public void run() {
		// NOTE: For the time being, I need this model only for U_CELL, not for U_PARCEL.  So it does not need to be general.
		
		// compute the extent of the coordinates for persons:
		for ( Person person : population ) {
			Plan plan = person.getSelectedPlan() ;
			Act act = plan.getFirstActivity();
			Coord homeCoord = act.getCoord() ;
			checkMax( homeCoord ) ;
		}
		// same for jobs except it's a bit more indirect:
		for ( Iterator<Id> it = locationFromJob.values().iterator(); it.hasNext(); ) {
			LocationId gridId = (LocationId) it.next();
			Coord jobCoord = facilities.getLocation( gridId ).getCenter() ;
			checkMax(jobCoord) ;
		}
			
		
		// marginals
		List<PseudoCell> pseudoCells = new ArrayList<PseudoCell>(llll) ;
		for ( int ii=0 ; ii<llll ; ii++ ) {
			PseudoCell pc = new PseudoCell() ;
			pseudoCells.add(pc) ;
		}

		// for every job, add it to the pseudoCell
		for ( Iterator<Id> it = locationFromJob.values().iterator(); it.hasNext(); ) {
			LocationId parcelId = (LocationId) it.next();
			Location parcel = facilities.getLocation( parcelId ) ;
			Coord cc = parcel.getCenter() ;
			int bin = binFromXY( cc.getX() , cc.getY() ) ;
			PseudoCell pc = pseudoCells.get(bin) ;
			pc.addJob( cc ) ;
		}
		
		// for every worker, add it to the pseudoCell
		for ( Person pp : population.getPersons().values() ) {
			if ( pp.getEmployed().equals("yes") ) {
				Coord cc = pp.getSelectedPlan().getFirstActivity().getCoord(); // awkward
				int bin = binFromXY( cc.getX(), cc.getY() ) ;
				PseudoCell pc = pseudoCells.get(bin) ;
//				System.out.println ( "adding a worker" ) ;
				pc.addWorker( cc ) ;
			}
			
			
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
			Utils.completePlanToHwh(plan, workCoord) ;

		}
	}

}
