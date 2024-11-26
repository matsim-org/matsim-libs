/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiversWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.freight.receiver;

import org.matsim.freight.carriers.TimeWindow;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 *
 * @author jwjoubert
 */
 interface ReceiversWriterHandler {

	/* <freightReceivers> ... </freightReceivers> */
	void startReceivers(final Receivers receivers, final BufferedWriter out) throws IOException;
	void endReceivers(final BufferedWriter out) throws IOException;

	/* <products> ... </products> */
	void startProducts(final BufferedWriter out) throws IOException;
	void endProducts(final BufferedWriter out) throws IOException;

	/* <product> ... </product> */
	void startProduct(final ProductType product, final BufferedWriter out) throws IOException;
	void endProduct(final BufferedWriter out) throws IOException;

	/* <receiver> ... </receiver> */
	void startReceiver(final Receiver receiver, final BufferedWriter out) throws IOException;
	void endReceiver(final BufferedWriter out) throws IOException;

	/* <timeWindow> ... </timeWindow> */
	void startTimeWindow(final TimeWindow window, final BufferedWriter out) throws IOException;
	void endTimeWindow(final BufferedWriter out) throws IOException;

	/* <product> ... </product> */
	void startReceiverProduct(final ReceiverProduct product, final BufferedWriter out) throws IOException;
	void endReceiverProduct(final BufferedWriter out) throws IOException;

	/* <reorderPolicy> ... </reorderPolicy> */
	void startReorderPolicy(final ReorderPolicy policy, final BufferedWriter out) throws IOException;
	void endReorderPolicy(final BufferedWriter out) throws IOException;

	/* <plan> ... </plan> */
	void startPlan(final ReceiverPlan plan, final BufferedWriter out) throws IOException;
	void endPlan(final BufferedWriter out) throws IOException;

	/* <order> ... </order> */
	void startOrder(final ReceiverOrder order, final BufferedWriter out) throws IOException;
	void endOrder(final BufferedWriter out) throws IOException;

	/* <item> ... </item> */
	void startItem(final Order item, final BufferedWriter out) throws IOException;
	void endItem(final BufferedWriter out) throws IOException;

	/*TODO <route ... > */

	void writeSeparator(final BufferedWriter out) throws IOException;
	void writeGap(final BufferedWriter out) throws IOException;

}
