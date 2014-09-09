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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.pseudosimulation.mobsim.PSimFactory;

public class SlaveControler implements IterationStartsListener {
	class TimesReceiver implements Runnable {

		@Override
		public void run() {
			while(true){
				boolean res = false;
				try {
					res = reader.readBoolean();
				}
				catch (IOException e) {
					System.out.println("Master terminated. Exiting.");
					System.exit(0);
				}
				try {
					if(res) {
						linkTravelTimes = (SerializableLinkTravelTimes) reader.readObject();
						Map<String,PlanSerializable> plans  =new HashMap<>();
						for(Person person:matsimControler.getPopulation().getPersons().values())
							plans.put(person.getId().toString(), new PlanSerializable(person.getSelectedPlan()));
						writer.writeObject(plans);
						pSimFactory.setTimes(linkTravelTimes);
					}
					else {
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
	private SerializableLinkTravelTimes linkTravelTimes;
	private ObjectInputStream reader;
	private ObjectOutputStream writer;
	private PSimFactory pSimFactory;

	public SlaveControler(String[] args) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		matsimControler = new Controler(ScenarioUtils.loadScenario(ConfigUtils
				.loadConfig(args[0])));
		matsimControler.setOverwriteFiles(true);
		matsimControler.addControlerListener(this);
		Socket socket = new Socket(args[1], Integer.parseInt(args[2]));
		this.reader = new ObjectInputStream(socket.getInputStream());
		this.writer = new ObjectOutputStream(socket.getOutputStream());
		removeNonSimulatedAgents((List<String>) reader.readObject());
		new Thread(new TimesReceiver()).start();
	}

	private void removeNonSimulatedAgents(List<String> idStrings) {
		Set<Id<Person>> noIds = new HashSet<>(matsimControler.getPopulation().getPersons().keySet());
		Set<String> noIdStrings = new HashSet<>();
		for(Id<Person> id:noIds)
			noIdStrings.add(id.toString());
		noIdStrings.removeAll(idStrings);
		
		for(String idString:noIdStrings){
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
		Collection<Plan> plans  =new ArrayList<>();
		for(Person person:matsimControler.getPopulation().getPersons().values())
			plans.add(person.getSelectedPlan());
		pSimFactory.setPlans(plans);
		matsimControler.run();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if(event.getIteration()==0)
			pSimFactory.setTimes(matsimControler.getLinkTravelTimes());
		Collection<Plan> plans  =new ArrayList<>();
		for(Person person:matsimControler.getPopulation().getPersons().values())
			plans.add(person.getSelectedPlan());
		pSimFactory.setPlans(plans);
	}
}
