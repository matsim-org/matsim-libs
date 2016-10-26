package tutorial.programming.ownMobsimAgent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.Facility;

class MyMobsimAgent implements MobsimAgent {

    private final Scenario scenario;

    private enum MyState {Activity, Leg}

    private MyState myState = MyState.Activity;
    private final Id<Person> id;
    private Id<Link> currentLinkId;
    private final MobsimTimer simTimer;

    MyMobsimAgent(Scenario scenario, MobsimTimer simTimer) {
        this.id = Id.createPersonId("MyMobsimAgent");
        this.scenario = scenario;
        this.simTimer = simTimer;
        Id<Link> linkId = getRandomLinkId();
        this.currentLinkId = linkId;
    }

    private Id<Link> getRandomLinkId() {
        // get a random link:
        Link[] links = NetworkUtils.getSortedLinks(scenario.getNetwork());
        int random = MatsimRandom.getLocalInstance().nextInt(links.length);
        return links[random].getId();
    }

    @Override
    public Id<Link> getCurrentLinkId() {
        return currentLinkId;
    }

    @Override
    public Id<Link> getDestinationLinkId() {
        return getRandomLinkId();
    }

    @Override
    public Id<Person> getId() {
        return this.id;
    }

    @Override
    public State getState() {
        switch (myState) {
            case Activity:
                return State.ACTIVITY;
            case Leg:
                return State.LEG;
        }
        throw new IllegalStateException();
    }

    @Override
    public double getActivityEndTime() {
        if (simTimer.getTimeOfDay() > 18.0 * 60.0 * 60.0) {
            // Sandmaennchen. Ab in die Falle.
            return Double.POSITIVE_INFINITY;
        } else {
            int currentTenMinuteInterval = (int) (simTimer.getTimeOfDay() / (24.0 * 6.0));
            return (currentTenMinuteInterval + 1) * 24.0 * 6.0;
        }
    }

    @Override
    public void endActivityAndComputeNextState(double now) {
        myState = MyState.Leg;
    }

    @Override
    public void endLegAndComputeNextState(double now) {
        myState = MyState.Activity;
    }

    @Override
    public void setStateToAbort(double now) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Double getExpectedTravelTime() {
        return 5 * 60.0; // all my travels take 5 minutes
    }

    @Override
    public Double getExpectedTravelDistance() {
        return 1000.0; // all my travels are 1km
    }

    @Override
    public String getMode() {
        return "teleportation";
    }

    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
        this.currentLinkId = linkId;
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
}

}
