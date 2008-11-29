package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelTime;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.Layer;
import org.matsim.world.Location;

import playground.toronto.ttimematrix.SpanningTree;
import playground.toronto.ttimematrix.TTimeMatrixCalculator;
import playground.toronto.ttimematrix.SpanningTree.NodeData;
import sun.text.CompactShortArray.Iterator;

/**
 * 
 * @author nagel
 *
 */
public class MyControlerListener implements ShutdownListener {
	private static final Logger log = Logger.getLogger(MyControlerListener.class);
	
	Layer zones ;
	
	public MyControlerListener( Layer zones ) {
		this.zones = zones ;
	}

	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." ) ;

		// get the calling controler:
		Controler controler = event.getControler() ;

		TravelTime ttc = controler.getTravelTimeCalculator();
		SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc));
		
		NetworkLayer network = controler.getNetwork() ;
		double dpTime = 8.*3600 ;
		st.setDepartureTime(dpTime);

		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(Matsim4Urbansim.PATH_TO_OPUS_MATSIM+"tmp/travel_data_from_matsim.csv");

			log.info("Computing and writing travel_data" ) ;
			System.out.println("|--------------------------------------------------------------------------------------------------|") ;
			
			writer.write ( "from_zone_id:i4,to_zone_id:i4,single_vehicle_to_work_travel_cost:f4" ) ; writer.newLine();
			
			long cnt = 0 ; long percentDone = 0 ;
			for ( Location fromZone : zones.getLocations().values() ) {
				if ( (int) (100.*cnt/zones.getLocations().size()) > percentDone ) { 
					percentDone++ ; System.out.print('.') ; 
				}  
				cnt++ ;
				Coord coord = fromZone.getCenter() ;
				assert( coord != null ) ;
				Node fromNode = network.getNearestNode( coord ) ;
				assert( fromNode != null ) ;
				st.setOrigin( fromNode ) ;
				st.run(network) ;
				for ( Location toZone : zones.getLocations().values() ) {
					Coord toCoord = toZone.getCenter() ;
					Node toNode = network.getNearestNode( toCoord ) ;
					double arrTime = st.getTree().get(toNode.getId()).getTime();
					double ttime = arrTime - dpTime ;
					writer.write ( fromZone.getId().toString()
							+ "," + toZone.getId().toString()
							+ "," + ttime ) ;
					writer.newLine();
				}
			}
			System.out.println(" done") ;
			log.info("DONE with writing travel_data.tab" ) ;
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("... done with notifyShutdown.") ;
	}

}
