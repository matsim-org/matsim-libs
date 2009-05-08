package playground.gregor.otf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.data.OTFDataWriter;

public class InundationDataWriter extends OTFDataWriter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6693752092591508527L;
	
	private final InundationData data;

	public InundationDataWriter(InundationData data) {
		this.data = data;
	}

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		
		
		ByteArrayOutputStream a = new ByteArrayOutputStream();
		
		ObjectOutputStream o = new ObjectOutputStream(a);
		o.writeObject(this.data);
		out.put(a.toByteArray());
		
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
