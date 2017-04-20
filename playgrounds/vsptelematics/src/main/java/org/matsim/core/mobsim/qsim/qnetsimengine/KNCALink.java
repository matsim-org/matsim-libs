/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.lanes.data.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

/**
 * The problem with this approach is that, because of the buffer, the vehicles do not go around in undisturbed circles.  kai, jun'16
 * 
 * @author nagel
 *
 */
public class KNCALink {
	private static Logger log = Logger.getLogger( KNCALink.class ) ;
	
	private static int LEN = (int) (10000/7.5) ;

	static class CAVehicle {
		QVehicle qVehicle ;
		int spd ;
		
		@Override
		public String toString() {
			return "[spd=" + spd + "] " ;
		}
	}

	static class MyQNetworkFactory extends QNetworkFactory {
		private final class QLaneIExtension extends QLaneI {
			private final class VisDataImplementation implements VisData {
				@Override public Collection<AgentSnapshotInfo> addAgentSnapshotInfo( Collection<AgentSnapshotInfo> positions, double now) {
					for ( int ii=MAXV ; ii<MAXV+LEN ; ii++ ) {
						if ( array[ii]!=null ) {
							Id<Person> personId = array[ii].qVehicle.getDriver().getId() ;
							AgentSnapshotInfo info = snapshotInfoFactory.createAgentSnapshotInfo(personId, link, ii*7.5, 0) ;
							info.setAgentState( AgentState.PERSON_DRIVING_CAR ) ;
							info.setColorValueBetweenZeroAndOne(0.5);
							positions.add(info) ;
						}
					}
					return positions ;
				}
			}

			private int MAXV = 5 ;
			private CAVehicle[] array = new CAVehicle[MAXV + LEN + MAXV] ;
			private Link link;
			private VisData visData = new VisDataImplementation() ;

			QLaneIExtension(Link link) {
				log.warn("here10");
				this.link = link ;
				double sum = 0 ;
				double dens = 0.13 ;
				for ( int ii=MAXV ; ii<MAXV+LEN ; ii++ ) {
					sum += dens ;
					if ( sum > 1 ) {
						sum-- ;
						Id<Vehicle> vehId = Id.createVehicleId( ii ) ;
						Id<Person> driverId = Id.createPersonId( ii ) ;
						CAVehicle caVeh = new CAVehicle() ;
						caVeh.spd = MAXV ;
						Vehicle veh = new Vehicle(){
							@Override public Id<Vehicle> getId() {
								return vehId ;
							}
							@Override public VehicleType getType() {
								return VehicleUtils.getDefaultVehicleType() ;
							}
						} ;
						caVeh.qVehicle = new QVehicle(veh) ;
						caVeh.qVehicle.setDriver( new MobsimDriverAgent(){
							@Override public Id<Person> getId() {
								return driverId ;
							}
							@Override
							public Id<Link> getCurrentLinkId() { 
								throw new RuntimeException("not implemented") ;
							}

							@Override
							public Id<Link> getDestinationLinkId() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}

							@Override
							public String getMode() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}

							@Override
							public void setVehicle(MobsimVehicle veh) {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}

							@Override
							public MobsimVehicle getVehicle() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}

							@Override
							public Id<Vehicle> getPlannedVehicleId() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}

							@Override
							public Id<Link> chooseNextLinkId() {
								throw new RuntimeException("not implemented") ;
							}

							@Override
							public void notifyMoveOverNode(Id<Link> newLinkId) {
							}

							@Override
							public boolean isWantingToArriveOnCurrentLink() {
								return false ;
							}
							@Override
							public State getState() {
								return State.LEG ;
							}
							@Override
							public double getActivityEndTime() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public void endActivityAndComputeNextState(double now) {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public void endLegAndComputeNextState(double now) {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public void setStateToAbort(double now) {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public Double getExpectedTravelTime() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public Double getExpectedTravelDistance() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public Facility<? extends Facility<?>> getCurrentFacility() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}
							@Override
							public Facility<? extends Facility<?>> getDestinationFacility() {
								// TODO Auto-generated method stub
								throw new RuntimeException("not implemented") ;
							}} );
						array[ii] = caVeh ;
					}
				}
				log.warn("here20");
			}
			
			@Override public Id<Lane> getId() {
				return Id.create("1", Lane.class ) ;
			}

