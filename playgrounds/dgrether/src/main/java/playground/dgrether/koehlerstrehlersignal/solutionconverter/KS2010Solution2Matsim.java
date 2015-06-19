/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010Solution2MatsimConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalSystem;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class KS2010Solution2Matsim {
	
	private static final Logger log = Logger.getLogger(KS2010Solution2Matsim.class);
	private DgIdPool idPool;
	private DgIdConverter idConverter;
	private int scale = 1;
	
	
	public KS2010Solution2Matsim(DgIdPool idPool){
		this.idPool = idPool;
		this.idConverter = new DgIdConverter(idPool);
	}
	
	
	private Map<Id<Node>, KS2010CrossingSolution> convertIds(DgIdPool idPool, List<KS2010CrossingSolution> solutionCrossings){
		DgIdConverter idConverter = new DgIdConverter(idPool);
		Map<Id<Node>, KS2010CrossingSolution> newMap = new HashMap<>();
		for (KS2010CrossingSolution sol : solutionCrossings){
			Id<Node> nodeId = idConverter.convertCrossingId2NodeId(sol.getCrossingId());
			sol.setCrossingId(Id.create(nodeId, DgCrossing.class));
			newMap.put(nodeId, sol);
		}
		return newMap;
	}
	
	/**
	 * overwrite all offsets in signalControl with the offsets from solutionCrossings.
	 * since solutionCrossings only contains crossings with nonzero offsets, 
	 * all offsets are reset to zero first.
	 * 
	 * @param signalControl
	 * @param solutionCrossings
	 */
	public void convertSolution(SignalControlData signalControl, 
			List<KS2010CrossingSolution> solutionCrossings){
		
		// reset all offsets to zero (solutions are only specified for nonzero offsets)
		for (SignalSystemControllerData controllerData : signalControl.
				getSignalSystemControllerDataBySystemId().values()){
			SignalPlanData plan = controllerData.getSignalPlanData().values().iterator().next();
			plan.setOffset(0);
		}
		
		// overwrite zero offsets with the ones from solutionCrossings
		for (KS2010CrossingSolution solution : solutionCrossings) {
//			if (! solution.getProgramIdOffsetMap().containsKey(M2KS2010NetworkConverter.DEFAULT_PROGRAM_ID)) {
				Id<DgProgram> programId = solution.getProgramIdOffsetMap().keySet().iterator().next();
				Id<SignalSystem> signalSystemId = this.idConverter.convertProgramId2SignalSystemId(programId);
				if (! signalControl.getSignalSystemControllerDataBySystemId().containsKey(signalSystemId)) {
					throw new IllegalStateException("something's wrong with program id " + programId 
							+ " = signal system id " + signalSystemId);
				}
				SignalSystemControllerData controllerData = signalControl.
						getSignalSystemControllerDataBySystemId().get(signalSystemId);
				if (! (controllerData.getSignalPlanData().size() == 1)) {
					throw new IllegalStateException("something's wrong");
				}
				SignalPlanData plan = controllerData.getSignalPlanData().values().iterator().next();
				int offset = solution.getProgramIdOffsetMap().get(programId);
				offset = offset * scale;
				plan.setOffset(offset);
				log.info("SignalSystem Id " + controllerData.getSignalSystemId() + " Offset: " + offset);
//			}
		}
	}

	
	
	public void convertSolutionOld(SignalControlData signalControl, Map<Id, KS2010CrossingSolution> solutionCrossingByIdMap){
		//TODO modify matching in case of more complex scenarios
		log.warn("Matching of signal system to node to solution might not be correct!");
		for (SignalSystemControllerData controllerData : signalControl.getSignalSystemControllerDataBySystemId().values()){
			log.debug("Processing control for signal system id : " + controllerData.getSignalSystemId());
			KS2010CrossingSolution solutionCrossing = solutionCrossingByIdMap.get(controllerData.getSignalSystemId());
			log.debug("  solution crossing : " + solutionCrossing.getCrossingId());
			for (SignalPlanData signalPlan : controllerData.getSignalPlanData().values()){
				Integer offset = solutionCrossing.getProgramIdOffsetMap().get(signalPlan.getId().toString());
				log.debug("  processing plan: " + signalPlan.getId() + " offset: " + offset);
				signalPlan.setOffset(offset);
			}
		}
	}


	public void setScale(int i) {
		log.warn("Setting scale to " + Integer.toString(i) + " this might not be intented. ");
		this.scale = i;
	}

	
	
}
