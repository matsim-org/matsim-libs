package org.matsim.freight.receiver.collaboration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.receiver.*;

public class CollaborationUtils{
	private CollaborationUtils(){} // do not instantiate
	private static final Logger LOG = LogManager.getLogger(CollaborationUtils.class);

	public static final String ATTR_COLLABORATION_STATUS = "collaborationStatus" ;
	public static final String ATTR_GRANDCOALITION_MEMBER = "grandCoalitionMember" ;
	private static final String COALITION_SCENARIO_ELEMENT = "Coalition" ;


	public static MutableCoalition createCoalition(){
		return new MutableCoalition();
	}

	public static void setCoalitionFromReceiverAttributes( Scenario sc ){
		for ( Receiver receiver : ReceiverUtils.getReceivers( sc ).getReceivers().values()){
			if (receiver.getAttributes().getAttribute( ATTR_COLLABORATION_STATUS ) != null){
				if ((boolean) receiver.getAttributes().getAttribute(ATTR_COLLABORATION_STATUS)){
					if (!getCoalition( sc ).getReceiverCoalitionMembers().contains(receiver)){
						getCoalition( sc ).addReceiverCoalitionMember(receiver);
					}
				} else {
					if ( getCoalition( sc ).getReceiverCoalitionMembers().contains(receiver)){
						getCoalition( sc ).removeReceiverCoalitionMember(receiver);
					}
				}
			}
		}
	}

	public static void createCoalitionWithCarriersAndAddCollaboratingReceivers(Scenario sc ){
		/* Add carrier and receivers to coalition */
		Coalition coalition = CollaborationUtils.createCoalition();

		for (Carrier carrier : CarriersUtils.getCarriers(sc).getCarriers().values()){
			if (!coalition.getCarrierCoalitionMembers().contains(carrier)){
				coalition.addCarrierCoalitionMember(carrier);
			}
		}

		for ( Receiver receiver : ReceiverUtils.getReceivers( sc ).getReceivers().values()){
			if ( (boolean) receiver.getAttributes().getAttribute( ATTR_COLLABORATION_STATUS ) ){
				if (!coalition.getReceiverCoalitionMembers().contains(receiver)){
					coalition.addReceiverCoalitionMember(receiver);
				}
			} else {
				if (coalition.getReceiverCoalitionMembers().contains(receiver)){
					coalition.removeReceiverCoalitionMember(receiver);
				}
			}
		}

		setCoalition( coalition, sc );
	}

	/**
	 * Ensures that each {@link Receiver}'s {@link ReceiverOrder}s are linked
	 * to a {@link Carrier}, and does not simply have a pointer to the
	 * {@link Carrier}'s {@link Id}.
	 */
	public static void linkReceiverOrdersToCarriers(Receivers receivers, Carriers carriers) {
		for (Receiver receiver : receivers.getReceivers().values()) {
			for (ReceiverPlan plan : receiver.getPlans()) {
				for (ReceiverOrder receiverOrder : plan.getReceiverOrders()) {
					/* Check that the carrier actually exists. */
					if (!carriers.getCarriers().containsKey(receiverOrder.getCarrierId())) {
						throw new RuntimeException("Cannot find carrier \""
								+ receiverOrder.getCarrierId().toString() + "\" for receiver \""
								+ receiver.getId().toString() + "\"'s order. ");
					}

					receiverOrder.setCarrier(carriers.getCarriers().get(receiverOrder.getCarrierId()));
				}
			}
		}
	}

	public static void setCoalition( final Coalition coalition, final Scenario sc ) {
		sc.addScenarioElement( COALITION_SCENARIO_ELEMENT, coalition );
	}

	public static Coalition getCoalition( final Scenario sc ) {
		return (Coalition) sc.getScenarioElement( COALITION_SCENARIO_ELEMENT );
	}


	public static void setCoalitionFromReceiverAttributes( Scenario scenario, Coalition coalition ){
		for ( Receiver receiver : ReceiverUtils.getReceivers( scenario ).getReceivers().values()){
			if(receiver.getAttributes().getAttribute( ATTR_COLLABORATION_STATUS )!=null){
				if ( (boolean) receiver.getAttributes().getAttribute( ATTR_COLLABORATION_STATUS ) ){
					if (!coalition.getReceiverCoalitionMembers().contains(receiver)){
						coalition.addReceiverCoalitionMember(receiver);
					}
				}
			}
		}
		LOG.info("Current number of receiver coalition members: " + coalition.getReceiverCoalitionMembers().size());
		LOG.info("Current number of carrier coalition members: " + coalition.getCarrierCoalitionMembers().size());
		LOG.info("Total number of receiver agents: " + ReceiverUtils.getReceivers(scenario).getReceivers().size());
	}

}
