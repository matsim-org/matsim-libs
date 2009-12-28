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
package playground.dgrether.signalsystems;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.lanes.basic.BasicLaneDefinitionsImpl;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.basic.BasicSignalSystemsImpl;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;

import playground.dgrether.DgPaths;
import playground.dgrether.consistency.ConsistencyChecker;


/**
 * @author dgrether
 *
 */
public class SignalSystemsConfigurationConsistencyChecker implements ConsistencyChecker {
	
	private static final Logger log = Logger.getLogger(SignalSystemsConfigurationConsistencyChecker.class);
	
	private Network network;
	private BasicLaneDefinitions lanes;
	private boolean removeMalformed = false;

	private BasicSignalSystems signals;

	private BasicSignalSystemConfigurations signalConfig;
	
	public SignalSystemsConfigurationConsistencyChecker(Network net, BasicLaneDefinitions laneDefs, BasicSignalSystems signals, BasicSignalSystemConfigurations signalConfig) {
		this.network = net;
		this.lanes = laneDefs;
		this.signals = signals;
		this.signalConfig = signalConfig;
	}
	
	public void checkConsistency() {
		log.info("checking consistency...");
		Set<BasicSignalSystemConfiguration> malformedGroups = new HashSet<BasicSignalSystemConfiguration>();

		for (BasicSignalSystemConfiguration ssc : this.signalConfig.getSignalSystemConfigurations().values()){
			//check signal system reference
			if (!this.signals.getSignalSystemDefinitions().containsKey(ssc.getSignalSystemId())){
				log.error("No signal system found with id " + ssc.getSignalSystemId() + " signal system configuration not valid");
				malformedGroups.add(ssc);
			}
			//check signal groups of plan based control
			if (ssc.getControlInfo() instanceof BasicPlanBasedSignalSystemControlInfo){
				BasicPlanBasedSignalSystemControlInfo pbci = (BasicPlanBasedSignalSystemControlInfo) ssc.getControlInfo();
				for (BasicSignalSystemPlan plan : pbci.getPlans().values()){
					for (BasicSignalGroupSettings settings : plan.getGroupConfigs().values()){
						if (!this.signals.getSignalGroupDefinitions().containsKey(settings.getReferencedSignalGroupId())){
							log.error("Plan id " + plan.getId() + " of signalSystemConfiguration for signal system id " + ssc.getSignalSystemId() +
									" references signalGroup id " + settings.getReferencedSignalGroupId() + " that is not defined.");
							malformedGroups.add(ssc);
						}
					}
				}
			}
		} //end for
		
		if (this.removeMalformed){
			for (BasicSignalSystemConfiguration id : malformedGroups) {
				this.signalConfig.getSignalSystemConfigurations().remove(id.getSignalSystemId());
			}
		}
		log.info("checked consistency.");
	}
	
	public boolean isRemoveMalformed() {
		return removeMalformed;
	}

	
	public void setRemoveMalformed(boolean removeMalformed) {
		this.removeMalformed = removeMalformed;
	}

	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFile = DgPaths.IVTCHBASE + "baseCase/network/ivtch-osm.xml";
		String lanesFile = DgPaths.STUDIESDG + "signalSystemsZh/laneDefinitions.xml";
		String signalSystemsFile = DgPaths.STUDIESDG + "signalSystemsZh/signalSystems.xml";
		String signalSystemsConfigFile = DgPaths.STUDIESDG + "signalSystemsZh/signalSystemsConfig.xml";
		NetworkLayer net = new NetworkLayer();
		MatsimNetworkReader netReader = new MatsimNetworkReader(net);
		netReader.readFile(netFile);
	  log.info("read network");
	  
	  
	  BasicLaneDefinitions laneDefs = new BasicLaneDefinitionsImpl();
		MatsimLaneDefinitionsReader laneReader = new MatsimLaneDefinitionsReader(laneDefs );
	  laneReader.readFile(lanesFile);
	  
	  BasicSignalSystems signals = new BasicSignalSystemsImpl();
	  MatsimSignalSystemsReader signalReader = new MatsimSignalSystemsReader(signals);
	  signalReader.readFile(signalSystemsFile);
	  
	  BasicSignalSystemConfigurations signalConfig = new BasicSignalSystemConfigurationsImpl();
	  MatsimSignalSystemConfigurationsReader signalConfigReader = new MatsimSignalSystemConfigurationsReader(signalConfig);
	  signalConfigReader.readFile(signalSystemsConfigFile);
	  
	  SignalSystemsConfigurationConsistencyChecker sscc = new SignalSystemsConfigurationConsistencyChecker((Network)net, laneDefs, signals, signalConfig);
		sscc.setRemoveMalformed(true);
		sscc.checkConsistency();

		
		
//		MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(laneDefs);
//		laneWriter.writeFile(lanesFile);
	}

}
