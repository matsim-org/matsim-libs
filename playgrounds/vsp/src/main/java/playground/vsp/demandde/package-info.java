package playground.vsp.demandde;

/**
 * This package is trying to (somewhat) pull together the pieces that led to the Munich demand.  As there are:<ul>
 * 
 * <li> Munich MiD travel diaries for approx. 1% of the population living within the city limits.  That code is ... somewhere. 
 * Yet since generating MATSim plans from travel diaries is fairly straightforward, and 90% of the work is adapting to the
 * local survey coding conventions, this is omitted here for the time being.
 * 
 * <li> Input from the commuter statistics (Pendlermatrix) of the German employment office (Agentur fuer Arbeit).  This is the 
 * package "pendlermatrix"; documentation should be there but probably is not yet there.
 * 
 * <li> Input from the so-called analysis case of the so-called "prognosis 2025" study.  Contains origin-destination matrices
 * for all traffic Germany, on a relatively coarse scale.  In principle also contains person trips, but for person-trips the 
 * "Pendlermatrix" was used.  However, the freight matrices from "prognosis 2025" were used.  They were generated using some
 * ruby scripts which are in this same playground but outside the java part of it.
 * 
 * <li> The "munich" package pulls those three inputs together into one demand.
 * 
 * </ul>
 * 
 * @author zilske, grether, kickhoefer, nagel
 */

class DemanddePackageInfo{
	
}