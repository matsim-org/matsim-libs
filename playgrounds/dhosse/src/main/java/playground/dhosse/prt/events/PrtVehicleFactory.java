package playground.dhosse.prt.events;

import java.io.File;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.controler.events.IterationStartsEvent;

import playground.dhosse.prt.PrtConfig;
import playground.michalm.taxi.data.*;

public class PrtVehicleFactory {

	private VrpData vrpData;
	private Scenario scenario;
	private PrtRankAndPassengerStatsHandler handler;
	private PrtConfig config;
	
	public PrtVehicleFactory(PrtConfig config, VrpData vrpData, Scenario scenario, PrtRankAndPassengerStatsHandler rankHandler){
		
		this.config = config;
		this.vrpData = vrpData;
		this.scenario = scenario;
		this.handler = rankHandler;
		
	}
	
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		if(event.getIteration() > 0){
			
			ETaxiData data = new ETaxiData();
			
			for(TaxiRank rank : ((ETaxiData)this.vrpData).getTaxiRanks().values()){
				data.addTaxiRank(rank);
			}
			
			for(Vehicle vehicle : this.vrpData.getVehicles().values()){
				data.addVehicle(new VehicleImpl(Id.create(vehicle.getId(), Vehicle.class),
						vehicle.getStartLink(), vehicle.getCapacity(), vehicle.getT0(), vehicle.getT1()));
			}
			
			double maxWTime = Double.NEGATIVE_INFINITY;
			TaxiRank maxWTimeRank = null;
			for(TaxiRank rank : ((ETaxiData)this.vrpData).getTaxiRanks().values()){
				if(this.handler.rankIds2PassengerWaitingTimes.containsKey(rank.getId())){
					double wtime = this.handler.rankIds2PassengerWaitingTimes.get(rank.getId()).getMax();
					if(wtime > maxWTime){
						maxWTime = wtime;
						maxWTimeRank = rank;
					}
				}
			}
			
			if(maxWTimeRank != null){
				data.addVehicle(new VehicleImpl(Id.create(maxWTimeRank.getId() + "_" + data.getVehicles().size(),
						Vehicle.class), maxWTimeRank.getLink(), config.getVehicleCapacity(),
						0, this.scenario.getConfig().qsim().getEndTime()));
			}
			
			VehicleWriter writer = new VehicleWriter(data.getVehicles().values());
			
			String path = this.config.getPrtOutputDirectory() + "it." + event.getIteration() + "/";
			File f = new File(path);
			f.mkdirs();
			
			writer.write(path + "prtVehicles.xml");
			
			//FIXME
			//this.context.setVrpData(data);
			
		}
		
	}

}
