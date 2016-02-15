/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyServer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFLiveServer;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.matsim.vis.otfvis.utils.WGS84ToMercator;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisNetwork;

/**
 * OnTheFlyServer is the live server of the OTFVis.
 * it is injected into a running QueueSimulation by either overloading the
 * simulation's afterSimStep() method or via the external trigger events.
 * The simulation should call on every tick the method updateStatus().
 * Then the server can stop the simulation run whenever wanted.
 * The interface to the client is given by the OTFLiveServerRemote interface.
 *
 * @author dstrippgen
 *
 */
public class OnTheFlyServer extends PlayPauseSimulationControl implements OTFLiveServer {

	private final class CurrentTimeStepView implements SimulationViewForQueries {

		@Override
		public Map<Id<Person>, Plan> getPlans() {
			Map<Id<Person>, Plan> plans = new HashMap<>();
			if (visMobsim != null ) {
				Collection<MobsimAgent> agents = visMobsim.getAgents();
				for (MobsimAgent agent : agents) {
					if (agent instanceof PlanAgent) {
						PlanAgent pa = (PlanAgent) agent;
						plans.put(pa.getCurrentPlan().getPerson().getId(), pa.getCurrentPlan());
					}
				}
			} else {
				for (Person person : scenario.getPopulation().getPersons().values()) {
					Plan plan = person.getSelectedPlan();
					plans.put(person.getId(), plan);
				}
			}
			return plans;
		}

		@Override
		public Network getNetwork() {
			return scenario.getNetwork();
		}

		@Override
		public EventsManager getEvents() {
			return events;
		}

		@Override
		public void addTrackedAgent(Id<Person> agentId) {

		}

		@Override
		public void removeTrackedAgent(Id<Person> agentId) {

		}

		@Override
		public VisNetwork getVisNetwork() {
			return visMobsim.getVisNetwork();
		}

		@Override
		public OTFServerQuadTree getNetworkQuadTree() {
			return quad;
		}

		@Override
		public VisData getNonNetwokAgentSnapshots() {
			if (visMobsim != null) {
				return visMobsim.getNonNetworkAgentSnapshots();
			} else {
				return visData;
			}
		}

	}

	private static final Logger log = Logger.getLogger(OnTheFlyServer.class);

	private OTFServerQuadTree quad;

	private final List<OTFDataWriter<?>> additionalElements= new LinkedList<OTFDataWriter<?>>();

	private EventsManager events;

	private Collection<AbstractQuery> activeQueries = new ArrayList<AbstractQuery>();

	private final ByteBuffer buf = ByteBuffer.allocate(80000000);

	private VisMobsim visMobsim;

	@SuppressWarnings("rawtypes") // should work with arbitrary "agent" types. kai, sep'14
	private Map<Id, AgentSnapshotInfo> snapshots = new LinkedHashMap<Id, AgentSnapshotInfo>();

