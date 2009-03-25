package playground.andreas.intersection.tl;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemControlInfo;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.signalsystems.control.SignalSystemControler;

import playground.andreas.intersection.QControler;

public class NewSignalSystemControlerImpl implements SignalSystemControler {
	
	private BasicSignalSystemControlInfo controlInfo;
	private Map<Id, BasicSignalSystemPlan> plans;
	private BasicSignalSystemPlan activePlan;
	private double defaultCirculationTime;

	public NewSignalSystemControlerImpl(BasicSignalSystemConfiguration basicLightSignalSystemConfiguration) {
		this.controlInfo = basicLightSignalSystemConfiguration.getControlInfo();
		this.plans = ((BasicPlanBasedSignalSystemControlInfo) this.controlInfo).getPlans();
		
		Logger.getLogger(QControler.class).warn("Plan Choice not implemented yet. Simulation could crash performing the next step.");
		// TODO [an] Plan is set static, no plan choice implemented, first one is taken
		this.activePlan = (BasicSignalSystemPlan) this.plans.values().toArray()[0];
	}

	public void setCirculationTime(double defaultCirculationTime) {
		this.defaultCirculationTime = defaultCirculationTime;		
	}

	public boolean givenSignalGroupIsGreen(BasicSignalGroupDefinition signalGroup) {
		
		if(this.activePlan == null){
			System.err.println("Got no active signal system plan.");
		}
		
		int currentSecondInPlan = 1 + ((int) (SimulationTimer.getTime() % this.defaultCirculationTime));
		
		BasicSignalGroupSettings tempSGConfig = this.activePlan.getGroupConfigs().get(signalGroup.getId());
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
