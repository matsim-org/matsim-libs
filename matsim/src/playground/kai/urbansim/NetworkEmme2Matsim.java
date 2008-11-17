/**
 * 
 */
package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.utils.io.IOUtils;

/**
 * Translates emme networks into matsim networks.
 * 
 * Uses the "emme network export" result as input, NOT the GIS export.
 * 
 * A serious problem is that there is not enough info in the headers of the emme files:
 * (column counting starts at 0!!!)
 * - link "length" (column # 3) is in arbitrary units, connected to rest of system only through freespeed value
 * - Column # 8-10 are user-defined BUT contain freespeed, capacity, ... in arbitrary order and units
 *   (connected to rest of system through user-defined volume-delay functions) 
 * 
 * @author nagel
 *
 */
public class NetworkEmme2Matsim {
	private static final Logger log = Logger.getLogger(NetworkEmme2Matsim.class);

	private static final int PSRC = 0 ;
	private static final int EUGENE = 1 ;

	private static final int NW_NAME = EUGENE ;

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
						if ( NW_NAME==PSRC ) {
							node.setOrigId(parts[7]);		
						}
//						checkMax( xxStr, yyStr ) ;
					} else {
						Node fromNode = network.getNode(parts[1]) ;
						Node   toNode = network.getNode(parts[2]);
						double length = 1600 * Double.parseDouble( parts[3] ) ; // probably miles
//						String type = parts[5] ;

						double permlanes = Double.parseDouble( parts[6] ) ;
						if ( permlanes <= 0 ) { permlanes = 0.5 ; }

						double capacity, freespeed ;
						if ( NW_NAME==PSRC ) {
							capacity = permlanes * Double.parseDouble( parts[8] ) ;
							if ( capacity <= 500 ) { capacity = 500. ; }

							freespeed = Double.parseDouble( parts[9] ) ; // mph
							if ( freespeed < 10. ) { freespeed = 10. ; }
							freespeed *= 1600./3600. ;
						} else if ( NW_NAME==EUGENE ) {
							log.warn("For EUGENE, I have not clarified if capacity really needs to be multiplied by number of lanes.");
							capacity = permlanes * Double.parseDouble( parts[10] ) ;
							if ( capacity <= 500 ) { capacity = 500. ; }

							freespeed = Double.parseDouble( parts[9] ) ; // mph
							if ( freespeed < 10. ) { freespeed = 10. ; }
							freespeed *= 1600./3600. ;
						} else {
							log.error( "NW_NAME not known; aborting" ) ;
							System.exit(-1);
						}

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



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NetworkLayer network = new NetworkLayer() ;

		log.info("reading network ...");
		readNetwork(network) ;
		log.info("... finished reading network.\n");

		log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		log.info("... finished cleaning network.\n") ;

		log.info("writing network ...") ;
		NetworkWriter nwWriter = new NetworkWriter(network,"/home/nagel/tmp/net.xml.gz") ;
		nwWriter.write() ;
		log.info("... finished writing network.\n") ;
	}

}
