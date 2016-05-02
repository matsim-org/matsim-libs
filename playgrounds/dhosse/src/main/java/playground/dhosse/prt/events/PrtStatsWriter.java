package playground.dhosse.prt.events;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.minibus.genericUtils.RecursiveStatsContainer;
import org.matsim.core.controler.events.*;
import org.matsim.core.utils.io.IOUtils;

import playground.dhosse.prt.PrtConfig;
import playground.michalm.taxi.data.TaxiRank;

public class PrtStatsWriter {
	
	private Logger log = Logger.getLogger(PrtStatsWriter.class);
	
	private PrtConfig config;
	private CostContainerHandler costHandler;
	private PrtRankAndPassengerStatsHandler rankHandler;
	
	private String path;
	
	public PrtStatsWriter(PrtConfig config, CostContainerHandler costHandler, PrtRankAndPassengerStatsHandler rankHandler){
		
		this.config = config;
		this.costHandler = costHandler;
		this.rankHandler = rankHandler;
		
	}

	public void notifyIterationEnds(IterationEndsEvent event) {

		log.info("Finalizing unprocessed waiting time entries...");
		this.rankHandler.finalize(event);
		
		log.info("Writing PRT stats...");
		
		this.path = config.getPrtOutputDirectory() + "it." + event.getIteration();
		File f = new File(path);
		
		if(!f.exists()){
			f.mkdirs();
		}
		
		log.info("PRT stats output directory set to " + this.path);
		
		log.info("Writing cost and fare stats...");
		writePrtStats(event);
		
		log.info("Writing vehicle stats...");
		writeAppendedVehicleStats(event);
		writeVehicleStats(event);
		
		log.info("Writing rank stats...");
		writeRankStats(event);
		
		log.info("Writing passenger stats...");
		writeAppendedPassengerStats(event);
		writePassengerStats(event);
		
//		ChartWindowUtils.showFrame(TaxiScheduleChartUtils.chartSchedule(context.getVrpData()
//                .getVehicles()));
		
		log.info("...done.");
		
	}

	private void writeAppendedVehicleStats(IterationEndsEvent event) {
		
		BufferedWriter writer = null;
		String path = event.getServices().getControlerIO().getOutputPath() + "/vehicleStats.txt";
		
			try {
				
				if(event.getIteration() == 0){
					
					writer = IOUtils.getBufferedWriter(path);
					writer.write("it\ttasksServed\tmeters_travelled\tperson_meters_travelled\tcummulated_costs");
					writer.newLine();
					
				} else{
				
					writer = IOUtils.getAppendingBufferedWriter(path);
					
				}
				
				for(CostContainer cc : this.costHandler.getTaskContainersByVehicleId().values()){
					
					writer.write(event.getIteration() + "\t" +  cc.getTasksServed() + "\t" +
							cc.getMeterTravelled() + "\t" + cc.getPersonMetersTravelled() + "\t" +
							cc.getCummulatedCosts());
					writer.newLine();
					
				}
				
				writer.flush();
				writer.close();
				
			} catch (IOException e) {
				
				e.printStackTrace();
				
			}
		
	}

	private void writeAppendedPassengerStats(IterationEndsEvent event) {

		BufferedWriter writer = null;
		String path = event.getServices().getControlerIO().getOutputPath() + "/passengerStats.txt";
		
			try {
				if(event.getIteration() == 0){
					
					writer = IOUtils.getBufferedWriter(path);
					writer.write("it\twaitingTime");
					writer.newLine();
					
				} else{
				
					writer = IOUtils.getAppendingBufferedWriter(path);
					
				}
				
				for(Entry<Id<Person>,Double> waitingTimeEntry : this.rankHandler.personId2WaitingTime.entrySet()){
					
					writer.write(event.getIteration() + "\t" + Double.toString(waitingTimeEntry.getValue()));
					writer.newLine();
					
				}
				
				writer.flush();
				writer.close();
				
			} catch (IOException e) {
				
				e.printStackTrace();
				
			}
		
	}

