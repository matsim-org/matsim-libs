package playground.mzilske.withinday;

import java.util.ArrayDeque;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

public class MzPlanAgentImpl implements RealAgent {

	public static class DriveThisRoute implements DrivingBehavior {

		private Queue<Id> route;

		public DriveThisRoute(LinkNetworkRoute route) {
			this.route = new ArrayDeque<Id>(route.getLinkIds());
			this.route.add(route.getEndLinkId());
		}

		@Override
		public void doSimStep(DrivingWorld drivingWorld) {
			if (drivingWorld.requiresAction()) {
				if (route.isEmpty()) {
					drivingWorld.park();
				} else {
					drivingWorld.nextTurn(route.poll());
				}
			}
		}

	}

	public static class TeleportTo implements TeleportationBehavior {

		private double travelTime;

		private Id endLinkId;

		private String mode;

		public TeleportTo(Id endLinkId, String mode, double travelTime) {
			this.endLinkId = endLinkId;
			this.travelTime = travelTime;
			this.mode = mode;
		}

		@Override
		public void doSimStep(TeleportationWorld world) {

		}

		@Override
		public double getTravelTime() {
			return travelTime;
		}

		@Override
		public Id getDestinationLinkId() {
			return endLinkId;
		}

		@Override
		public String getMode() {
			return mode;
		}

	}

	public static class DoUntil implements ActivityBehavior {

		private String type;

		private double endTime;

		public DoUntil(String type, double endTime) {
			this.type = type;
			this.endTime = endTime;
		}

		@Override
		public void doSimStep(ActivityWorld world) {
			if (world.getTime() >= endTime) {
				world.stopActivity();
			}
		}

		@Override
		public String getActivityType() {
			return type;
		}


	}

	Plan plan;

	Queue<PlanElement> planElements;

	public MzPlanAgentImpl(Plan selectedPlan) {
		this.plan = selectedPlan;
		this.planElements = new ArrayDeque<PlanElement>(plan.getPlanElements());
	}

	@Override
	public void doSimStep(World world) {
		PlanElement nextThingToDo = planElements.poll();
		if (nextThingToDo instanceof Activity) {
			Activity nextActivity = (Activity) nextThingToDo;
			assertIAmAtActivityLocation(world, nextActivity);
			world.getActivityPlane().startDoing(new DoUntil(nextActivity.getType(), nextActivity.getEndTime()));
		} else if (nextThingToDo instanceof Leg) {
			Leg nextLeg = (Leg) nextThingToDo;
			if (nextLeg.getMode().equals(TransportMode.car)) {
				LinkNetworkRoute route = (LinkNetworkRoute) nextLeg.getRoute();
				world.getRoadNetworkPlane().startDriving(new DriveThisRoute(route));
			} else {
				world.getTeleportationPlane().startTeleporting(new TeleportTo(nextLeg.getRoute().getEndLinkId(), nextLeg.getMode(), nextLeg.getTravelTime()));
			}
		} else if (nextThingToDo == null) {
			world.done();
		}
	}

	private void assertIAmAtActivityLocation(World world, Activity nextActivity) {
		Id currentLinkId = world.getLocation();
		if (!currentLinkId.equals(nextActivity.getLinkId())) {
			throw new RuntimeException("I am not at my required link (" + nextActivity.getLinkId() + "), but at "+ currentLinkId);
		}
	}

}
