package playground.mzilske.withinday;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.qsim.PersonDriverPassengerAgent;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

public class AdapterAgent implements PersonDriverPassengerAgent, SimulationBeforeSimStepListener {
	
	Id id;
	
	RealAgent realAgent;

	protected PlanElement currentPlanElement;

	protected TeleportationBehavior teleportationBehavior;
	
	private DrivingBehavior drivingBehavior;
	
	private Id nextLinkId;
	
	TeleportationWorld teleportationWorld = new TeleportationWorld() {

		@Override
		public double getTime() {
			return now;
		}

		@Override
		public void stop() {
			System.out.println("Agent wants to stop, but stopping teleportation isn't supported.");
			// yy stopping is supported nearly nowhere, since it does not make sense.  An airplane can also not
			// just stop.  Shouldn't we get rid of this method?  kai, jun'11
		}
		
	};
	
	DrivingWorld drivingWorld = new DrivingWorld() {

	    @Override
	    public double getTime()
	    {
	        return now;
	    }
	    
	    
		@Override
		public void park() {
			
		}

		@Override
		public void nextTurn(Id nextLinkId) {
			// storing nextLinkId internally:
			AdapterAgent.this.nextLinkId = nextLinkId;
			
			// pushing nextLinkId to the "plan" agent (this should not really be necessary ... although it might make sense
			// also in the context of getExecutedPlan()) 
			((NetworkRoute) ((Leg) currentPlanElement).getRoute()).setEndLinkId(nextLinkId);
		}

		@Override
		public boolean requiresAction() {
			if (nextLinkId == null) {
				return true;
			} else {
				return false;
			}
		}
		
	};
	
	World world = new World() {

		@Override
		public ActivityPlane getActivityPlane() {
			return new ActivityPlane() {

				@Override
				public void startDoing(ActivityBehavior activityBehavior) {
					AdapterAgent.this.activityBehavior = activityBehavior;
					eventsManager.processEvent(eventsManager.getFactory().createActivityStartEvent(now, id, currentLinkId, null, activityBehavior.getActivityType()));
					ActivityImpl activityImpl = new ActivityImpl(activityBehavior.getActivityType(), currentLinkId);
					activityImpl.setEndTime(Double.POSITIVE_INFINITY);
					AdapterAgent.this.currentPlanElement = activityImpl;
					simulation.scheduleActivityEnd(AdapterAgent.this);
				}
				
			};
		}

		@Override
		public TeleportationPlane getTeleportationPlane() {
			return new TeleportationPlane() {

				@Override
				public void startTeleporting(TeleportationBehavior teleportTo) {
					Id destination = teleportTo.getDestinationLinkId();
					if (destination == null) {
						throw new RuntimeException();
					}
					LegImpl legImpl = new LegImpl(teleportTo.getMode());
					legImpl.setRoute(new GenericRouteImpl(currentLinkId, destination));
					legImpl.setTravelTime(teleportTo.getTravelTime());
					AdapterAgent.this.currentPlanElement = legImpl;
					simulation.arrangeAgentDeparture(AdapterAgent.this);
					AdapterAgent.this.teleportationBehavior = teleportTo;
				}
				
			};
		}

		@Override
		public double getTime() {
			return now;
		}

		@Override
		public void done() {
			System.out.println("I'm done. I am at: " + currentLinkId);
			simulation.getAgentCounter().decLiving();
			AdapterAgent.this.living = false;
		}

		@Override
		public Id getLocation() {
			return currentLinkId;
		}

		@Override
		public RoadNetworkPlane getRoadNetworkPlane() {
			return new RoadNetworkPlane() {

				@Override
				public void startDriving(DrivingBehavior drivingBehavior) {
					AdapterAgent.this.drivingBehavior = drivingBehavior;
					LegImpl legImpl = new LegImpl(TransportMode.car);
					NetworkRoute route = new LinkNetworkRouteImpl(currentLinkId, currentLinkId);
					legImpl.setRoute(route);
					AdapterAgent.this.currentPlanElement = legImpl;
					drivingBehavior.doSimStep(drivingWorld);
					simulation.arrangeAgentDeparture(AdapterAgent.this);
				}
				
			};
		}
		
	};

