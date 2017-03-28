/**
 * 
 */
package playground.tschlenther.parkingSearch.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.SortedSet;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.log4j.Logger;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

import playground.tschlenther.parkingSearch.utils.ZoneParkingManager;

/**
 * @author Work
 *
 */
public class ZoneParkingOccupationListener implements MobsimBeforeCleanupListener, MobsimAfterSimStepListener{
	
	private final ZoneParkingManager zoneManager;
	private Logger log = Logger.getLogger(ZoneParkingOccupationListener.class);
	private HashMap<String,double[]> zoneOccupationPerHour; 
	private int nrMonitoredHours = 26;
	private MatsimServices services;
	int iteration;
	
	
	/**
	 * 
	 */
	@Inject
	public ZoneParkingOccupationListener(ParkingSearchManager manager, MatsimServices services, Config config) {
		this.zoneManager = (ZoneParkingManager) manager;
		iteration = config.controler().getFirstIteration();
		this.services = services;
		this.zoneOccupationPerHour = new HashMap<String,double[]>();
		for(String zone : zoneManager.getZones()){
			this.zoneOccupationPerHour.put(zone, new double[1]);
		}
	}

	public void zwischenZeitlich(MobsimAfterSimStepEvent e){
		if( (e.getSimulationTime()%3600)  == 0 && !(e.getSimulationTime() == 0)){
			
			double[] oldOccArr;
			
			int hour = (int) (e.getSimulationTime() / 3600);
			
			log.error("ich merke mir die occupancy zur stunde h=" + hour );
			
			for(String zone : this.zoneOccupationPerHour.keySet()){
				
				oldOccArr= this.zoneOccupationPerHour.get(zone);
				
//				log.error("altes array von zone " + zone + ":\n" + printarray(oldOccArr));
				
				double[] newOccArr = new double[hour];
				
				for(int i = 0; i <= oldOccArr.length-1; i++){
					newOccArr[i] = oldOccArr[i];
				}
				
				for(int z = oldOccArr.length; z <= hour-2 ; z++){
					newOccArr[z] = oldOccArr[oldOccArr.length-1];
				}
				
				newOccArr[hour-1] = this.zoneManager.getOccupancyRatioOfZone(zone);
				
//				log.error("\n neues array:\n" + printarray(newOccArr));
				
				this.zoneOccupationPerHour.put(zone, newOccArr);
			}
		}
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
//		if( (e.getSimulationTime()%3600)  == 0 && !(e.getSimulationTime() == 0)){
//			int hour = (int) (e.getSimulationTime() / 3600);
//			log.error("ich merke mir die occupancy zur stunde h=" + hour );
//			for(String zone : this.zoneOccupationPerHour.keySet()){
////				log.error("merken für zone " + zone + ": occupancy ist gleich " + this.zoneManager.getOccupancyRatioOfZone(zone));
//				double[] occPerHour = this.zoneOccupationPerHour.get(zone);
////				log.error("occArray before overwriting: \n length = " + occPerHour.length + "\n" + printarray(occPerHour));
//				occPerHour[hour] = this.zoneManager.getOccupancyRatioOfZone(zone);
////				log.error("occArray after overwriting: \n "+ occPerHour.length + "\n" + printarray(occPerHour));
//				this.zoneOccupationPerHour.put(zone, occPerHour);
//			}
//		}
		zwischenZeitlich(e);
	}

	
	private String printarray(double[] arr){
		String str = "";
		for(int i = 0; i< arr.length; i++){
			str += "" + i + ":" + arr[i] + "\n";
		}
		return str;
	}

	
	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		String fileName = services.getControlerIO().getIterationFilename(iteration, "OccupationStats_it" + iteration + ".csv");
		writeStats(fileName);
		double lastOcc;
		for(String zone : zoneManager.getZones()){
			lastOcc = this.zoneManager.getOccupancyRatioOfZone(zone);
//			lastOcc = this.zoneOccupationPerHour.get(zone)[this.zoneOccupationPerHour.get(zone).length-1];
			double[] newOcc = new double[1];
			newOcc[0] = lastOcc;
			this.zoneOccupationPerHour.put(zone, newOcc);
		}		
		iteration++;
	}
	
	private void writeStats(String fileName) {
		log.error("WRITING OCCUPANCY STATS TO " + fileName);
//		log.error("nr of monitored hours= " + nrMonitoredHours);
//		log.error("nr of zones = " + this.zoneOccupationPerHour.keySet().size());
		String head = "Zone;TotalCapacity;";
		for(int i = 1; i < nrMonitoredHours; i++){
			head += "" + i +";";
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			bw.write(head);
			for (String zone : this.zoneOccupationPerHour.keySet()){
				bw.newLine();
				String line = zone + ";" + this.zoneManager.getTotalCapacityOfZone(zone) + ";";
//				for(int i = 0; i < nrMonitoredHours; i++){
//					line += "" +  this.zoneOccupationPerHour.get(zone)[i] +";";
//				}
				for(double d : this.zoneOccupationPerHour.get(zone)){
					line += "" + d +";" ;
				}
				bw.write(line);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}





