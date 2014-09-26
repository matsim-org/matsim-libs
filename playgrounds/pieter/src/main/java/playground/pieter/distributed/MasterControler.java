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
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeStuckCalculator;

public class MasterControler implements AfterMobsimListener, ShutdownListener {
	private class Slave implements Runnable {
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
				writer.writeBoolean(true);
				writer.writeObject(linkTravelTimes);
				// writer.writeObject(stopStopTimeCalculator.getStopStopTimes());
				// writer.writeObject(waitTimeCalculator.getWaitTimes());
				Map<String, PlanSerializable> serialPlans = (Map<String, PlanSerializable>) reader
						.readObject();
				for (Entry<String, PlanSerializable> entry : serialPlans
						.entrySet()) {
					plans.put(
							entry.getKey(),
							entry.getValue().getPlan(
									matsimControler.getPopulation()));
				}
				numThreads.decrementAndGet();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			} catch (Exception e){
				e.printStackTrace();

				
				System.exit(0);
			}
		}

		public void sendIds(Collection<String> idStrings) throws IOException {
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
	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private SerializableLinkTravelTimes linkTravelTimes;
	private AtomicInteger numThreads;
	private HashMap<String, Plan> newPlans = new HashMap<>();

	public Controler getMATSimControler() {
		return matsimControler;
	}

	public MasterControler(String[] args) throws NumberFormatException,
			IOException {
		matsimControler = new Controler(ScenarioUtils.loadScenario(ConfigUtils
				.loadConfig(args[0])));
		matsimControler.setOverwriteFiles(true);
		matsimControler.setMobsimFactory(new QSimFactory());
		int size = matsimControler.getScenario().getPopulation().getPersons()
				.size();
		Set<Id<Person>> ids = matsimControler.getScenario().getPopulation()
				.getPersons().keySet();
		ServerSocket server = new ServerSocket(Integer.parseInt(args[1]));
		int numSlaves = Integer.parseInt(args[2]);
		slaves = new Slave[numSlaves];
		List<Id<Person>>[] split = CollectionUtils.split(ids, numSlaves);
		for (int i = 0; i < numSlaves; i++) {
			Socket s = server.accept();
			slaves[i] = new Slave(s);
			slaves[i].sendNumber(i);
			List<String> idStrings = new ArrayList<>();
			for (Id id : split[i])
				idStrings.add(id.toString());
			slaves[i].sendIds(idStrings);
		}
		/*
		 * waitTimeCalculator = new WaitTimeStuckCalculator(
		 * matsimControler.getPopulation(),
		 * matsimControler.getScenario().getTransitSchedule(),
		 * matsimControler.getConfig().travelTimeCalculator()
		 * .getTraveltimeBinSize(), (int)
		 * (matsimControler.getConfig().qsim().getEndTime() - matsimControler
		 * .getConfig().qsim().getStartTime()));
		 * matsimControler.getEvents().addHandler(waitTimeCalculator);
		 * stopStopTimeCalculator = new StopStopTimeCalculator(
		 * matsimControler.getScenario().getTransitSchedule(),
		 * matsimControler.getConfig().travelTimeCalculator()
		 * .getTraveltimeBinSize(), (int)
		 * (matsimControler.getConfig().qsim().getEndTime() - matsimControler
		 * .getConfig().qsim().getStartTime()));
		 * matsimControler.getEvents().addHandler(stopStopTimeCalculator);
		 */
		matsimControler.addPlanStrategyFactory("ReplacePlanFromSlave",
				new ReplacePlanFromSlaveFactory(newPlans));
		matsimControler.addControlerListener(this);
	}

	public void run() {
		matsimControler.run();
	}

	public static void main(String[] args) throws NumberFormatException,
			IOException {
		MasterControler master = new MasterControler(args);
		master.run();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		linkTravelTimes = new SerializableLinkTravelTimes(event.getControler()
				.getLinkTravelTimes(), matsimControler.getConfig()
				.travelTimeCalculator().getTraveltimeBinSize(), matsimControler
				.getConfig().qsim().getEndTime(), matsimControler.getNetwork()
				.getLinks().values());
		numThreads = new AtomicInteger(slaves.length);
		for (int i = 0; i < slaves.length; i++)
			new Thread(slaves[i]).start();
		while (numThreads.get() > 0);
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
