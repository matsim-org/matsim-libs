/**
 *
 */
package playground.kai.urbansim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.world.Location;

import playground.kai.urbansim.ids.LocationId;

/**
 * This is meant to generate hwh trips if only home locations and work locations are given, but not their relation (2nd step of
 * four step process, also called destination choice or trip distribution).  It should work, but the only time I tried to use it
 * was with the urbansim eugene data set, and I am reasonably sure that there were coordinate system issues (the coordinate system
 * of the network had nothing to do with the coordinate system of the urbansim gridcells ... then it can't possibly work).
 *
 * So it is (ab)using the "Deprecated" marker for "not fully developed".
 *
 * The approach here is completely independent from any spatial system given in the original data set: It only uses point coordinates
 * for home locations and work locations.   These are aggregated into a grid that is hardcoded below as 33x33 (this could be made
 * configurable without problems, but runtimes go up quite a lot if this is made much bigger).
 *
 * There is also, as of now, absolutely no distance-dependent impedance, so jobs are selected randomly in the area.  This is clearly
 * not very realistic, but in order to get small-scale scenarios off the ground, it should actually be enough.  It is also in principle
 * easy to fix, although it would mean that the coordinate system that is used conveys some meaning of "distance" or "trave time",
 * which is not necessary with the current approach.
 *
 * @author nagel
 *
 */
@Deprecated
public class PseudoGravityModel {
	private static final Logger log = Logger.getLogger(PseudoGravityModel.class);

	private Population population ;
	private ActivityFacilitiesImpl facilities ;
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
	public PseudoGravityModel ( Population population, ActivityFacilitiesImpl facilities, Map<Id,Id> locationFromJob ) {
		this.population = population ;
		this.facilities = facilities ;
		this.locationFromJob = locationFromJob ;
	}

	public void run() {
		// NOTE: For the time being, I need this model only for U_CELL, not for U_PARCEL.  So it does not need to be general.

		// compute the extent of the coordinates for persons:
		for ( Person person : population.getPersons().values() ) {
			Plan plan = person.getSelectedPlan() ;
			ActivityImpl act = ((PlanImpl) plan).getFirstActivity();
			Coord homeCoord = act.getCoord() ;
			checkMax( homeCoord ) ;
		}
		// same for jobs except it's a bit more indirect:
		for ( Iterator<Id> it = locationFromJob.values().iterator(); it.hasNext(); ) {
			LocationId gridId = (LocationId) it.next();
			Coord jobCoord = facilities.getFacilities().get( gridId ).getCoord() ;
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
			Location parcel = facilities.getFacilities().get( parcelId ) ;
			Coord cc = parcel.getCoord() ;
			int bin = binFromXY( cc.getX() , cc.getY() ) ;
			PseudoCell pc = pseudoCells.get(bin) ;
			pc.addJob( cc ) ;
		}

		// for every worker, add it to the pseudoCell
		for ( Person pp : population.getPersons().values() ) {
			if ( ((PersonImpl) pp).getEmployed().equals("yes") ) {
				Coord cc = ((PlanImpl) pp.getSelectedPlan()).getFirstActivity().getCoord(); // awkward
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
			Coord homeCoord = ((PlanImpl) pp.getSelectedPlan()).getFirstActivity().getCoord(); // awkward

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
			Utils.completePlanToHwh((PlanImpl) plan, workCoord) ;

		}
	}

}
