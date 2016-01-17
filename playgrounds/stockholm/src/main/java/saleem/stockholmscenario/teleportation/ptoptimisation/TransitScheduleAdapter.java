package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

public class TransitScheduleAdapter {
	Scenario scenario;
	public TransitScheduleAdapter(Scenario scenario) {
		this.scenario=scenario;
	}
	public void updateSchedule(){
		VehicleRemover vehremover = new VehicleRemover(scenario);
		VehicleAdder vehadder = new VehicleAdder(scenario);
		Map<Id<TransitLine>, TransitLine> lines = scenario.getTransitSchedule().getTransitLines();
		Iterator<Id<TransitLine>> lineids = lines.keySet().iterator();
		while(lineids.hasNext()){
			TransitLine tline = lines.get(lineids.next());
			if(Math.random()<=0.05){//With 5% probability
				if(Math.random()<=0.5){//With 50% probability
					vehadder.addDeparturesToLine(tline, 0.1);//Adds 10 % departures and corresponding vehicles from tline
				}
				else {
					vehremover.removeDeparturesFromLine(tline, 0.1);//Removes  10 % departures and corresponding vehicles from tline
				}
			}
		}
	}
	//Remove vehicles from the transitschedule according to the sample size
	public void removeVehicles(double sample){
		VehicleRemover rveh = new VehicleRemover(scenario);
		rveh.removeVehicles(sample);
		rveh.removeDeletedVehicleDepartures();
		
	}
	public void writeSchedule(String name){
		
		TransitScheduleWriter tw = new TransitScheduleWriter(scenario.getTransitSchedule());
		tw.writeFile("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\"+name);
	}
	public void writeVehicles(String name){
		VehicleWriterV1 vwriter = new VehicleWriterV1(scenario.getTransitVehicles());
		vwriter.writeFile("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\"+name);
	}
	//Add vehicles to the transitschedule according to the sample size
	public void addVehicles(double sample){
		VehicleAdder vehadder = new VehicleAdder(scenario);
		vehadder.addVehicles(sample);
		
	}
	public static void main(String[] args){
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
        Config config = ConfigUtils.loadConfig(path);
        MatsimServices controler = new Controler(config);
        TransitScheduleAdapter adapter = new TransitScheduleAdapter(controler.getScenario());
		adapter.updateSchedule();
		adapter.writeSchedule("UpdatedSchedule.xml");
		adapter.writeVehicles("UpdatedVehicles.xml");

	}
}
