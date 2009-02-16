package playground.andreas.intersection.tl;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.signalsystems.control.SignalSystemControler;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemControlInfo;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.signalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.v01.Id;
import org.matsim.mobsim.queuesim.SimulationTimer;

import playground.andreas.intersection.QControler;

public class NewSignalSystemControlerImpl extends SignalSystemControler {
	
	private BasicLightSignalSystemControlInfo controlInfo;
	private Map<Id, BasicLightSignalSystemPlan> plans;
	private BasicLightSignalSystemPlan activePlan;
	private double defaultCirculationTime;

	public NewSignalSystemControlerImpl(BasicLightSignalSystemConfiguration basicLightSignalSystemConfiguration) {
		this.controlInfo = basicLightSignalSystemConfiguration.getControlInfo();
		this.plans = ((BasicPlanBasedLightSignalSystemControlInfo) this.controlInfo).getPlans();
		
		Logger.getLogger(QControler.class).warn("Plan Choice not implemented yet. Simulation could crash performing the next step.");
		// TODO [an] Plan is set static, no plan choice implemented, first one is taken
		this.activePlan = (BasicLightSignalSystemPlan) this.plans.values().toArray()[0];
	}

	public void setCirculationTime(double defaultCirculationTime) {
		this.defaultCirculationTime = defaultCirculationTime;		
	}

	@Override
	public boolean givenSignalGroupIsGreen(BasicLightSignalGroupDefinition signalGroup) {
		
		if(this.activePlan == null){
			System.err.println("Got no active signal system plan.");
		}
		
		int currentSecondInPlan = 1 + ((int) (SimulationTimer.getTime() % this.defaultCirculationTime));
		
		BasicLightSignalGroupConfiguration tempSGConfig = this.activePlan.getGroupConfigs().get(signalGroup.getId());
		if ( (tempSGConfig.getRoughCast() < currentSecondInPlan) && (currentSecondInPlan <= tempSGConfig.getDropping())){
			// Debug only
//			System.out.println("green " + signalGroupSetting.getSignalGroupDefinition().toString());
			return true;			
		}
		// else {
			return false;
//			System.out.println(" red " + signalGroupSetting.getSignalGroupDefinition().toString());
//		}
		
	}

}