	private double now;

	private QVehicle veh;
	
	private boolean firstTimeGetCurrentLinkId = true;

	private Id currentLinkId;

	private Mobsim simulation;

	private EventsManager eventsManager;

	private ActivityBehavior activityBehavior;

	private ActivityWorld activityWorld = new ActivityWorld() {

		@Override
		public double getTime() {
			return now;
		}

		@Override
		public void stopActivity() {
			System.out.println("I want to stop my activity.");
			eventsManager.processEvent(eventsManager.getFactory().createActivityEndEvent(now, id, currentLinkId, null, activityBehavior.getActivityType()));
			((Activity) currentPlanElement).setEndTime(now);
			simulation.rescheduleActivityEnd(AdapterAgent.this, Double.POSITIVE_INFINITY, now);

			simulation.getAgentCounter().decLiving(); 
			// This is necessary because the QSim thinks it must increase the living agents counter in the rescheduling step.  mz
			// The intention there was that an agent that is no longer alive has an activity end time of infinity (rather
			// than removing it completely from teh mobsim).  The number of
			// alive agents is only modified when an activity end time is changed between a finite time and infinite.  kai, jun'11

			activityBehavior = null;
		}
		
	};

	private Plan selectedPlan;

	public AdapterAgent(Plan selectedPlan, Mobsim simulation) {
	    this(selectedPlan, simulation, new MzPlanAgentImpl(selectedPlan));
    }
	
	public AdapterAgent(Plan selectedPlan, Mobsim simulation, RealAgent realAgent) {
		id = selectedPlan.getPerson().getId();
		this.selectedPlan = selectedPlan;

		//realAgent = new MzPlanAgentImpl(selectedPlan);
		this.realAgent = realAgent;
		
		currentLinkId = ((Activity) selectedPlan.getPlanElements().get(0)).getLinkId();
		this.simulation = simulation;
		this.eventsManager = simulation.getEventsManager();
	}
	
	
	private boolean living = true;

	@Override
	public void notifySimulationBeforeSimStep(@SuppressWarnings("rawtypes") SimulationBeforeSimStepEvent e) {
	    if (!living) {
	        return;//or maybe should be unregistered from listeners in World.done() method
	    }
	    
		now = e.getSimulationTime();
		if (teleportationBehavior != null) {
			teleportationBehavior.doSimStep(teleportationWorld);
		} else if (activityBehavior != null) {
			activityBehavior.doSimStep(activityWorld);
		} else if (drivingBehavior != null) {
			drivingBehavior.doSimStep(drivingWorld);
		}else {
			realAgent.doSimStep(world);
			// I guess you get here in the time step after endLeg/ActAndAssumeControl, since this particular agent does not 
			// do anything in those methods.  So the agent is in limbo for the time being.  Is "being in limbo" consistent with
			// the original design?  kai, jun'11
		}
	}

	@Override
	public Person getPerson() {
		// I often get asked about my Person, but all they really want to know
		// is my Id. Except for the visualizer, when it wants to visualize my Plan. mz
		//
		// The PlanAgent is in fact directly Identifiable, exactly for that reason.  Should be push this up 
		// even further (to MobsimAgent)?  kai, jun'11
		return new Person() {

			@Override
			public Map<String, Object> getCustomAttributes() {
				throw new RuntimeException();
			}

			@Override
			public Id getId() {
				return id;
			}

			@Override
			public List<? extends Plan> getPlans() {
				throw new RuntimeException();
			}

			@Override
			public void setId(Id id) {
				throw new RuntimeException();
			}

			@Override
			public boolean addPlan(Plan p) {
				throw new RuntimeException();
			}

			@Override
			public Plan getSelectedPlan() {
				return selectedPlan;
			}
			
		};
	}

