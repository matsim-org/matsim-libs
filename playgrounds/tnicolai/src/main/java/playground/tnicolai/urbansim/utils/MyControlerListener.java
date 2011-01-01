package playground.tnicolai.urbansim.utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.urbansim.constants.Constants;
import playground.toronto.ttimematrix.SpanningTree;

/**
 *
 * @author nagel
 *
 */
public class MyControlerListener implements ShutdownListener {
	private static final Logger log = Logger.getLogger(MyControlerListener.class);

	private ActivityFacilitiesImpl zones;
	private Map<Id,WorkplaceObject> numberOfWorkplacesPerZone;
	private String travelDataPath;
	private String zonesPath;
	
	private StringBuffer messageBuffer = null;

	/**
	 * constructor
	 * @param zones 
	 */
	public MyControlerListener( final ActivityFacilitiesImpl zones, final Map<Id,WorkplaceObject> numberOfWorkplacesPerZone ) {
		this.zones = zones;
		this.numberOfWorkplacesPerZone = numberOfWorkplacesPerZone;
		this.travelDataPath = Constants.OPUS_HOME + MATSimConfigObject.getTempDirectory() + "travel_data.csv";
		this.zonesPath = Constants.OPUS_HOME + MATSimConfigObject.getTempDirectory() + "zones.csv";
		
		this.messageBuffer = new StringBuffer();
		this.messageBuffer.append("The computed accessibility for the following zones is < 0.\n");
		this.messageBuffer.append("Therefore their value will be set to 1 to avoid negative logsum values in the zone.csv table.\n");
		this.messageBuffer.append("affected zones are:\n");
	}
	
	/**
	 *	
	 */
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." ) ;
		boolean isSmallerOne = false;

		// get the calling controler:
		Controler controler = event.getControler() ;

		TravelTime ttc = controler.getTravelTimeCalculator();
		SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().charyparNagelScoring()));

		NetworkImpl network = controler.getNetwork() ;
		double depatureTime = 8.*3600 ;
		st.setDepartureTime(depatureTime);

		
		try {
			BufferedWriter travelDataWriter = IOUtils.getBufferedWriter( travelDataPath );
			BufferedWriter zonesWriter = IOUtils.getBufferedWriter( zonesPath );

			log.info("Computing and writing travel_data" ) ;
			// log.warn("Can't feed floats to urbansim; am thus feeding ints for the ttime.") ;
			// solved 3dec08 by travis

			// Travel Data Header
			travelDataWriter.write ( "from_zone_id:i4,to_zone_id:i4,single_vehicle_to_work_travel_cost:f4,am_single_vehicle_to_work_travel_time:f4" ) ; 
			travelDataWriter.newLine();
			
			// Zone Header (workplace asseccibility)
			zonesWriter.write( "zone_id:i4,workplace_accessibility:f4") ;
			zonesWriter.newLine();
			
			// Progress bar
			System.out.println("|--------------------------------------------------------------------------------------------------|") ;
			long cnt = 0; 
			long percentDone = 0;
			
			// main for loop, dumping out zone2zone impeadances (travel times) and workplace accsessibility in two seperate files
			for ( ActivityFacility fromZone : zones.getFacilities().values() ) {
				// pogressbar
				if ( (int) (100.*cnt/zones.getFacilities().size()) > percentDone ) {
					percentDone++ ; System.out.print('=') ;
				}
				cnt++;
				
				// running through network from given origin (from) zone
				Coord coord = fromZone.getCoord();
				assert( coord != null );
				Node fromNode = network.getNearestNode( coord );
				assert( fromNode != null );
				st.setOrigin( fromNode );
				st.run(network);
				
				// initialize accessibility for origin (from) zone
				double accessibility = 0.;
				double beta = -1.;
				
				// 
				for ( ActivityFacility toZone : zones.getFacilities().values() ) {
					Coord toCoord = toZone.getCoord();
					Node toNode = network.getNearestNode( toCoord );
					double arrivalTime = st.getTree().get(toNode.getId()).getTime();
					// travel times
					double ttime = arrivalTime - depatureTime ;
					
					// tnicolai test
					// travel costs
					//LinkImpl toLink = network.getNearestLink( toCoord );
					//double tcost = st.getTravelCostCalulator().getLinkGeneralizedTravelCost(toLink, depatureTime); // .getLinkTravelCost(toLink, depatureTime);
					// end tnicolai test
					
					// this sum corresponts to the sum term of the log sum computation
					if(numberOfWorkplacesPerZone.get(toZone.getId()) != null){
						long weight = numberOfWorkplacesPerZone.get(toZone.getId()).counter;
						double costFunction = Math.exp( beta * ttime );
						accessibility += weight * costFunction;
					}
					// yyyy should only be work facilities!!!! kai & thomas, dec'10
					
					travelDataWriter.write ( fromZone.getId().toString()	//origin zone id
							+ "," + toZone.getId().toString()				//destination zone id
							+ "," + ttime 									//tcost
							+ "," + ttime ) ;								//ttimes
					travelDataWriter.newLine();
				}
				
				if(accessibility < 1){
					isSmallerOne = true;
					messageBuffer.append("Zone: " + fromZone.getId());
					accessibility = 1;
				}
				zonesWriter.write( fromZone.getId().toString() + "," +  Math.log( accessibility ) ) ;
				zonesWriter.newLine();
				
			}
			// finish pograess bar
			System.out.println();
			// flush and close writers
			travelDataWriter.flush();
			travelDataWriter.close();
			log.info("... done with writing travel_data.csv" );
			zonesWriter.flush();
			zonesWriter.flush();
			log.info("... done with writing zones.csv" );
			
			if(isSmallerOne)
				log.warn(messageBuffer.toString());
			
			System.out.println(" ... done");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("... done with notifyShutdown.") ;
	}

}
