package playground.pieter.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;

import playground.pieter.pseudosimulation.mobsim.PSimFactory;
import playground.singapore.transitRouterEventsBased.TransitRouterWSImplFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTime;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTime;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;

public class SlaveControler implements IterationStartsListener, BeforeMobsimListener {
	class TimesReceiver implements Runnable {
		Logger timesLogger = Logger.getLogger(this.getClass());

		@Override
		public void run() {
			while (true) {
				boolean res = false;
				try {
					timesLogger.warn("trying to read boolean from master");
					res = reader.readBoolean();
				} catch (IOException e) {
					System.out.println("Master terminated. Exiting.");
					System.exit(0);
				}
				try {
					if (res) {
						timesLogger.warn("Receiving travel times.");
						linkTravelTimes = (SerializableLinkTravelTimes) reader.readObject();
						stopStopTimes = (StopStopTime) reader.readObject();
						waitTimes = (WaitTime) reader.readObject();
						timesLogger.warn("Checking to see if plans are ready to be sent");
						while (!readyToSendPlans)
							;
						timesLogger.warn("Sending plans...");
						writer.writeObject(plansCopyForSending);
						timesLogger.warn("Sending completed.");
					} else {
						System.out.println("Master terminated. Exiting.");
						System.exit(0);
					}
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	Controler matsimControler;
	Logger slaveLogger = Logger.getLogger(this.getClass());
	private SerializableLinkTravelTimes linkTravelTimes;
	private WaitTime waitTimes;
	private StopStopTime stopStopTimes;
	private ObjectInputStream reader;
	private ObjectOutputStream writer;
	private PSimFactory pSimFactory;
	private Map<String, PlanSerializable> plansCopyForSending;
	private boolean readyToSendPlans = false;

	public SlaveControler(String[] args) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		matsimControler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		matsimControler.setOverwriteFiles(true);
		matsimControler.setCreateGraphs(false);
		matsimControler.addControlerListener(this);
		Socket socket = new Socket(args[1], Integer.parseInt(args[2]));
		this.reader = new ObjectInputStream(socket.getInputStream());
		this.writer = new ObjectOutputStream(socket.getOutputStream());
		int myNumber = reader.readInt();
		matsimControler.getConfig().controler()
				.setOutputDirectory(matsimControler.getConfig().controler().getOutputDirectory() + "_" + myNumber);
		slaveLogger.warn("About to receive agent ids for removal from master");
		List<String> idStrings = (List<String>) reader.readObject();
		slaveLogger.warn("RECEIVED agent ids for removal from master. Starting TimesReceiver thread.");
		removeNonSimulatedAgents(idStrings);
		new Thread(new TimesReceiver()).start();
		if(args.length==3){
			slaveLogger.warn("Singapore scenario: Doing events-based transit routing.");
			matsimControler.setTransitRouterFactory(new TransitRouterWSImplFactory(matsimControler.getScenario(), waitTimes, stopStopTimes));
			matsimControler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(matsimControler.getScenario().getConfig().planCalcScore(), matsimControler.getScenario()));
		}
	}

	private void removeNonSimulatedAgents(List<String> idStrings) {
		Set<Id<Person>> noIds = new HashSet<>(matsimControler.getPopulation().getPersons().keySet());
		Set<String> noIdStrings = new HashSet<>();
		for (Id<Person> id : noIds)
			noIdStrings.add(id.toString());
		noIdStrings.removeAll(idStrings);
		slaveLogger.warn("removing ids");
		for (String idString : noIdStrings) {
			matsimControler.getPopulation().getPersons().remove(Id.create(idString, Person.class));
		}

	}

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		SlaveControler slave = new SlaveControler(args);
		slave.run();
	}

	private void run() {
		pSimFactory = new PSimFactory();
		matsimControler.setMobsimFactory(pSimFactory);
		// Collection<Plan> plans = new ArrayList<>();
		// for (Person person :
		// matsimControler.getPopulation().getPersons().values())
		// plans.add(person.getSelectedPlan());
		// // pSimFactory.setPlans(plans);
		matsimControler.run();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// the time calculators are null until the master sends them, need
		// something to keep psim occupied until then
		if (linkTravelTimes == null)
			pSimFactory.setTravelTime(matsimControler.getLinkTravelTimes());
		else
			pSimFactory.setTravelTime(linkTravelTimes);
		if (matsimControler.getConfig().scenario().isUseTransit()) {
			if (stopStopTimes == null)
				pSimFactory.setStopStopTime(new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
						matsimControler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (matsimControler.getConfig()
								.qsim().getEndTime() - matsimControler.getConfig().qsim().getStartTime())).getStopStopTimes());
			else
				pSimFactory.setStopStopTime(stopStopTimes);
			if (waitTimes == null)
				pSimFactory.setWaitTime(new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
						matsimControler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (matsimControler.getConfig()
								.qsim().getEndTime() - matsimControler.getConfig().qsim().getStartTime())).getWaitTimes());
			else
				pSimFactory.setWaitTime(waitTimes);
		}
		Collection<Plan> plans = new ArrayList<>();
		for (Person person : matsimControler.getPopulation().getPersons().values())
			plans.add(person.getSelectedPlan());
		pSimFactory.setPlans(plans);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		createPlansCopyForMaster();
	}

	public void createPlansCopyForMaster() {
		Map<String, PlanSerializable> tempPlansCopyForSending = new HashMap<>();
		for (Person person : matsimControler.getPopulation().getPersons().values())
			tempPlansCopyForSending.put(person.getId().toString(), new PlanSerializable(person.getSelectedPlan()));
		plansCopyForSending = tempPlansCopyForSending;
		readyToSendPlans = true;

	}

}