	@Override
	public double getActivityEndTime() {
		// I get asked about this first thing in the morning.
		// After I have decided I am in an activity, I need to give the
		// same answer again every time I am asked (until I am told my activity
		// is over), or I will confuse the simulation. mz
		//
		// The main reason for this is that the activity end queue is time-sorted, so that not all agents need to be asked
		// if they want to depart.  If you take care of agent departure yourself, this could just return infinity.
		return ((Activity) currentPlanElement).getEndTime();
	}

	@Override
	public void endActivityAndAssumeControl(double now) {
		// The simulation tells me when the time has come to end my activity.
		// This may be later than the time I told it I wanted to end my activity, 
		// because it may already have been later then.
		// On the other hand, "really" ending the activity is still up to me, because I have 
		// to throw the event.
		// So all the simulation really does is set an alarm clock for me and tell me if
		// I've missed it. It doesn't really care about activities otherwise.
		//
		// I should just try to ignore the Simulation's views about activities and decide:
		// Do I want to leave right now? Then I'm going to depart.
		// Or do I not? Then I will tell the Simulation I am having an activity until the next timestep.
		
	}

	@Override
	public void endLegAndAssumeControl(double now) {
		// The simulation tells me I have arrived.
		// Interestingly, I have to throw the event myself. mz
		//
		// In theory, transport-related events should be thrown by the transport part of the simulation, activity-related
		// events should be thrown by the agent.  In practice, there are many transport-related modules, such as teleportation, 
		// walk, public transit, etc., and many of them are pluggable.  It is quite possible that one of them forgets the 
		// corresponding arrival event.  So it seems easier to ensure consistency in the agent than in the framework.
		// In part also because the user of the agent arrival event is probably the agent programmer, not the framework
		// programmer.  Given that this was also the structure that I found, I decided to leave it that way.  kai, jun'11

		currentLinkId = ((Leg) currentPlanElement).getRoute().getEndLinkId();
		eventsManager.processEvent(eventsManager.getFactory().createAgentArrivalEvent(now, id, currentLinkId, ((Leg) currentPlanElement).getMode()));
		currentPlanElement = null;
		teleportationBehavior = null;
		drivingBehavior = null;
	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return currentPlanElement;
	}

	@Override
	public PlanElement getNextPlanElement() {
		// I am never asked this. mz
		//
		// Then why is it in the interface? :-) kai, jun'11
		throw new RuntimeException();
	}

	@Override
	@Deprecated // try to use getCurrentPlanElement()
	public Leg getCurrentLeg() {
		// I am getting asked about this as soon as I tell the Simulation I want to depart and on several other
		// occasions. Most of the time, what they really want to know is only the mode I am choosing,
		// but sometimes they also want to know the car Route I am taking. This is strange because I am asked at every time
		// step what link I want to go to. Then I realized that I am only asked this so the Simulation knows if maybe I already
		// am where I want to go. mz
		//
		// I am not fully sure but I seem to remember that the problem is that someone (not me) put the arrival link id into the 
		// route rather than into the leg.  So you either have to ask the activity or the route if you have arrived.  Out of these
		// two, I would say that route is the less awkward.  
		//
		// There is also getDestinationLinkId(), and it is not clear if "getCurrentLeg()" is even still necessary.  kai, jun'11
		//
		// Personally, I also think that getCurrentLeg and getCurrentActivity should simply be removed.
	    
        PlanElement currentPlanElement = this.getCurrentPlanElement();

        if (!(currentPlanElement instanceof Leg)) {
            return null;
        }
        
        return (Leg) currentPlanElement;
	}

	@Override
	@Deprecated // try to use getCurrentPlanElement()
	public Activity getCurrentActivity() {
	    PlanElement currentPlanElement = this.getCurrentPlanElement();
	    
	    if (!(currentPlanElement instanceof Activity)) {
            return null;
        }
	    
        return (Activity) currentPlanElement;
	}

