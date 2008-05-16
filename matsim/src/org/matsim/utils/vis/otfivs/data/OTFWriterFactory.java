package org.matsim.utils.vis.otfivs.data;

public interface OTFWriterFactory<SrcClass> {

	public OTFDataWriter<SrcClass> getWriter();
}
