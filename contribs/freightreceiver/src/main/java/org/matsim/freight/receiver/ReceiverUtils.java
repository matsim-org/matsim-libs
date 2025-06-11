package org.matsim.freight.receiver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class ReceiverUtils {
	private final static String REPLAN_INTERVAL = "replanInterval";
	private static final Logger LOG = LogManager.getLogger(ReceiverUtils.class);

	private ReceiverUtils() {
	} // do not instantiate

	public static final String ATTR_RECEIVER_SCORE = "score";
	public static final String ATTR_RECEIVER_TW_COST = "twCost";

	//Now using the MATSim-infrastructure to avoid problems if this element is named differently beetween (MATSim) CarrierControlerUtils and here.
	// I also replaced the usage here by the current MATSim syntax KMT'jan21
//	private static final String CARRIERS_SCENARIO_ELEMENT = "carriers";

	private static final String RECEIVERS_SCENARIO_ELEMENT = "Receivers";

	public static final String FILENAME_RECEIVER_SCORES = "/receiver_scores";

	public static Receivers createReceivers() {
		return new Receivers();
	}

	/*
	 * Create a new instance of a receiver.
	 */
	public static Receiver newInstance(Id<Receiver> id) {
		// this pattern allows to make the implementation package-protected. kai, sep'18

		return new ReceiverImpl(id);
	}

	public static void setReceivers(final Receivers receivers, final Scenario sc) {
		sc.addScenarioElement(RECEIVERS_SCENARIO_ELEMENT, receivers);
	}

	public static Receivers getReceivers(final Scenario sc) {
		Receivers receivers = (Receivers) sc.getScenarioElement(RECEIVERS_SCENARIO_ELEMENT);
		if (receivers == null) {
			LOG.error("No receivers were set. Returning new, empty receivers, AND memorizing them.");
			receivers = new Receivers();
			setReceivers(receivers, sc);
		}
		return receivers;
	}

	public static ProductType getProductType(Receivers receivers, Id<ProductType> typeId) {
		return receivers.getProductType(typeId);
	}

	public static ProductType createAndGetProductType(Receivers receivers, Id<ProductType> typeId, Id<Link> originLinkId) {
		return receivers.createAndAddProductType(typeId, originLinkId);
	}

	public static ReceiverConfigGroup getConfigGroup(Config config) {
		return ConfigUtils.addOrGetModule(config, ReceiverConfigGroup.class);
	}

	/**
	 * A cost allocation model where the {@link Carrier}(s) charge a fixed amount
	 * to the receivers. One cannot expect to much behavioral response in this
	 * scenario.
	 *
	 * @param cost the amount charged by the {@link Carrier}. It is assumed that
	 *             this is a <b><i>cost</i></b>, so a positive value will have a
	 *             negative utility on the receiver.
	 */
	public static ReceiverCostAllocationFixed createFixedReceiverCostAllocation(double cost) {
		return new ReceiverCostAllocationFixed(cost);
	}

	/**
	 * A cost allocation model where each {@link Carrier} charges an equal amount
	 * to each of the receivers it services, irrespective of the number, size or
	 * value of the {@link Receiver}'s order.
	 */
	public static ReceiverCostAllocationEqualProportion createEqualProportionCostAllocation(){
		return new ReceiverCostAllocationEqualProportion();
	}

	public static ReorderPolicy createSSReorderPolicy(double s, double S) {
		return new SSReorderPolicy(s, S);
	}

}