	@Override
	public void notifyTeleportToLink(Id linkId) {
		// I am told this when the Simulation decides to not move me to my destination over the network, but to teleport me there.
		// This is a little silly - apparently the Simulation thinks that when I'm moved over the network, I can keep track of
		// my current link myself, but when I'm "teleported", I can't do that. But I can! After all, the Simulation just asked me where I 
		// want to go. mz
		//
		// Actually no.  The teleportation arrival is much later than the teleportation departure.  Also, there is no guarantee
		// that the mobsim delivers to where it should deliver.  The corresponding notification method for network movement 
		// is "notifyMoveOverNode". kai, jun'11
	}

	@Override
	public void initialize() {
		// I am told this at the very beginning, after I'm given a vehicle.
		simulation.getAgentCounter().incLiving();
	}

	@Override
	public Plan getSelectedPlan() {
		// TODO Auto-generated method stub
		throw new RuntimeException();
	}

	@Override
	public Id getCurrentLinkId() {
		// I am asked this right at the beginning so the Simulation can park my vehicle on that link. mz
		//
		// Many pieces of the code are easier if the agent knows where she is.  In most places, this is retrieved over the
		// person, but this seems the better way (I think).  kai, jun'11
		if (firstTimeGetCurrentLinkId) {
			return currentLinkId ;
		} else {
			throw new RuntimeException();
		}
	}

	@Override
	public Id getDestinationLinkId() {
		// If I want to "arrive", I need to
		// return the link I am currently on (I need to know that),
		// and ALSO, on the next call to chooseNextLinkId, choose null.
		// I will then be told to endLegAndAssumeControl immediately afterwards.
		//
		// Also, when I am being teleported, I will be asked via this method
		// where it is I wanted to go, even though the simulation should know
		// this because it is in my Leg, which it already used to determine
		// that I need to be teleported.
		// I just have to be honest in answering that question. The simulation will
		// immediately notify me that I was teleported to the link which I answer here. mz
		//
		// The idea was/is to make the mobsim run without knowing about legs and routes.  kai, jun'11
		return ((Leg) currentPlanElement).getRoute().getEndLinkId();
	}

	@Override
	public Id getId() {
		return id;
	}

	@Override
	public Id chooseNextLinkId() {
		return nextLinkId;
	}

	@Override
	public void setVehicle(QVehicle veh) {
		// The Simulation tells me what vehicle I get to use. Don't know what I should do with that information.
		// I need to remember it so I can give it back when getVehicle is called. mz
		//
		// This is just a back pointer.  I would prefer bi-directional pointers (i.e. connectors), but they
		// don't exist in java.  As an example: Person may receive a recommendation to change lanes via his/her iPhone.
		// The person would need to be able to do give this info to the steering wheel of the vehicle.
		// It is, however, not clear why the agent needs to expose that info.  kai, jun'11

		this.veh = veh;
	}

	@Override
	public QVehicle getVehicle() {
		// The simulation asks me what vehicle I am using. This is silly. I am an agent! Nobody should use me as a data container.
		// mz
		//
		// agreed.  it is used only in two locations; maybe we can get rid of it.  
		// (I fact, both places where it is needed have to do with non-physical behavior.)  kai, jun'11
		return this.veh;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		// I think I am told this some time after I was asked about my next link. I think this means that I have entered it now. mz
		//
		// yes, exactly.  It may have happened that it was not possible to cross the intersection, and the mobsim may have made you
		// "stuckAndAbort", or a police person may have waved you into a link into which you did not want to go.  
		// (It is, however, quite strange that this does not pass on the new link as an argument.)
		// kai, jun'11
		this.nextLinkId = null; // this is set to null because it means the "next" linkId, not the "current" linkId.  kai, jun'11
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome) {
		// Do I want to enter that bus?
		return false;
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		// Do I want to get off here?
		return false;
	}

	@Override
	public double getWeight() {
		// whatever. mz
		//
		// yyyy well, would be better to set this to "1" since this is, in the end, the space that the agent uses, say in the
		// bus.  But it is one of these functions that someone added without thinking it through, and nobody has used so far. 
		// kai, jun'11
		return 0;
	}

}