			@Override void addFromWait(QVehicle veh) {
			}

			@Override boolean isAcceptingFromWait(QVehicle veh) {
				return false ;
			}

			@Override boolean isActive() {
				return true ;
			}

			@Override double getSimulatedFlowCapacityPerTimeStep() {
				return 0.5 ;
			}

			@Override QVehicle getVehicle(Id<Vehicle> vehicleId) {
				for ( int ii=MAXV ; ii<MAXV+LEN ; ii++ ) {
					if ( array[ii]!=null && array[ii].qVehicle.getId().equals( vehicleId ) ) {
						return array[ii].qVehicle ;
					}
				}
				return null ;
			}

			@Override double getStorageCapacity() {
				return LEN ;
			}

			@Override VisData getVisData() {
				return visData ;
			}

			@Override void addTransitSlightlyUpstreamOfStop(QVehicle veh) {
				throw new RuntimeException("not implemented") ;
			}

			@Override void changeUnscaledFlowCapacityPerSecond(double val) {
				throw new RuntimeException("not implemented") ;
			}

			@Override void changeEffectiveNumberOfLanes(double val) {
				throw new RuntimeException("not implemented") ;
			}

			@Override boolean doSimStep() {

				for ( int ii=0 ; ii<MAXV ; ii++ ) {
					array[ii] = array[LEN+ii] ;
					array[MAXV+LEN+ii] = array[MAXV+ii] ;
				}
				
				for ( int ii=MAXV ; ii<LEN+MAXV ; ii++ ) {
					if ( array[ii]!=null ) {
						int spdTmp = array[ii].spd ;
						spdTmp++ ;
						
						
						int gap = MAXV ;
						for ( int jj=ii+1 ; jj<=Math.min( ii+MAXV, array.length-1 ) ; jj++ ) {
							if ( array[jj]!=null ) {
								gap = jj-ii-1 ;
								break ;
							}
						}
						if ( spdTmp > gap ) {
							spdTmp = gap ;
						}
						
						if ( spdTmp > MAXV ) {
							spdTmp = MAXV ;
						}
						
						if ( array[ii].spd==0 ) {
							if ( spdTmp >=1 && MatsimRandom.getRandom().nextDouble() < 0.1 ) {
								array[ii].spd ++ ;
							}
						} else {
							array[ii].spd = spdTmp ;
							if ( array[ii].spd >= 1 && MatsimRandom.getRandom().nextDouble() < 0.1 ) {
								array[ii].spd -- ;
							}
						}
						
					}
				}
				
				for ( int ii=0 ; ii<MAXV ; ii++ ) {
					array[MAXV + LEN+ii] = null ;
				}

				
//				for ( int ii=0 ; ii<array.length ; ii++ ) {
//					log.warn( ii + ": " + (array[ii]==null ? "null " : array[ii].toString()  ) ) ;
//				}
				
				for ( int ii=MAXV+LEN-1 ; ii>=MAXV ; ii-- ) {
					if ( array[ii]!=null ) {
						int jj = ii + array[ii].spd ;
						if ( jj!=ii ) {
							if ( array[jj]!=null ) {
								log.warn("ii=" + ii + "; jj=" + jj ) ;
								throw new RuntimeException("exit") ;
							};
							array[jj] = array[ii] ;
							array[ii] = null ;
						}
					}
				}
				
				for ( int ii=0 ; ii<MAXV ; ii++ ) {
					if ( array[MAXV + LEN+ii] != null ) {
						array[MAXV+ii] = array[MAXV + LEN + ii ] ;
					}
				}

				return true ;
			}

			@Override void clearVehicles() {
				for ( int ii=0 ; ii<array.length ; ii++ ) {
					array[ii] = null ;
				}
			}

			@Override Collection<MobsimVehicle> getAllVehicles() {
				List<MobsimVehicle> list = new ArrayList<>() ; 
				for ( int ii=MAXV ; ii<MAXV+LEN ; ii++ ) {
					if ( array[ii]!=null ) {
						list.add( array[ii].qVehicle ) ;
					}
				}
				return list ;
			}

			@Override void addFromUpstream(QVehicle veh) {
			}

			@Override boolean isNotOfferingVehicle() {
				return true ;
			}

			@Override QVehicle popFirstVehicle() {
				return null ;
			}