	private void writePassengerStats(IterationEndsEvent event) {
		
		BufferedWriter writer = IOUtils.getBufferedWriter(path + "/passengerStats.txt");
		
		try {
			
			writer.write("person_id\twaiting_time");
			writer.newLine();
			
			for(Entry<Id<Person>,Double> waitingTimeEntry : this.rankHandler.personId2WaitingTime.entrySet()){
				
				writer.write(waitingTimeEntry.getKey().toString() + "\t" + Double.toString(waitingTimeEntry.getValue()));
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		
	}

	private void writeVehicleStats(IterationEndsEvent event) {
		
		BufferedWriter writer = IOUtils.getBufferedWriter(path + "/vehicleStats.txt");
		
		try {
			
			writer.write("vehicle_id\ttasksServed\tmeters_travelled\tperson_meters_travelled\tcummulated_costs\tpassengers");
			writer.newLine();
			
			for(Entry<Id<Vehicle>,CostContainer> cc : this.costHandler.getTaskContainersByVehicleId().entrySet()){
				
				StringBuffer passengersList = new StringBuffer();
				
				for(Id<Person> p : cc.getValue().getAgentsServed()){
					passengersList.append(p.toString() + ",");
				}
				
				writer.write(cc.getKey().toString() + "\t" +  cc.getValue().getTasksServed() + "\t" +
						cc.getValue().getMeterTravelled() + "\t" + cc.getValue().getPersonMetersTravelled() + "\t" +
						cc.getValue().getCummulatedCosts() + "\t" + passengersList.toString());
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

	private void writePrtStats(IterationEndsEvent event) {
		
		BufferedWriter writer = null;
		String path = event.getServices().getControlerIO().getOutputPath() + "/prtStats.txt";
		
		try {
			
			if(event.getIteration() == 0){
				writer = IOUtils.getBufferedWriter(path);
				writer.write("it\tn_vehicles\ttasks_served\tmeters_travelled\tperson_meters_travelled\ttotal_cost\tfare_per_pax\tfare_per_km");
				writer.newLine();
			} else{
				writer = IOUtils.getAppendingBufferedWriter(path);
			}
			
			
			writer.write(event.getIteration() + "\t" + this.costHandler.getTaskContainersByVehicleId().size() + "\t" +
					this.costHandler.getTasksServed() + "\t" +
					this.costHandler.getMetersTravelled() + "\t" + this.costHandler.getPersonMetersTravelled() + "\t" +
					this.costHandler.getCostsPerDay() + "\t" + this.costHandler.getFarePerPassenger() + "\t" +
					this.costHandler.getFarePerKm());
			writer.newLine();
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

	private void writeRankStats(IterationEndsEvent event) {
		
		BufferedWriter writer = IOUtils.getBufferedWriter(path + "/rankStats.txt");
		
		Map<Id<TaxiRank>, StringBuffer> stringMap = new HashMap<Id<TaxiRank>, StringBuffer>();
		
		for(Entry<Id<TaxiRank>, RecursiveStatsContainer> entry : this.rankHandler.rankIds2PassengerWaitingTimes.entrySet()){
			
			if(!stringMap.containsKey(entry.getKey())){
				stringMap.put(entry.getKey(), new StringBuffer());
			}
			
			stringMap.get(entry.getKey()).append("\t" + entry.getValue().getMax() + "\t" + entry.getValue().getMin() + "\t" + entry.getValue().getMean() + "\t" + entry.getValue().getStdDev());
			
		}
		
		for(Entry<Id<TaxiRank>,Integer> entry : this.rankHandler.rankIds2NumberOfBoardingPassengers.entrySet()){
			
			if(!stringMap.containsKey(entry.getKey())){
				
				stringMap.put(entry.getKey(), new StringBuffer());
				stringMap.get(entry.getKey()).append("\t0\t0\t0\t0");
				
			}
			
			stringMap.get(entry.getKey()).append("\t" + entry.getValue());
			
		}
		
		for(Entry<Id<TaxiRank>,Integer> entry : this.rankHandler.rankIds2NumberOfAlightingPassengers.entrySet()){
			
			if(!stringMap.containsKey(entry.getKey())){
				
				stringMap.put(entry.getKey(), new StringBuffer());
				stringMap.get(entry.getKey()).append("\t0\t0\t0\t0\t0");
				
			}
			StringBuffer b = stringMap.get(entry.getKey());
			if(!this.rankHandler.rankIds2NumberOfBoardingPassengers.containsKey(entry.getKey())){
				b.append("\t");
			}
			
			stringMap.get(entry.getKey()).append("\t" + entry.getValue());
			
		}
		
			try {
				
				writer.write("rank_id\tmax_wtime\tmin_wtime\tmean_wtime\tstdDev\tboarding\talighting");
				writer.newLine();
				
				for(Entry<Id<TaxiRank>,StringBuffer> entry : stringMap.entrySet()){
					
					String s = entry.getKey().toString() + entry.getValue().toString();
					writer.write(s);
					writer.newLine();
					
				}
				
				writer.flush();
				writer.close();
				
			} catch (IOException e) {
				
				e.printStackTrace();
				
			}
			
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		this.costHandler.reset(event.getIteration());
		this.rankHandler.reset(event.getIteration());
		
	}

}
