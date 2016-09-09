/* *********************************************************************** *
 * project: org.matsim.*
 * NewVisNetworkBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.vismodel;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.lanes.ModelLane;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.vis.VisLane;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.vis.VisSignal;
import org.matsim.contrib.signals.otfvis.VisSignalGroup;
import org.matsim.contrib.signals.otfvis.VisSignalSystem;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisLink;

import playground.dgrether.signalsystems.utils.DgSignalsUtils;


/**
 * @author dgrether
 */
public class VisScenarioBuilder {

	public VisScenario createVisScenario(Scenario scenario) {
		VisScenario visScenario = new VisScenario(scenario.getNetwork());
		CoordinateTransformation transform = visScenario.getVisTransform();
		createVisLinks(scenario, visScenario, transform);
		createVisSignals(scenario, visScenario, transform);
		return visScenario;
	}

	private void createVisSignals(Scenario scenario, VisScenario visScenario, CoordinateTransformation transform) {
		SignalsData signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		for (SignalSystemData ssd : signals.getSignalSystemsData().getSignalSystemData().values()){
			Set<Node> nodes = DgSignalsUtils.calculateSignalizedNodes4System(ssd, scenario.getNetwork());
			Node node = nodes.iterator().next();
			Coord visCoord = visScenario.getVisTransform().transform(node.getCoord());
			String systemIdS = ssd.getId().toString();
			VisSignalSystem visSignalSystem = new VisSignalSystem(systemIdS);
			visSignalSystem.setVisCoordinate(new Point2D.Float((float)visCoord.getX(), (float)visCoord.getY()));
			visScenario.getVisSignalSystemsByIdMap().put(systemIdS, visSignalSystem);
			
			Map<Id<SignalGroup>, SignalGroupData> groups = signals.getSignalGroupsData().getSignalGroupDataBySystemId(ssd.getId());
			for (SignalGroupData group : groups.values()){
				VisSignalGroup visGroup = new VisSignalGroup(systemIdS, group.getId().toString());
				visSignalSystem.addOTFSignalGroup(visGroup);
				for (Id<Signal> signalId : group.getSignalIds()){
					VisSignal visSignal = new VisSignal(systemIdS, signalId.toString());
					visGroup.addSignal(visSignal);
					SignalData signal = signals.getSignalSystemsData().getSignalSystemData().get(ssd.getId()).getSignalData().get(signalId);
					VisLinkWLanes visLink = visScenario.getLanesLinkData().get(signal.getLinkId().toString());
					if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
						visLink.addSignal(visSignal);
					}
					else {
						for (Id<Lane> laneId : signal.getLaneIds()){
							VisLane visLane = visLink.getLaneData().get(laneId.toString());
							visLane.addSignal(visSignal);
						}
					}
					
					if (! (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty())){
						for (Id<Link> outLinkId : signal.getTurningMoveRestrictions()){
							VisLinkWLanes vl = visScenario.getLanesLinkData().get(outLinkId.toString());
							visSignal.addTurningMoveRestriction(vl);
						}
					}
				}
			}
		}
	}

	private void createVisLinks(Scenario scenario, VisScenario visScenario,
			CoordinateTransformation transform) {
		VisLaneModelBuilder visLaneModelBuilder = new VisLaneModelBuilder();
		Lanes lanes = (Lanes) scenario.getLanes();
		for (Link l : scenario.getNetwork().getLinks().values()){
			LanesToLinkAssignment l2l = null;
			if (lanes != null) {
				l2l = lanes.getLanesToLinkAssignments().get(l.getId());
			}
			VisLink vl = new VisLinkImpl(l);
			List<ModelLane> la = null; 
			if (l2l != null) {
				la = LanesUtils.createLanes(l, l2l);
			}
			VisLinkWLanes link = visLaneModelBuilder.createVisLinkLanes(transform, vl, scenario.getConfig().qsim().getNodeOffset(),  la);
			SnapshotLinkWidthCalculator lwc = new SnapshotLinkWidthCalculator();
			visLaneModelBuilder.recalculatePositions(link, lwc);
			visScenario.getLanesLinkData().put(link.getLinkId(), link);
		}
		visLaneModelBuilder.connect(visScenario.getLanesLinkData());
	}
}
