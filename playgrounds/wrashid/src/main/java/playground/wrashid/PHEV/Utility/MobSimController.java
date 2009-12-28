package playground.wrashid.PHEV.Utility;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.jdeqsim.util.Timer;



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
		controler.setOverwriteFiles(true);
		EventsManager events=controler.getEvents();
		
		
		ElectricCostHandler ecHandler=new ElectricCostHandler(controler,getEnergyConsumptionSamples(),events,"1");
		events.addHandler(ecHandler);
		
		
		controler.run();
		t.endTimer();
		t.printMeasuredTime("Time needed for MobSimController run: ");
		ecHandler.printRecordedSOC();
	}
	
	public static EnergyConsumptionSamples getEnergyConsumptionSamples(){
		EnergyConsumptionSamples ecs=new EnergyConsumptionSamples();
		
		// TODO: update values
		ecs.add(new AverageSpeedEnergyConsumption(5.555555556,3.173684E+02));
		ecs.add(new AverageSpeedEnergyConsumption(8.333333333,4.231656E+02));
		ecs.add(new AverageSpeedEnergyConsumption(11.11111111,5.549931E+02));
		ecs.add(new AverageSpeedEnergyConsumption(13.88888889,1.039878E+03));
		ecs.add(new AverageSpeedEnergyConsumption(16.66666667,4.056338E+02));
		ecs.add(new AverageSpeedEnergyConsumption(19.44444444,4.784535E+02));
		ecs.add(new AverageSpeedEnergyConsumption(22.22222222,5.580053E+02));
		ecs.add(new AverageSpeedEnergyConsumption(25,6.490326E+02));
		ecs.add(new AverageSpeedEnergyConsumption(27.77777778,7.502112E+02));
		ecs.add(new AverageSpeedEnergyConsumption(30.55555556,8.614505E+02));
		ecs.add(new AverageSpeedEnergyConsumption(33.33333333,1.179291E+03));
		ecs.add(new AverageSpeedEnergyConsumption(36.11111111,1.825931E+03));
		ecs.add(new AverageSpeedEnergyConsumption(38.88888889,2.418100E+03));
		ecs.add(new AverageSpeedEnergyConsumption(41.66666667,2.905639E+03));
		
		return ecs;
	}
	
}
