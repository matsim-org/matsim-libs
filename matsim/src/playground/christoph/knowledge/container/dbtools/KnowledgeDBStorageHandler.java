package playground.christoph.knowledge.container.dbtools;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.core.gbl.Gbl;
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
public class KnowledgeDBStorageHandler extends Thread implements BasicActivityStartEventHandler{

	private ArrayList<PersonImpl> newPersons = new ArrayList<PersonImpl>();
	private LinkedList<PersonImpl> personsToProcess = new LinkedList<PersonImpl>();

	private PopulationImpl population;
	private KnowledgeTools knowledgeTools;
	private SubNetworkTools subNetworkTools;
	
	private boolean stopHandler = false;
		
//	private int count = 0;

	public KnowledgeDBStorageHandler(PopulationImpl population)
	{
		this.setDaemon(true);
		this.setName("KnowledgeDBStorageHandler");
		
		this.population = population;
		
		knowledgeTools = new KnowledgeTools();
		subNetworkTools = new SubNetworkTools();
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
	
	public void startProcessing()
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

	public synchronized void handleEvent(BasicActivityStartEvent event)
	{
		PersonImpl person = population.getPersons().get(event.getPersonId());
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
	
}