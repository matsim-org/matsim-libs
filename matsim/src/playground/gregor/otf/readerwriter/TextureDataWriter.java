package playground.gregor.otf.readerwriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.data.OTFDataWriter;

import playground.gregor.otf.drawer.OTFBackgroundTexturesDrawer;


public class TextureDataWriter extends OTFDataWriter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8078062058033632057L;
	private OTFBackgroundTexturesDrawer sbg;

	public TextureDataWriter(OTFBackgroundTexturesDrawer sbg) {
		this.sbg = sbg;
	}

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		ByteArrayOutputStream a = new ByteArrayOutputStream();

		ObjectOutputStream o = new ObjectOutputStream(a);
		o.writeObject(this.sbg);

		out.putInt(a.toByteArray().length);
		out.put(a.toByteArray());

	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// TODO Auto-generated method stub

	}

}