	private final VisData visData = new VisData() {

		@Override
		public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
			return snapshots.values();
		}

	};

	private final CurrentTimeStepView currentTimeStepView = new CurrentTimeStepView();

	private Scenario scenario;

	private Map<Id<Person>, Plan> plans = new HashMap<Id<Person>, Plan>();
	
	OnTheFlyServer(Scenario scenario, EventsManager events) {
		this.scenario = scenario;
		this.events = events; 
	}

	public static OnTheFlyServer createInstance(Scenario scenario, EventsManager events) {
		OnTheFlyServer instance = new OnTheFlyServer(scenario, events);
		return instance;
	}

	@Override
	public boolean isLive() {
		return true;
	}

	@Override
	public OTFServerQuadTree getQuad(OTFConnectionManager connect) {
		if (quad != null) {
			return quad;
		} else {
			Config config = this.scenario.getConfig();
			this.setShowNonMovingItems(ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).isDrawNonMovingItems());
			String scenarioCRS = config.global().getCoordinateSystem();
			int maxZoom = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).getMaximumZoom();
			if (ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).isMapOverlayMode()) {
				// Transform everything from network coordinates first to WGS84 and then to standard mercator projection as used by OSM tiles and Geoserver.
				// The user needs to know the maximum zoom level they would like to use beforehand.
				// The coordinates are transformed so that one unit is one pixel in the maximum zoom level.
				final CoordinateTransformation scenario2wgs84 = TransformationFactory.getCoordinateTransformation(scenarioCRS, TransformationFactory.WGS84);
				final CoordinateTransformation wgs842Mercator = new WGS84ToMercator.Project(maxZoom);

				// Must be called before the constructor of LiveServerQuadTree
				// TODO: Get rid of global variables.
				OTFServerQuadTree.setTransformation(new CoordinateTransformation() {

					@Override
					public Coord transform(Coord coord) {
						return wgs842Mercator.transform(scenario2wgs84.transform(coord));
					}

				});
			}
			if (this.visMobsim != null) {
				quad = new LiveServerQuadTree(this.visMobsim.getVisNetwork());
				if (ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).isShowTeleportedAgents()) {
					OTFAgentsListHandler.Writer teleportationWriter;
					teleportationWriter = new OTFAgentsListHandler.Writer();
					teleportationWriter.setSrc(this.visMobsim.getNonNetworkAgentSnapshots());
					quad.addAdditionalElement(teleportationWriter);
				}
			} else {
				quad = new SnapshotWriterQuadTree(this.scenario.getNetwork());
				OTFAgentsListHandler.Writer teleportationWriter;
				teleportationWriter = new OTFAgentsListHandler.Writer();
				teleportationWriter.setSrc(visData);
				quad.addAdditionalElement(teleportationWriter);
			}
			quad.initQuadTree(connect);

			for(OTFDataWriter<?> writer : additionalElements) {
				log.info("Adding additional element: " + writer.getClass().getName());
				quad.addAdditionalElement(writer);
			}
			return quad;
		}
	}

	@Override
	public void setShowNonMovingItems(boolean showParking) {
		OTFLinkAgentsHandler.showParked = showParking;
	}

	@Override
	public byte[] getQuadConstStateBuffer() {
		try {
			getAccessToQNetwork().acquire();
			byte[] result;
			buf.position(0);
			quad.writeConstData(buf);
			int pos = buf.position();
			result = new byte[pos];
			buf.position(0);
			buf.get(result);
			return result;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			getAccessToQNetwork().release();
		}
	}

	@Override
	public byte[] getQuadDynStateBuffer(final QuadTree.Rect bounds) {
		try {
			getAccessToQNetwork().acquire();
			byte[] result;
			buf.position(0);
			quad.writeDynData(bounds, buf);
			int pos = buf.position();
			result = new byte[pos];
			buf.position(0);
			buf.get(result);
			return result;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			getAccessToQNetwork().release();
		}
	}

	@Override
	public OTFQueryRemote answerQuery(AbstractQuery query) {
		try {
			getAccessToQNetwork().acquire();
			query.installQuery(currentTimeStepView);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			getAccessToQNetwork().release();
		}
		activeQueries.add(query);
		return query;
	}

	@Override
	public void removeQueries() {
		for (AbstractQuery query : activeQueries) {
			query.uninstall();
		}
		activeQueries.clear();
	}

	@Override
	public Collection<Double> getTimeSteps() {
		// There are no timesteps implemented here right now, so we return null instead
		return null;
	}

	public void addAdditionalElement(OTFDataWriter<?> element) {
		this.additionalElements.add(element);
	}

	public void setSimulation(VisMobsim visMobsim) {
		this.visMobsim = visMobsim;
	}

	@Override
	public OTFVisConfigGroup getOTFVisConfig() {
		OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(this.scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		if (otfVisConfig == null) {
			otfVisConfig = new OTFVisConfigGroup();
		}
		double effLaneWidth = this.scenario.getNetwork().getEffectiveLaneWidth() ;
		if (! Double.isNaN(effLaneWidth)){
			otfVisConfig.setEffectiveLaneWidth( effLaneWidth ) ;
		}
		return otfVisConfig ;
	}

//	public SnapshotWriter getSnapshotReceiver() {
//		return new SnapshotWriter() {
//
//			@Override
//			public void beginSnapshot(double time) {
//				snapshots.clear();
//				setLocalTime((int) time);
//			}
//
//			@Override
//			public void endSnapshot() {
//
//			}
//
//			@Override
//			public void addAgent(AgentSnapshotInfo position) {
//				snapshots.put(position.getId(), position);
//			}
//
//			@Override
//			public void finish() {
//				setStatus(Status.FINISHED);
//			}
//
//		};
//	}
	// this does not seem to be used anywhere. kai, mar'15

	public void addAdditionalPlans(Map<Id<Person>, Plan> additionalPlans) {
		this.plans.putAll(additionalPlans);
	}

	@Override
	public boolean requestNewTime(final int time, final TimePreference searchDirection) {
		if ( isFinished() ) {
			return true;
//		} else if( ((searchDirection == TimePreference.RESTART) && (time < getLocalTime()))) { 
//			doStep(time);
//			return true;
			// not doing anything meaningful anywhere (I think).  kai, jan'16
		} else if (time < getLocalTime()) {
			// if requested time lies in the past, sorry we cannot do that right now

			setStepToTime(0);
			// yyyy I think we could as well call "pause" (and should do that for clarity and encapsulation).  However, I am unsure
			// if the current pause method releases enough of the monitors. kai, jan'16
			
			// if forward search is OK, then the actual timestep is the BEST fit
			return (searchDirection != TimePreference.EARLIER);
		} else if (time == getLocalTime()) {
			setStepToTime(0); // see above
			return true;
		} else {
			doStep(time);
			return true;
		}
	}

}