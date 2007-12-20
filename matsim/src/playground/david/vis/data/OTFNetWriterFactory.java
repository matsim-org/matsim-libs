package playground.david.vis.data;


public interface OTFNetWriterFactory {
	OTFDataWriter getLinkWriter();
	OTFDataWriter getNodeWriter();
	OTFDataWriter getAgentWriter();

}
