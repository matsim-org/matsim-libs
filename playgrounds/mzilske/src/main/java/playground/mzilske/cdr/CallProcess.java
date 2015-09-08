package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableScheduler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CallProcess implements ActivityStartEventHandler, ActivityEndEventHandler, Steppable {

    private final Sightings sightings;
    private final MessageQueue mq;

    public class CallingAgent {

		public int nCalls;

        public double rate;

        Person person;

        double getNextCallTime() {
            return lastTime + (- Math.log(Math.random()) / (rate / (24*60*60)));
        }
    }

    class NextCallMessage extends Message {

        CallingAgent agent;

        @Override
        public void processEvent() {

        }

        @Override
        public void handleMessage() {
            call(lastTime, agent.person.getId());
            NextCallMessage nextCallMessage = new NextCallMessage();
            nextCallMessage.agent = agent;
            nextCallMessage.setMessageArrivalTime(agent.getNextCallTime());
            mq.putMessage(nextCallMessage);
        }

    }

	private Population population;

	private Map<Id, CallingAgent> agents = new HashMap<>();

	final private ZoneTracker zoneTracker;

	private double lastTime = 0.0;

	private CallBehavior callBehavior;

    private SteppableScheduler scheduler;

	@Inject
    CallProcess(Scenario scenario, Sightings sightings, final ZoneTracker zoneTracker, CallBehavior callBehavior) {
		this.sightings = sightings;
        this.population = scenario.getPopulation();
		this.zoneTracker = zoneTracker;
		this.callBehavior = callBehavior;
        mq = new MessageQueue();
		for (Person p : population.getPersons().values()) {
            CallingAgent agent = new CallingAgent();
            agent.person = p;
            Object phonerate = p.getCustomAttributes().get("phonerate");
            agents.put(p.getId(), agent);
            if (phonerate != null) {
                agent.rate = (double) phonerate;
                NextCallMessage m = new NextCallMessage();
                m.agent = agent;
                m.setMessageArrivalTime(agent.getNextCallTime());
                mq.putMessage(m);
            }
		}
        this.scheduler = new SteppableScheduler(mq);
	}

	public void finish() {
		for (Person p : population.getPersons().values()) {
            if (callBehavior.makeACallAtMorningAndNight(p.getId())) {
                handleNight(p);
            }
		}
	}

	public void doSimStep(double time) {
        lastTime = time;
        if (time == 0.0) {
                for (Person p : population.getPersons().values()) {
                    if (callBehavior.makeACallAtMorningAndNight(p.getId())) {
				    handleMorning(p);
                }
			}
		}
//		for (Person p : population.getPersons().values()) {
//			CallingAgent agent = agents.get(p.getId());
//			if (callBehavior.makeACall(p.getId(), time)) { // Let's make a call!
//				agent.nCalls++;
//				call(time, p.getId());
//			}
//		}
        scheduler.doSimStep(time);
	}

	private void handleNight(Person p) {
		call(lastTime, p.getId());
	}

	private void handleMorning(Person p) {
		call(0.0, p.getId());
	}

	private void call(double time, Id<Person> personId) {
		String cellId = zoneTracker.getZoneForPerson(personId).toString();
		Sighting sighting = new Sighting(personId, (long) time, cellId);
        List<Sighting> sightingsPerPerson = sightings.getSightingsPerPerson().get(personId);
        if (sightingsPerPerson == null) {
            sightingsPerPerson = new ArrayList<>();
            sightings.getSightingsPerPerson().put(personId, sightingsPerPerson);
        }
        sightingsPerPerson.add(sighting);
        CallingAgent agent = agents.get(personId);
        agent.nCalls++;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (callBehavior.makeACall(event)) {
			call(event.getTime(), event.getPersonId());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (callBehavior.makeACall(event)) {
			call(event.getTime(), event.getPersonId());
		}
	}

}
