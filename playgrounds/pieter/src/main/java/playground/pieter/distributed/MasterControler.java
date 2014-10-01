package playground.pieter.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.pseudosimulation.util.CollectionUtils;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculator;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeStuckCalculator;

public class MasterControler implements AfterMobsimListener, ShutdownListener {
	private class Slave implements Runnable {
		Logger slaveLogger = Logger.getLogger(this.getClass());
		ObjectInputStream reader;
		ObjectOutputStream writer;
		Map<String, Plan> plans = new HashMap<>();

		public Slave(Socket socket) throws IOException {
			super();
			this.writer = new ObjectOutputStream(socket.getOutputStream());
			this.reader = new ObjectInputStream(socket.getInputStream());
		}

		@Override
		public void run() {
			try {
				slaveLogger.warn("About to send travel times.");
				writer.writeBoolean(true);
				writer.writeObject(linkTravelTimes);
				writer.writeObject(stopStopTimeCalculator.getStopStopTimes());
				writer.writeObject(waitTimeCalculator.getWaitTimes());
				slaveLogger.warn("waiting to receive plans");
				Map<String, PlanSerializable> serialPlans = (Map<String, PlanSerializable>) reader.readObject();
				slaveLogger.warn("Plans received.");
				for (Entry<String, PlanSerializable> entry : serialPlans.entrySet()) {
					plans.put(entry.getKey(), entry.getValue().getPlan(matsimControler.getPopulation()));
				}
				numThreads.decrementAndGet();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();

				System.exit(0);
			}
		}

		public void sendIds(Collection<String> idStrings) throws IOException {
			slaveLogger.warn("Sending ids.");
			writer.writeObject(idStrings);
		}

		public void shutdown() throws IOException {
			// kills the slave
			writer.writeBoolean(false);
		}

		public void sendNumber(int i) throws IOException {
			writer.writeInt(i);
		}

	}

	private Controler matsimControler;
	private Slave[] slaves;
	private WaitTimeCalculatorSerializable waitTimeCalculator;
	private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
	private SerializableLinkTravelTimes linkTravelTimes;
	private AtomicInteger numThreads;
	private HashMap<String, Plan> newPlans = new HashMap<>();
	Logger masterLogger = Logger.getLogger(this.getClass());

	public Controler getMATSimControler() {
		return matsimControler;
	}

	public MasterControler(String[] args) throws NumberFormatException, IOException {
		int numSlaves = Integer.parseInt(args[2]);
		slaves = new Slave[numSlaves];
		ServerSocket server = new ServerSocket(Integer.parseInt(args[1]));
		for (int i = 0; i < numSlaves; i++) {
			Socket s = server.accept();
			masterLogger.warn("Slave accepted.");
			slaves[i] = new Slave(s);
			slaves[i].sendNumber(i);
		}
		server.close();
		matsimControler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		matsimControler.setOverwriteFiles(true);
		matsimControler.setMobsimFactory(new QSimFactory());
		Set<Id<Person>> ids = matsimControler.getScenario().getPopulation().getPersons().keySet();
		List<Id<Person>>[] split = CollectionUtils.split(ids, numSlaves);
		for (int i = 0; i < numSlaves; i++) {
			List<String> idStrings = new ArrayList<>();
			for (Id id : split[i])
				idStrings.add(id.toString());
			slaves[i].sendIds(idStrings);
		}
		if (matsimControler.getConfig().scenario().isUseTransit()) {
			waitTimeCalculator = new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(), matsimControler
					.getConfig().travelTimeCalculator().getTraveltimeBinSize(),
					(int) (matsimControler.getConfig().qsim().getEndTime() - matsimControler.getConfig().qsim().getStartTime()));
			matsimControler.getEvents().addHandler(waitTimeCalculator);
			stopStopTimeCalculator = new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
					matsimControler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (matsimControler.getConfig().qsim()
							.getEndTime() - matsimControler.getConfig().qsim().getStartTime()));
			matsimControler.getEvents().addHandler(stopStopTimeCalculator);
		}

		matsimControler.addPlanStrategyFactory("ReplacePlanFromSlave", new ReplacePlanFromSlaveFactory(newPlans));
		matsimControler.addControlerListener(this);
		masterLogger.warn("master inited");
	}

	public void run() {
		matsimControler.run();
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		MasterControler master = new MasterControler(args);
		master.run();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		linkTravelTimes = new SerializableLinkTravelTimes(event.getControler().getLinkTravelTimes(), matsimControler.getConfig()
				.travelTimeCalculator().getTraveltimeBinSize(), matsimControler.getConfig().qsim().getEndTime(), matsimControler
				.getNetwork().getLinks().values());
		numThreads = new AtomicInteger(slaves.length);
		for (int i = 0; i < slaves.length; i++)
			new Thread(slaves[i]).start();
		while (numThreads.get() > 0)
			;
		mergePlansFromSlaves();
	}

	private void mergePlansFromSlaves() {
		newPlans.clear();
		for (Slave slave : slaves) {
			newPlans.putAll(slave.plans);
		}

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		for (Slave slave : slaves) {
			try {
				slave.shutdown();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
