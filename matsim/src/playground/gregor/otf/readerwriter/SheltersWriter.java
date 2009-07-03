package playground.gregor.otf.readerwriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.data.OTFDataWriter;

import playground.gregor.otf.drawer.OTFSheltersDrawer;

public class SheltersWriter extends OTFDataWriter {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1023289245440233199L;
	private final OTFSheltersDrawer data;

	public SheltersWriter(OTFSheltersDrawer data) {
		this.data = data;
	}
	
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		
		
		ByteArrayOutputStream a = new ByteArrayOutputStream();
		
		ObjectOutputStream o = new ObjectOutputStream(a);
		o.writeObject(this.data);
		
		out.putInt(a.toByteArray().length);
		
		out.put(a.toByteArray());
		
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
