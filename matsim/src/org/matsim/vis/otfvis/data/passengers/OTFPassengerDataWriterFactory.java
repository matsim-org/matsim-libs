package org.matsim.vis.otfvis.data.passengers;

import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFWriterFactory;

public class OTFPassengerDataWriterFactory implements OTFWriterFactory<QueueLink> {

	public OTFDataWriter<QueueLink> getWriter() {
		return new OTFPassengerDataWriter();
	}

}
