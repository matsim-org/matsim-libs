package playground.david.vis.data;

import java.io.Serializable;

import playground.david.vis.handler.OTFDefaultLinkHandler;
import playground.david.vis.handler.OTFDefaultNodeHandler;

public class OTFDefaultNetWriterFactoryImpl implements Serializable, OTFNetWriterFactory {

	private  OTFWriterFactory agentWriterFac = null;
	private  OTFWriterFactory nodeWriterFac = new OTFDefaultNodeHandler.Writer();
	private  OTFWriterFactory linkWriterFac = new OTFDefaultLinkHandler.Writer();
	
	public OTFDataWriter getAgentWriter() {
		if(agentWriterFac != null) return agentWriterFac.getWriter();
		return null;
	}

	public OTFDataWriter getLinkWriter() {
		return linkWriterFac.getWriter();
	}

	public OTFDataWriter getNodeWriter() {
		return nodeWriterFac.getWriter();
	}

	public void setAgentWriterFac(OTFWriterFactory agentWriterFac) {
		this.agentWriterFac = agentWriterFac;
	}

	public void setNodeWriterFac(OTFWriterFactory nodeWriterFac) {
		this.nodeWriterFac = nodeWriterFac;
	}

	public void setLinkWriterFac(OTFWriterFactory linkWriterFac) {
		this.linkWriterFac = linkWriterFac;
	}

}
