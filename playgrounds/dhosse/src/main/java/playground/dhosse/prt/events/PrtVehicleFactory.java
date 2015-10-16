package playground.dhosse.prt.events;

import java.io.File;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import playground.dhosse.prt.PrtConfigGroup;
import playground.michalm.taxi.data.*;

public class PrtVehicleFactory {

	private MatsimVrpContextImpl context;
	private PrtRankAndPassengerStatsHandler handler;
	private PrtConfigGroup config;
	
	public PrtVehicleFactory(PrtConfigGroup config, MatsimVrpContextImpl context, PrtRankAndPassengerStatsHandler rankHandler){
		
		this.config = config;
		this.context = context;
		this.handler = rankHandler;
		
	}
	
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		if(event.getIteration() > 0){
			
			ETaxiData data = new ETaxiData();
			
			for(TaxiRank rank : ((ETaxiData)this.context.getVrpData()).getTaxiRanks().values()){
				data.addTaxiRank(rank);
			}
			
			for(Vehicle vehicle : this.context.getVrpData().getVehicles().values()){
				data.addVehicle(new VehicleImpl(Id.create(vehicle.getId(), Vehicle.class),
						vehicle.getStartLink(), vehicle.getCapacity(), vehicle.getT0(), vehicle.getT1()));
			}
			
			double maxWTime = Double.NEGATIVE_INFINITY;
			TaxiRank maxWTimeRank = null;
			for(TaxiRank rank : ((ETaxiData)this.context.getVrpData()).getTaxiRanks().values()){
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
						0, this.context.getScenario().getConfig().qsim().getEndTime()));
			}
			
			VehicleWriter writer = new VehicleWriter(data.getVehicles().values());
			
			String path = this.config.getPrtOutputDirectory() + "it." + event.getIteration() + "/";
			File f = new File(path);
			f.mkdirs();
			
			writer.write(path + "prtVehicles.xml");
			
			this.context.setVrpData(data);
			
		}
		
	}

}
