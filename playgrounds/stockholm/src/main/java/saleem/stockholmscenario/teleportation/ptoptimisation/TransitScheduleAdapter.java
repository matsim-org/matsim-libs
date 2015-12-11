package saleem.stockholmscenario.teleportation.ptoptimisation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

public class TransitScheduleAdapter {
	Scenario scenario;
	public TransitScheduleAdapter(Scenario scenario) {
		this.scenario=scenario;
	}
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
	public void addVehicles(double sample){
		VehicleAdder vehadder = new VehicleAdder(scenario);
		vehadder.addVehicles(sample);
		
	}
}
