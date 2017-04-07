/**
 * 
 */
package playground.tschlenther.parkingSearch.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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

import playground.tschlenther.parkingSearch.utils.ParkingTuple;
import playground.tschlenther.parkingSearch.utils.ZoneParkingManager;

/**
 * @author Work
 *
 */
public class ZoneParkingOccupationListenerV2 implements MobsimBeforeCleanupListener, MobsimAfterSimStepListener{
	
	private final ZoneParkingManager zoneManager;
	private Logger log = Logger.getLogger(ZoneParkingOccupationListenerV2.class);
	private HashMap<String,TreeSet<ParkingTuple>> zoneOccupationPerTime; 
	private MatsimServices services;
	int iteration;
	
	private TreeSet<Double> allMonitoredTimeStamps;
	
	/**
	 * 
	 */
	@Inject
	public ZoneParkingOccupationListenerV2(ParkingSearchManager manager, MatsimServices services, Config config) {
		this.zoneManager = (ZoneParkingManager) manager;
		iteration = config.controler().getFirstIteration();
		this.services = services;
		this.zoneOccupationPerTime = new HashMap<String,TreeSet<ParkingTuple>>();
		for(String zone : zoneManager.getZones()){
			this.zoneOccupationPerTime.put(zone, new TreeSet<ParkingTuple>());
		}
		this.allMonitoredTimeStamps = new TreeSet<Double>();
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		if( (e.getSimulationTime()%900)  == 0 && !(e.getSimulationTime() == 0)){
			
			double	timeStamp = e.getSimulationTime();
			if (!(this.allMonitoredTimeStamps.contains(timeStamp)) ) this.allMonitoredTimeStamps.add(timeStamp);
			
			for(String zone : this.zoneOccupationPerTime.keySet()){
				double occ = this.zoneManager.getOccupancyRatioOfZone(zone);
				ParkingTuple tuple = new ParkingTuple(timeStamp, occ);
				this.zoneOccupationPerTime.get(zone).add(tuple);	
			}
		}
	}

	
	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		String fileName = services.getControlerIO().getIterationFilename(iteration, "OccupationStats_VERTICAL_it" + iteration + ".csv");
		writeStatsVertical(fileName);
		double lastOcc;
		for(String zone : zoneManager.getZones()){
			lastOcc = this.zoneManager.getOccupancyRatioOfZone(zone);
//			lastOcc = this.zoneOccupationPerHour.get(zone)[this.zoneOccupationPerHour.get(zone).length-1];
			TreeSet<ParkingTuple> tSet = new TreeSet<ParkingTuple>();
			tSet.add(new ParkingTuple(-1.0,lastOcc));
			this.zoneOccupationPerTime.put(zone, tSet);
			if(!this.allMonitoredTimeStamps.contains(-1.0)) this.allMonitoredTimeStamps.add(-1.0);
		}		
		iteration++;
	}
	
	private void writeStats(String fileName) {
		log.error("WRITING OCCUPANCY STATS TO " + fileName);

		String head = "Zone;TotalCapacity;";
		for(double d : this.allMonitoredTimeStamps){
			head += "" + d +";";
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			bw.write(head);
			for (String zone : this.zoneOccupationPerTime.keySet()){
				bw.newLine();
				String line = zone + ";" + this.zoneManager.getTotalCapacityOfZone(zone) + ";";

				TreeSet<ParkingTuple> currentSet = this.zoneOccupationPerTime.get(zone);
				TreeSet<Double> timeSet = new TreeSet<Double>();
				timeSet.addAll(allMonitoredTimeStamps);
				double oldOcc = -5.0;
				
				int zoneSize = currentSet.size();
				int timeSize = timeSet.size();
				
				for(int i = 0; i < zoneSize; i++){
					ParkingTuple ff = currentSet.pollFirst();
					for(int zz = 0; zz < timeSize; zz ++ ){
						double time = timeSet.pollFirst();
						if(time < ff.getTime()){
							line += "" + oldOcc +";";
						}
						else if( time == ff.getTime()){
							line += "" + ff.getOccupancy() + ";";
							break;
						}
						else{
							String timeString = "So sieht der TreeSet aus :\n" + timeSet.toString() + "\n So sieht das Original aus:\n" + this.allMonitoredTimeStamps.toString();
							throw new RuntimeException("Timestamp von zone " + zone + " wurde nicht aufgenommen.\n timeStamp von zone = " + ff.getTime() + "\t headerSpalte: " + time
									+ "\n\n" + timeString + "\n So sieht die Datei aus:\n" + head + "\n" + line);
						}
						oldOcc = ff.getOccupancy();
					}
				}
				bw.write(line);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	log.error("FINISHED WRITING OCCUPANCY STATS");
	}

	
	private void writeStatsVertical(String fileName) {
		log.error("WRITING OCCUPANCY STATS TO " + fileName);

		String head = ";;";
		String capacityLine = "TotalCapacity;;";
		String subheader = "SlotNr;SimTime;";
		
		ArrayList<double[]> zoneColumns = new ArrayList<double[]>();
		int numberOfSlots = this.allMonitoredTimeStamps.size();
		double[] slotTimes = new double[numberOfSlots];
		int[] slotNumbers = new int[numberOfSlots];
		TreeSet<Double> timeSet = new TreeSet<Double>();
		timeSet.addAll(allMonitoredTimeStamps);
		
		for (int timeSlot = 0; timeSlot < numberOfSlots; timeSlot ++ ){
			double tt = timeSet.pollFirst();
			slotTimes[timeSlot] = tt;
			slotNumbers[timeSlot] = (int) (tt/900); 
		}
		
		for(String zone : this.zoneManager.getZones()){
			head += zone +";";
			capacityLine += this.zoneManager.getTotalCapacityOfZone(zone) + ";";
			subheader += "OccRatio;";
			
			
			double[] ratios = new double[numberOfSlots];
			
			TreeSet<ParkingTuple> ratioSet = this.zoneOccupationPerTime.get(zone); 
			
			double oldRatio = 0.0;
			for(int i = 0; i < numberOfSlots; i++){
				
				ParkingTuple tuple = ratioSet.pollFirst(); 
				if(slotTimes[i] == tuple.getTime()){
					ratios[i] = tuple.getOccupancy();
					oldRatio = tuple.getOccupancy();
				}
				else if(slotTimes[i] < tuple.getTime()){
					ratios[i] = oldRatio;
				}
				else{
					throw new RuntimeException("timeSlot höher als aufgenommene Zeit. TimeSlot: " + slotTimes[i] + "\t Zeit: " + tuple.getTime());
				}
			}
			
			zoneColumns.add(ratios);			
		}
		
				BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			bw.write(head);
			bw.newLine();
			bw.write(capacityLine);
			bw.newLine();
			bw.write(subheader);
			
			
			for(int z = 0; z < numberOfSlots; z++){
				bw.newLine();
				
				String line = "" + slotNumbers[z] + ";" + slotTimes[z] + ";";
				for(int p = 0; p < this.zoneManager.getZones().size(); p++){
					line += "" + zoneColumns.get(p)[z] + ";";
				}
				
				bw.write(line);
			}
				
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	log.error("FINISHED WRITING OCCUPANCY STATS");
	}


}




