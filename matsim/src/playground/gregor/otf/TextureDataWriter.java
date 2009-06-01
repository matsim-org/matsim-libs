package playground.gregor.otf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.data.OTFDataWriter;


public class TextureDataWriter extends OTFDataWriter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8078062058033632057L;
	private SimpleBackgroundTextureDrawer sbg;

	public TextureDataWriter(SimpleBackgroundTextureDrawer sbg) {
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
