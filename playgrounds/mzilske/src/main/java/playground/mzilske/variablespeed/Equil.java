package playground.mzilske.variablespeed;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;

public class Equil {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler().setMobsim("custom-qsim");
		QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
		// Strict handling of vehicles
		qSimConfigGroup.setVehicleBehavior(QSimConfigGroup.VEHICLE_BEHAVIOR_EXCEPTION);
		config.addQSimConfigGroup(qSimConfigGroup);
		config.planCalcScore().setWriteExperiencedPlans(true);
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		// install the custom mobsim
		controler.addMobsimFactory("custom-qsim", new VariableSpeedQSim());
		controler.run();
	}

}
