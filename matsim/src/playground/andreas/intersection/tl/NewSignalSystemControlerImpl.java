package playground.andreas.intersection.tl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.lightsignalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemControlInfo;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.lightsignalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.trafficlights.control.SignalSystemControler;
import org.matsim.trafficlights.data.PlanbasedSignalSystemControlInfoImpl;
import org.matsim.trafficlights.data.SignalGroupSettings;
import org.matsim.trafficlights.data.SignalSystemConfiguration;
import org.matsim.trafficlights.data.SignalSystemPlan;

import playground.andreas.intersection.QControler;
import playground.andreas.intersection.sim.QNode;

public class NewSignalSystemControlerImpl extends SignalSystemControler {
	
	private SignalSystemConfiguration signalSystemConfiguration;
	private BasicLightSignalSystemControlInfo controlInfo;
	private Map<Id, BasicLightSignalSystemPlan> plans;
	private BasicLightSignalSystemPlan activePlan;
	private double defaultCirculationTime;

	public NewSignalSystemControlerImpl(SignalSystemConfiguration signalSystemConfiguration) {
		this.signalSystemConfiguration = signalSystemConfiguration;
	}

	public NewSignalSystemControlerImpl(BasicLightSignalSystemConfiguration basicLightSignalSystemConfiguration) {
		this.controlInfo = basicLightSignalSystemConfiguration.getControlInfo();
		this.plans = ((BasicPlanBasedLightSignalSystemControlInfo) this.controlInfo).getPlans();
		
		Logger.getLogger(QControler.class).warn("Plan Choice not implemented yet. Simulation could crash performing the next step.");
		// TODO [an] Plan is set static, no plan choice implemented, first one is taken
		this.activePlan = (BasicLightSignalSystemPlan) this.plans.values().toArray()[0];
	}
	
//	public void notifyNodes(QueueNetwork network, List<BasicLightSignalGroupDefinition> sgList){
//		
//		ArrayList<QNode> nodesToBeInformed = new ArrayList<QNode>();
//		for (BasicLightSignalGroupDefinition basicLightSignalGroupDefinition : sgList) {
//						
//			for (Id toLinkId : basicLightSignalGroupDefinition.getToLinkIds()) {
//
//				QNode tempNode = (QNode) network.getNodes().get(network.getQueueLink(toLinkId).getLink().getFromNode().getId());
//
//				if(!nodesToBeInformed.contains(tempNode)){
//					nodesToBeInformed.add(tempNode);
//				}
//
//			}			
//		}
//		
//		for (QNode node : nodesToBeInformed) {
//			node.setNewSignalSystemControler(this);
//		}
//		
//		
//		
//	}
	
////	@Override
//	public List<Id> getGreenSGs(double time) {
//		
//		if(this.activePlan == null){
//			System.err.println("Got no active signal system plan.");
//		}
//		
//		int currentSecondInPlan = 1 + ((int) (time % this.defaultCirculationTime));
//		
//		ArrayList<Id> greenSGsList = new ArrayList<Id>();
//		
//		for (BasicLightSignalGroupConfiguration sgConfig : this.activePlan.getGroupConfigs().values()) {
//			
//			if ( sgConfig.getRoughCast() < currentSecondInPlan && currentSecondInPlan <= sgConfig.getDropping()){
//				// Debug only
////				System.out.println("green " + signalGroupSetting.getSignalGroupDefinition().toString());
//				
//				greenSGsList.add(sgConfig.getReferencedSignalGroupId());
//			} else {
////				System.out.println(" red " + signalGroupSetting.getSignalGroupDefinition().toString());
//			}
//			
//		}
//				
//		// Debug only
////		System.out.println(currentSecondInPlan);
//
//	
//		return greenSGsList;
//	}

//	@Override
//	public SignalGroupSettings[] getGreenInLinks(double time) {
//		
//		if(this.signalSystemConfiguration == null){
//			System.err.println("Got no signalSystemConfiguration");
//		}
//		
//		List<SignalSystemPlan> plan = ((PlanbasedSignalSystemControlInfoImpl) this.signalSystemConfiguration.getSignalSystemControler()).getSignalSystemPlans();
//		
//		
//		// TODO [an] Es wird nur der erste Plan genommen, aber nicht ausgewählt
//		SignalSystemPlan signalSystemPlan = plan.get(0);
//		
//		int currentSecondInPlan = 1 + ((int) (time % signalSystemPlan.getCirculationTime()));		
//		
//		// Debug only
////		System.out.println(currentSecondInPlan);
//		
//		ArrayList<SignalGroupSettings> greenLinksList = new ArrayList<SignalGroupSettings>();
//		
//		for (SignalGroupSettings signalGroupSetting : signalSystemPlan.getSignalGroupSettings().values()) {
//			
//			if ( signalGroupSetting.getRoughCast() < currentSecondInPlan && currentSecondInPlan <= signalGroupSetting.getDropping()){
//				// Debug only
////				System.out.println("green " + signalGroupSetting.getSignalGroupDefinition().toString());
//				
//				greenLinksList.add(signalGroupSetting);
//			} else {
////				System.out.println(" red " + signalGroupSetting.getSignalGroupDefinition().toString());
//			}
//			
//		}
//		
//		SignalGroupSettings[] greenLinks = new SignalGroupSettings[0];		
//		greenLinks = greenLinksList.toArray(greenLinks);
//		
//		// Debug only
////		for (int i = 0; i < greenLinks.length; i++) {
////			System.out.println(greenLinks[i].getSignalGroupDefinition().getId());
////		}
//	
//		return greenLinks;
//	}

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
		if ( tempSGConfig.getRoughCast() < currentSecondInPlan && currentSecondInPlan <= tempSGConfig.getDropping()){
			// Debug only
//			System.out.println("green " + signalGroupSetting.getSignalGroupDefinition().toString());
			return true;			
		} else {
			return false;
//			System.out.println(" red " + signalGroupSetting.getSignalGroupDefinition().toString());
		}				
		// Debug only
//		System.out.println(currentSecondInPlan);
		
	}

}
