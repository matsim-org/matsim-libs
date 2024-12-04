/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiversWriterHandlerImpl_v1.java
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
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * First version of the freight receivers XML writer.
 *
 * @author jwjoubert
 */
 class ReceiversWriterHandlerImplV1 implements ReceiversWriterHandler {
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

	@Override
	public void startReceivers(Receivers receivers, BufferedWriter out) throws IOException {
		out.write("\n<freightReceivers");
		if(!receivers.getDescription().equalsIgnoreCase("")) {
			out.write(" desc=\"");
			out.write(receivers.getDescription());
			out.write("\"");
		}
		out.write(">\n");
		attributesWriter.writeAttributes("\t", out, receivers.getAttributes());
		out.write("\n");
	}

	@Override
	public void endReceivers(BufferedWriter out) throws IOException {
		out.write("\n</freightReceivers>\n");
	}

	@Override
	public void startReceiver(Receiver receiver, BufferedWriter out) throws IOException {
		out.write("\n\t<receiver id=\"");
		out.write(receiver.getId().toString());
		out.write("\"");
		if(receiver.getLinkId() != null) {
			out.write(" linkId=\"");
			out.write(receiver.getLinkId().toString());
			out.write("\"");
		}
		out.write(">\n");

		attributesWriter.writeAttributes("\t\t", out, receiver.getAttributes());
		if(receiver.getAttributes().size() > 0){
			this.writeGap(out);
		}
	}

	@Override
	public void endReceiver(BufferedWriter out) throws IOException {
		out.write("\t</receiver>\n");
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		out.write("\n\t<!-- =========================================================================================== -->\n");
	}

	@Override
	public void writeGap(BufferedWriter out) throws IOException {
		out.write("\n");
	}

	@Override
	public void startReceiverProduct(ReceiverProduct product, BufferedWriter out) throws IOException {
		out.write("\t\t<product id=\"");
		out.write(product.getProductType().getId().toString());
		out.write("\" onHand=\"");
		out.write(String.format(Locale.US, "%.2f", product.getStockOnHand()));
		out.write("\">\n");
	}

	@Override
	public void endReceiverProduct(BufferedWriter out) throws IOException {
		out.write("\t\t</product>\n");

	}

	@Override
	public void startReorderPolicy(ReorderPolicy policy, BufferedWriter out) throws IOException {
		out.write("\t\t\t<reorderPolicy name=\"");
		out.write(policy.getPolicyName());
		out.write("\">\n");

		final Attributes attributes = policy.getAttributes();
		attributesWriter.writeAttributes("\t\t\t\t", out, attributes);
	}

	@Override
	public void endReorderPolicy(BufferedWriter out) throws IOException {
		out.write("\t\t\t</reorderPolicy>\n");
	}

	@Override
	public void startPlan(ReceiverPlan plan, BufferedWriter out) throws IOException {
		Double score = plan.getScore();
		String selected = plan.isSelected() ? "yes" : "no";

		out.write("\t\t<plan");
		if(score != null) {
			out.write(" score=\"" + score + "\"");
		}
		out.write(" selected=\"" + selected + "\">\n");
	}

	@Override
	public void endPlan(BufferedWriter out) throws IOException {
		out.write("\t\t</plan>\n");
	}

	@Override
	public void endOrder(BufferedWriter out) throws IOException {
		out.write("\t\t\t</order>\n");
	}

	@Override
	public void startOrder(ReceiverOrder order, BufferedWriter out) throws IOException {
		out.write("\t\t\t<order carrierId=\"");
		out.write(order.getCarrierId().toString());
		out.write("\">\n");
	}

	@Override
	public void startItem(Order item, BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<item id=\"");
		out.write(item.getId().toString());
		out.write("\" productId=\"");
		out.write(item.getProduct().getProductType().getId().toString());
		/*TODO Add both weight and quantity on order */
		out.write("\" quantity=\"");
		out.write(String.valueOf(item.getOrderQuantity()));
		out.write("\" serviceTime=\"");
		out.write(Time.writeTime(item.getServiceDuration(), Time.TIMEFORMAT_HHMMSS));
		out.write("\"");
	}

	@Override
	public void endItem(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}

	@Override
	public void startProducts(BufferedWriter out) throws IOException {
		out.write("\t<productTypes>\n");
	}

	@Override
	public void endProducts(BufferedWriter out) throws IOException {
		out.write("\t</productTypes>\n");
	}

	@Override
	public void startProduct(ProductType productType, BufferedWriter out) throws IOException {
		out.write("\t\t<productType id=\"");
		out.write(productType.getId().toString());
		out.write("\" descr=\"");
		out.write(productType.getDescription());
		/* FIXME The reference should be expanded/changed to have a separate value and unit. */
		out.write("\" weight=\"");
		out.write(String.valueOf(productType.getRequiredCapacity()));
		out.write("\"");
	}

	@Override
	public void endProduct(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}


	@Override
	public void startTimeWindow(TimeWindow window, BufferedWriter out) throws IOException {
		out.write("\t\t\t<timeWindow start=\"");
		out.write(Time.writeTime(window.getStart(), Time.TIMEFORMAT_HHMMSS));
		out.write("\" end=\"");
		out.write(Time.writeTime(window.getEnd(), Time.TIMEFORMAT_HHMMSS));
		out.write("\"");
	}

	@Override
	public void endTimeWindow(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}

}
