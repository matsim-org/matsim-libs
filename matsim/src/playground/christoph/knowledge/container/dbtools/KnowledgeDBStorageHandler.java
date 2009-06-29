package playground.christoph.knowledge.container.dbtools;

import java.util.LinkedList;

import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;

import playground.christoph.knowledge.container.DBStorage;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;

/*
 * This Class takes care of the Data Exchange between the Persons 
 * and the Database that contains their Knowledge. 
 */
public class KnowledgeDBStorageHandler extends Thread implements BasicActivityStartEventHandler{

	LinkedList<Person> personsToProcess = new LinkedList<Person>();

	private boolean stopHandler = false;
	private boolean running = false;
	
	private Population population;
	
	public KnowledgeDBStorageHandler(Population population)
	{
		this.population = population;
	}
	
	
	@Override
	public void run()
	{
		while(!stopHandler)
		{			
			startProcessing();
			
			/*
			 *  All Elements from the LinkedList where processed.
			 *  Wait for some time and then look again for new Elements.
			 */
			try { Thread.sleep(50); }
			catch (Exception e) {}
		}
	}
	
	public void addPerson(Person person)
	{
		personsToProcess.add(person);
		
		if (!running) startProcessing();
	}
	
	public void startProcessing()
	{
		running = true;
		
		while (personsToProcess.peek() != null && !stopHandler)
		{
			Person person = personsToProcess.poll();
			
			NodeKnowledge nodeKnowledge = KnowledgeTools.getNodeKnowledge(person);
			
			if(nodeKnowledge instanceof DBStorage)
			{
				/*
				 *  The NodeKnowledge Class decides, whether reading the
				 *  Knowledge from the Database is really neccessary or not.
				 */
				((DBStorage) nodeKnowledge).readFromDB();
			}
		}
		running = false;
	}
	
	public void stopProcessing()
	{
		stopHandler = true;
		
		while (running)
		{
			try { Thread.sleep(50); }
			catch (Exception e) {}
		}
		
		stopHandler = false;
	}

	@Override
	public void handleEvent(BasicActivityStartEvent event)
	{
		Person person = population.getPersons().get(event.getPersonId());
		NodeKnowledge nodeKnowledge = KnowledgeTools.getNodeKnowledge(person);
		
		if (nodeKnowledge instanceof DBStorage)
		{
			((DBStorage) nodeKnowledge).clearLocalKnowledge();
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
}
