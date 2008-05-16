package org.matsim.utils.vis.otfivs.data;


public interface OTFNetWriterFactory {
	OTFDataWriter getLinkWriter();
	OTFDataWriter getNodeWriter();
	OTFDataWriter getAgentWriter();

}
