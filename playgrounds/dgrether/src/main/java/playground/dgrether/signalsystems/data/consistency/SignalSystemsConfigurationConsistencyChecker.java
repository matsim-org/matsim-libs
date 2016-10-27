/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsConsistencyChecker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether.signalsystems.data.consistency;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.Lanes;

import playground.dgrether.designdrafts.consistency.ConsistencyChecker;


/**
 * @author dgrether
 * @deprecated is only valid for the  1.1 model
 */
@Deprecated
public class SignalSystemsConfigurationConsistencyChecker implements ConsistencyChecker {
	
	private static final Logger log = Logger.getLogger(SignalSystemsConfigurationConsistencyChecker.class);
	
	private Network network;
	private Lanes lanes;
	private boolean removeMalformed = false;
	@Override
	public void checkConsistency() {
	}

//	private SignalSystems signals;
//
//	private SignalSystemConfigurations signalConfig;
//	
//	public SignalSystemsConfigurationConsistencyChecker(Network net, LaneDefinitions laneDefs, SignalSystems signals, SignalSystemConfigurations signalConfig) {
//		this.network = net;
//		this.lanes = laneDefs;
//		this.signals = signals;
//		this.signalConfig = signalConfig;
//	}
//	
//	public void checkConsistency() {
//		log.info("checking consistency...");
//		Set<SignalSystemConfiguration> malformedGroups = new HashSet<SignalSystemConfiguration>();
//
//		for (SignalSystemConfiguration ssc : this.signalConfig.getSignalSystemConfigurations().values()){
//			//check signal system reference
//			if (!this.signals.getSignalSystemDefinitions().containsKey(ssc.getSignalSystemId())){
//				log.error("No signal system found with id " + ssc.getSignalSystemId() + " signal system configuration not valid");
//				malformedGroups.add(ssc);
//			}
//			//check signal groups of plan based control
//			if (ssc.getControlInfo() instanceof PlanBasedSignalSystemControlInfo){
//				PlanBasedSignalSystemControlInfo pbci = (PlanBasedSignalSystemControlInfo) ssc.getControlInfo();
//				for (SignalSystemPlan plan : pbci.getPlans().values()){
//					for (SignalGroupSettings settings : plan.getGroupConfigs().values()){
//						if (!this.signals.getSignalGroupDefinitions().containsKey(settings.getReferencedSignalGroupId())){
//							log.error("Plan id " + plan.getId() + " of signalSystemConfiguration for signal system id " + ssc.getSignalSystemId() +
//									" references signalGroup id " + settings.getReferencedSignalGroupId() + " that is not defined.");
//							malformedGroups.add(ssc);
//						}
//					}
//				}
//			}
//		} //end for
//		
//		if (this.removeMalformed){
//			for (SignalSystemConfiguration id : malformedGroups) {
//				this.signalConfig.getSignalSystemConfigurations().remove(id.getSignalSystemId());
//			}
//		}
//		log.info("checked consistency.");
//	}
//	
//	public boolean isRemoveMalformed() {
//		return removeMalformed;
//	}
//
//	
//	public void setRemoveMalformed(boolean removeMalformed) {
//		this.removeMalformed = removeMalformed;
//	}
//
//	
//	
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		String netFile = DgPaths.IVTCHBASE + "baseCase/network/ivtch-osm.xml";
//		String lanesFile = DgPaths.STUDIESDG + "signalSystemsZh/laneDefinitions.xml";
//		String signalSystemsFile = DgPaths.STUDIESDG + "signalSystemsZh/signalSystems.xml";
//		String signalSystemsConfigFile = DgPaths.STUDIESDG + "signalSystemsZh/signalSystemsConfig.xml";
//		ScenarioImpl scenario = new ScenarioImpl();
//		NetworkImpl net = scenario.getNetwork();
//		new MatsimNetworkReader(scenario).readFile(netFile);
//	  log.info("read network");
//	  
//	  
//	  LaneDefinitions laneDefs = new LaneDefinitionsImpl();
//		MatsimLaneDefinitionsReader laneReader = new MatsimLaneDefinitionsReader(laneDefs );
//	  laneReader.readFile(lanesFile);
//	  
//	  SignalSystems signals = new SignalSystemsImpl();
//	  MatsimSignalSystemsReader signalReader = new MatsimSignalSystemsReader(signals);
//	  signalReader.readFile(signalSystemsFile);
//	  
//	  SignalSystemConfigurations signalConfig = new SignalSystemConfigurationsImpl();
//	  MatsimSignalSystemConfigurationsReader signalConfigReader = new MatsimSignalSystemConfigurationsReader(signalConfig);
//	  signalConfigReader.readFile(signalSystemsConfigFile);
//	  
//	  SignalSystemsConfigurationConsistencyChecker sscc = new SignalSystemsConfigurationConsistencyChecker(net, laneDefs, signals, signalConfig);
//		sscc.setRemoveMalformed(true);
//		sscc.checkConsistency();
//
//		
//		
////		MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(laneDefs);
////		laneWriter.writeFile(lanesFile);
//	}

}
