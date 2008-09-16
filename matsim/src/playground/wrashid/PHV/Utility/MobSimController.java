package playground.wrashid.PHV.Utility;

import org.matsim.controler.Controler;
import org.matsim.events.Events;

import playground.wrashid.DES.utils.Timer;


/**
 * The Controler is responsible for complete simulation runs, including
 * the initialization of all required data, running the iterations and
 * the replanning, analyses, etc.
 *
 * @author mrieser
 */
public class MobSimController extends Controler {

	public MobSimController(final String[] args) {
	    super(args);
	  }

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		final MobSimController controler = new MobSimController(args);
		Events events=controler.getEvents();
		
		
		ElectricCostHandler ecHandler=new ElectricCostHandler(controler.network,getEnergyConsumptionSamples(),events);
		events.addHandler(ecHandler);
		
		
		controler.run();
		t.endTimer();
		t.printMeasuredTime("Time needed for MobSimController run: ");
		controler.events.printEventsCount();
	}
	
	public static EnergyConsumptionSamples getEnergyConsumptionSamples(){
		EnergyConsumptionSamples ecs=new EnergyConsumptionSamples();
		
		// TODO: update values
		ecs.add(new AverageSpeedEnergyConsumption(5.555555556,8.815789E-05));
		ecs.add(new AverageSpeedEnergyConsumption(8.333333333,1.175460E-04));
		ecs.add(new AverageSpeedEnergyConsumption(11.11111111,1.541647E-04));
		ecs.add(new AverageSpeedEnergyConsumption(13.88888889,2.888550E-04));
		ecs.add(new AverageSpeedEnergyConsumption(16.66666667,1.126761E-04));
		ecs.add(new AverageSpeedEnergyConsumption(19.44444444,1.329037E-04));
		ecs.add(new AverageSpeedEnergyConsumption(22.22222222,1.550015E-04));
		ecs.add(new AverageSpeedEnergyConsumption(25,1.802868E-04));
		ecs.add(new AverageSpeedEnergyConsumption(27.77777778,2.083920E-04));
		ecs.add(new AverageSpeedEnergyConsumption(30.55555556,2.392918E-04));
		ecs.add(new AverageSpeedEnergyConsumption(33.33333333,3.275808E-04));
		ecs.add(new AverageSpeedEnergyConsumption(36.11111111,5.072029E-04));
		ecs.add(new AverageSpeedEnergyConsumption(38.88888889,6.716944E-04));
		ecs.add(new AverageSpeedEnergyConsumption(41.66666667,8.071218E-04));
		
		return ecs;
	}
	
}
