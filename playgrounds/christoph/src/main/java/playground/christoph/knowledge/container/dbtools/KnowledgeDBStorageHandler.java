package playground.christoph.knowledge.container.dbtools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

import playground.christoph.knowledge.container.DBStorage;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.network.util.SubNetworkTools;
import playground.christoph.router.util.KnowledgeTools;

/*
 * This Class takes care of the Data Exchange between the Persons 
 * and the Database that contains their Knowledge. 
 */
public class KnowledgeDBStorageHandler extends Thread implements ActivityStartEventHandler, SimulationBeforeSimStepListener{

	private ArrayList<PersonImpl> newPersons = new ArrayList<PersonImpl>();
	private LinkedList<PersonImpl> personsToProcess = new LinkedList<PersonImpl>();

	private PopulationImpl population;
	private KnowledgeTools knowledgeTools;
	private SubNetworkTools subNetworkTools;
	
	private boolean stopHandler = false;
	
	/*
	 * Basically a 1:1 copy of the activityEndsList of the QueueSimulation.
	 * The difference is, that this implementation uses a Time Offset. That means
	 * that we are informed that an activity will end a few Time Steps before it
	 * really ends. This allows us for example to read the known nodes of a Person
	 * from a Database before they are needed what should speed up the Simulation.
	 */
	protected final PriorityBlockingQueue<PersonDriverAgent> offsetActivityEndsList = new PriorityBlockingQueue<PersonDriverAgent>(500, new DriverAgentDepartureTimeComparator());
	protected final double timeOffset = 120.0;
	
//	private int count = 0;

	public KnowledgeDBStorageHandler(PopulationImpl population)
	{
		this.setDaemon(true);
		this.setName("KnowledgeDBStorageHandler");
		
		this.population = population;
		
		knowledgeTools = new KnowledgeTools();
		subNetworkTools = new SubNetworkTools();
	}
	
	public void stopHandler()
	{
		stopHandler = true;
		synchronized(personsToProcess)
		{
			personsToProcess.clear();
		}
	}
	
	@Override
	public void run()
	{
//		System.out.println("Running!");
		while(!stopHandler)
		{	
			/*
			 *  Don't allow adding of new Persons while we move them from
			 *  from newPersons to personsToProcess. We don't have to
			 *  lock personsToProcess because they are only accessed from
			 *  within startProcessing.
			 */
			synchronized(newPersons)
			{
				personsToProcess.addAll(newPersons);
				newPersons.clear();
			}
			
//			System.out.println("restart");
			startProcessing();
			
			// lock newPersons so that we don't miss a notify from addPerson
			try 
			{
				synchronized(newPersons)
				{
//					System.out.println("locked");
					
					// if there are no newPersons we wait until we get notified
					if (newPersons.size() == 0)
					{
//						System.out.println("waiting");
						newPersons.wait();
					}
//					System.out.println("released");
				}
			}
			catch (InterruptedException e)
			{
				Gbl.errorMsg(e);
			}
		}
	}
	
	public void addPerson(PersonImpl person)
	{
		synchronized(newPersons)
		{
			newPersons.add(person);
			newPersons.notify();
		}
	}
	
	private void startProcessing()
	{		
		while ((personsToProcess.peek() != null) && !stopHandler)
		{
			PersonImpl person = personsToProcess.poll();
			
			NodeKnowledge nodeKnowledge = knowledgeTools.getNodeKnowledge(person);
			
			if(nodeKnowledge instanceof DBStorage)
			{
				boolean leaveLinkReplanning = (Boolean)person.getCustomAttributes().get("leaveLinkReplanning");
				boolean actEndReplanning = (Boolean)person.getCustomAttributes().get("endActivityReplanning");
				
				if (leaveLinkReplanning || actEndReplanning)
				{
					/*
					 *  The NodeKnowledge Class decides, whether reading the
					 *  Knowledge from the Database is really neccessary or not.
					 */
					((DBStorage) nodeKnowledge).readFromDB();
					
//					count++;
//					if (count % 1000 == 0) System.out.println("Read " + count + " Knowledges from DB");
				}				
			}
		}
	}

	public synchronized void handleEvent(ActivityStartEvent event)
	{
		Person person = population.getPersons().get(event.getPersonId());
		NodeKnowledge nodeKnowledge = knowledgeTools.getNodeKnowledge(person);
		
		subNetworkTools.resetSubNetwork(person);
		
		if (nodeKnowledge instanceof DBStorage)
		{
			((DBStorage) nodeKnowledge).clearLocalKnowledge();
		}
	}

	
	public void reset(int iteration) {
		// TODO Auto-generated method stub	
	}
	
	public void scheduleActivityEnd(final PersonDriverAgent agent)
	{	
		offsetActivityEndsList.add(agent);
	}
	
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) 
	{
		handleOffsetActivityEnds(e.getSimulationTime());
	}
	
	private void handleOffsetActivityEnds(final double time)
	{		
		while (this.offsetActivityEndsList.peek() != null)
		{
			PersonDriverAgent agent = this.offsetActivityEndsList.peek();
			if (agent.getDepartureTime() <= time + timeOffset)
			{
				this.offsetActivityEndsList.poll();
				this.addPerson((PersonImpl) agent.getPerson());
			} 
			else
			{
				return;
			}
		} 
	}
	
	/*
	 * for the Knowledge Modules
	 */
	/*package*/ class DriverAgentDepartureTimeComparator implements Comparator<PersonDriverAgent>, Serializable {

		private static final long serialVersionUID = 1L;

		public int compare(PersonDriverAgent agent1, PersonDriverAgent agent2) {
			int cmp = Double.compare(agent1.getDepartureTime(), agent2.getDepartureTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return agent2.getPerson().getId().compareTo(agent1.getPerson().getId());
			}
			return cmp;
		}
	}
}