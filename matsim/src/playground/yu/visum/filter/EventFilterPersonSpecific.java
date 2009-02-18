/**
 *
 */
package playground.yu.visum.filter;

import java.util.HashSet;
import java.util.Set;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.BasicEvent;
import org.matsim.events.PersonEvent;
import org.matsim.interfaces.basic.v01.Id;

/**
 * A EventFilterPersonSpecific lets the events, whose agentId belong to the
 * set PERSONIDS, pass. In order to save the resource of computer, i suggest
 * that the EventFilterPersonSpecific should be the first PersonFilterA after the
 * PersonFilterAlgorithm.
 *
 * @author ychen
 */
public class EventFilterPersonSpecific extends EventFilterA {
	/*-----------------------MEMBER VARIABLE-----------------*/
	/**
	 * Every EventFilterPersonSpecific must have a Set of Person-IDs
	 */
	private Set<Id> personIds = new HashSet<Id>();

	/*----------------------CONSTRUCTOR----------------------*/
	/**
	 * @param personIDs -
	 *            A set of Person- IDs, which is any Integer object
	 */
	public EventFilterPersonSpecific(Set<Id> personIDs) {
		System.out.println("importing " + personIDs.size() + " Person- IDs.");
		setPersonIDs(personIDs);
	}

	/*------------------------SETTER---------------------------*/
	/**
	 * Sets a Set of Person- IDs in this class
	 *
	 * @param personIDs -
	 *            a Set of Person-IDs
	 */
	public void setPersonIDs(Set<Id> personIDs) {
		this.personIds = personIDs;
	}

	/*-------------------------OVERRIDING METHOD----------------------*/
	/**
	 * Returns true if this set contains PERSONIDS contains an
	 * Integer-object,that represents the specified int value of the agent-ID of
	 * the event
	 *
	 * @param event -
	 *            an event, whose presence in this set is to be tested.
	 * @return true if this Set contains the Integer object, which corresponds
	 *         the agent- ID.
	 */
	@Override
	public boolean judge(BasicEvent event) {
		if (event instanceof PersonEvent) {
			return this.personIds.contains(new IdImpl(((PersonEvent) event).agentId));
		}
		return false;
	}
}
