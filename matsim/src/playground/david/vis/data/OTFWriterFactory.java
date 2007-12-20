package playground.david.vis.data;

public interface OTFWriterFactory<SrcClass> {

	public OTFDataWriter<SrcClass> getWriter();
}
