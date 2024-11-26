/* *********************************************************************** *
 * project: org.matsim.*
 * RecieverWriter.java
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Writes a {@link Receivers} container in the MATSim XML format.
 *
 * @author jwjoubert
 */
public final class ReceiversWriter extends MatsimXmlWriter implements MatsimWriter{
	final private Logger log = LogManager.getLogger(ReceiversWriter.class);
	final private Receivers receivers;
	final private Counter counter = new Counter("   receiver # ");

	public ReceiversWriter(Receivers receivers) {
		super();
		this.receivers = receivers;
	}


	@Override
	public void write(String filename) {
		log.info("Writing receivers to file: " + filename);
		writeV2(filename);
	}


	public void writeV1(String filename) {
		String dtd = "https://matsim.org/files/dtd/freightReceivers_v1.dtd";
		ReceiversWriterHandler handler = new ReceiversWriterHandlerImplV1();

		try {
			sharedCodeBetweenV1andV2(filename, dtd, handler);
		} catch (IOException e){
			throw new UncheckedIOException(e);
		}
	}

	public void writeV2(String filename) {
		String dtd = "https://matsim.org/files/dtd/freightReceivers_v2.dtd";
		ReceiversWriterHandler handler = new ReceiversWriterHandlerImplV2();

		try {
			sharedCodeBetweenV1andV2(filename, dtd, handler);
		} catch (IOException e){
			throw new UncheckedIOException(e);
		}
	}

	private void sharedCodeBetweenV1andV2(String filename, String dtd, ReceiversWriterHandler handler) throws IOException {
		openFile(filename);
		writeXmlHead();
		writeDoctype("freightReceivers", dtd);

		handler.startReceivers(this.receivers, writer);

		/* Write all the product types */
		if (!receivers.getAllProductTypes().isEmpty()) {
			handler.startProducts(writer);

			for (ProductType type : receivers.getAllProductTypes()) {
				handler.startProduct(type, writer);
				handler.endProduct(writer);
			}
			handler.endProducts(writer);
			handler.writeSeparator(writer);
		}

		/* Write the receivers. */
		for (Receiver receiver : this.receivers.getReceivers().values()) {
			handler.startReceiver(receiver, writer);

			/* Write the products, if there are any. */
			if (!receiver.getProducts().isEmpty()) {
				for (ReceiverProduct product : receiver.getProducts()) {
					handler.startReceiverProduct(product, writer);
					handler.startReorderPolicy(product.getReorderPolicy(), writer);
					handler.endReorderPolicy(writer);
					handler.endReceiverProduct(writer);
				}
				handler.writeGap(writer);
			}

			/* Build receiver orders. */
			for (ReceiverPlan plan : receiver.getPlans()) {
				handler.startPlan(plan, writer);

				/* Write the time windows, if there are any. */
				if (plan.getTimeWindows().size() > 0) {
					for (TimeWindow tw : plan.getTimeWindows()) {
						handler.startTimeWindow(tw, writer);
						handler.endTimeWindow(writer);
					}
				}

				for (ReceiverOrder order : plan.getReceiverOrders()) {
					handler.startOrder(order, writer);
					for (Order item : order.getReceiverProductOrders()) {
						handler.startItem(item, writer);
						handler.endItem(writer);
					}
					handler.endOrder(writer);
				}
				handler.endPlan(writer);
			}

			handler.endReceiver(writer);
			counter.incCounter();

			handler.writeSeparator(writer);
		}
		counter.printCounter();
		handler.endReceivers(this.writer);
		this.writer.close();
	}

}

