package playground.christoph.knowledge.container.dbtools;

import java.util.LinkedList;

import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

import playground.christoph.knowledge.container.DBStorage;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;

/*
 * This Class takes care of the Data Exchange between the Persons 
 * and the Database that contains their Knowledge. 
 */
public class KnowledgeDBStorageHandler extends Thread implements BasicActivityStartEventHandler{

	LinkedList<PersonImpl> personsToProcess = new LinkedList<PersonImpl>();

	private boolean stopHandler = false;
	private boolean running = false;
	
	private PopulationImpl population;
	
	public KnowledgeDBStorageHandler(PopulationImpl population)
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
	
	public void addPerson(PersonImpl person)
	{
		personsToProcess.add(person);
		
		if (!running) startProcessing();
	}
	
	public void startProcessing()
	{
		running = true;
		
		while ((personsToProcess.peek() != null) && !stopHandler)
		{
			PersonImpl person = personsToProcess.poll();
			
			NodeKnowledge nodeKnowledge = KnowledgeTools.getNodeKnowledge(person);
			
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
				}				
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

	
	public void handleEvent(BasicActivityStartEvent event)
	{
		PersonImpl person = population.getPersons().get(event.getPersonId());
		NodeKnowledge nodeKnowledge = KnowledgeTools.getNodeKnowledge(person);
		
		if (nodeKnowledge instanceof DBStorage)
		{
			((DBStorage) nodeKnowledge).clearLocalKnowledge();
		}
	}

	
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
}
