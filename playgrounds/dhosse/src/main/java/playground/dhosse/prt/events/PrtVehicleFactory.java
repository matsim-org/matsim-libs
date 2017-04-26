package playground.dhosse.prt.events;

import java.io.File;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.controler.events.IterationStartsEvent;

import playground.dhosse.prt.PrtConfig;
import playground.michalm.taxi.data.*;

public class PrtVehicleFactory {

	private FleetImpl fleet;
	private TaxiRankDataImpl taxiRankData;
	private Scenario scenario;
	private PrtRankAndPassengerStatsHandler handler;
	private PrtConfig config;
	
	public PrtVehicleFactory(PrtConfig config, FleetImpl fleet, TaxiRankDataImpl taxiRankData, Scenario scenario, PrtRankAndPassengerStatsHandler rankHandler){
		
		this.config = config;
		this.fleet = fleet;
		this.taxiRankData = taxiRankData;
		this.scenario = scenario;
		this.handler = rankHandler;
		
	}
	
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		if(event.getIteration() > 0){
			
		    FleetImpl vData = new FleetImpl();
            TaxiRankDataImpl rData = new TaxiRankDataImpl();
			
			for(TaxiRank rank : (this.taxiRankData).getTaxiRanks().values()){
				rData.addTaxiRank(rank);
			}
			
			for(Vehicle vehicle : this.fleet.getVehicles().values()){
				vData.addVehicle(new VehicleImpl(Id.create(vehicle.getId(), Vehicle.class),
						vehicle.getStartLink(), vehicle.getCapacity(), vehicle.getServiceBeginTime(), vehicle.getServiceEndTime()));
			}
			
			double maxWTime = Double.NEGATIVE_INFINITY;
			TaxiRank maxWTimeRank = null;
			for(TaxiRank rank : (this.taxiRankData).getTaxiRanks().values()){
				if(this.handler.rankIds2PassengerWaitingTimes.containsKey(rank.getId())){
					double wtime = this.handler.rankIds2PassengerWaitingTimes.get(rank.getId()).getMax();
					if(wtime > maxWTime){
						maxWTime = wtime;
						maxWTimeRank = rank;
					}
				}
			}
			
			if(maxWTimeRank != null){
				vData.addVehicle(new VehicleImpl(Id.create(maxWTimeRank.getId() + "_" + vData.getVehicles().size(),
						Vehicle.class), maxWTimeRank.getLink(), config.getVehicleCapacity(),
						0, this.scenario.getConfig().qsim().getEndTime()));
			}
			
			VehicleWriter writer = new VehicleWriter(vData.getVehicles().values());
			
			String path = this.config.getPrtOutputDirectory() + "it." + event.getIteration() + "/";
			File f = new File(path);
			f.mkdirs();
			
			writer.write(path + "prtVehicles.xml");
			
			//FIXME
			//this.context.setVrpData(data);
			
		}
		
	}

}