			@Override QVehicle getFirstVehicle() {
				return null ;
			}

			@Override
			double getLastMovementTimeOfFirstVehicle() {
				return 0. ;
			}

			@Override boolean hasGreenForToLink(Id<Link> toLinkId) {
				return true ;
			}

			@Override boolean isAcceptingFromUpstream() {
				return false ;
			}

			@Override void changeSpeedMetersPerSecond(double val) {
				throw new RuntimeException("not implemented") ;
			}

			@Override double getLoadIndicator() {
				return 0.5 ;
			}

            @Override
            void initBeforeSimStep() {
            }
		}
		@Inject private EventsManager events ;
		@Inject private Scenario scenario ; // yyyyyy I would like to get rid of this. kai, mar'16
		@Inject private Network network ;
		@Inject private QSimConfigGroup qsimConfig ;

		private NetsimEngineContext context;
		private NetsimInternalInterface netsimEngine;
		private AgentSnapshotInfoFactory snapshotInfoFactory;

		@Override void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1) {
			double effectiveCellSize = ((Network)network).getEffectiveCellSize() ;

			SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
			linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
			if (! Double.isNaN(network.getEffectiveLaneWidth())){
				linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
			}
			AbstractAgentSnapshotInfoBuilder snapshotBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
			this.snapshotInfoFactory = new AgentSnapshotInfoFactory( linkWidthCalculator );


			this.context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, snapshotBuilder, qsimConfig, mobsimTimer, linkWidthCalculator ) ;

			this.netsimEngine = netsimEngine1 ;
		}
		@Override QNode createNetsimNode(Node node) {
			QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
			return builder.build( node ) ;

		}
		@Override QLinkI createNetsimLink(Link link, QNode queueNode) {

			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine) ;
			linkBuilder.setLaneFactory( new LaneFactory(){
				@Override public QLaneI createLane(AbstractQLink qLinkImpl) {
					QLaneI lane = new QLaneIExtension(qLinkImpl.getLink()) ;
					return lane ;
				}
			});

			return linkBuilder.build(link, queueNode) ;
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;

		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Collection<String> sf = new ArrayList<>() ;
		sf.add("otfvis") ;
		config.controler().setSnapshotFormat(sf);
		config.controler().setWriteSnapshotsInterval(1);
		
		config.qsim().setStartTime(0);
		config.qsim().setSimStarttimeInterpretation( StarttimeInterpretation.onlyUseStarttime );
		config.qsim().setEndTime(10.*3600.) ;
		config.qsim().setSimEndtimeInterpretation( EndtimeInterpretation.onlyUseEndtime );
		
		config.qsim().setNumberOfThreads(1);
		
		config.qsim().setSnapshotPeriod(1);
		
		OTFVisConfigGroup otfconf = ConfigUtils.addOrGetModule(config,  OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
		otfconf.setColoringScheme( ColoringScheme.byId );

		Scenario scenario = ScenarioUtils.createScenario(config) ;
		
		Network net = scenario.getNetwork() ;
		NetworkFactory nf = net.getFactory() ;
		Node node1 = nf.createNode(Id.createNodeId(1), new Coord( 0., 0. ) ) ;
		net.addNode( node1 );
		Node node2 = nf.createNode(Id.createNodeId(2), new Coord( LEN*7.5, 0. ) ) ;
		net.addNode( node2 );
		Node node3 = nf.createNode(Id.createNodeId(3), new Coord( 0., -LEN*7.5 ) ) ;
		net.addNode( node3 );
		Node node4 = nf.createNode(Id.createNodeId(4), new Coord( 0., LEN*7.5 ) ) ;
		net.addNode( node4 );
		Link link = nf.createLink(Id.createLinkId("1-2"), node1, node2) ;
		link.setLength( LEN*7.5 );
		net.addLink( link ) ;
//		Link link2 = nf.createLink(Id.createLinkId("1-3"), node1, node3) ;
//		link.setLength( LEN*7.5 );
//		net.addLink( link2 ) ;

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				bind( QNetworkFactory.class ).to( MyQNetworkFactory.class ) ;
			}
		});

		controler.addOverridingModule( new OTFVisLiveModule() ) ;
		controler.addOverridingModule( new OTFVisFileWriterModule() ) ;

		controler.run();
	}

}
