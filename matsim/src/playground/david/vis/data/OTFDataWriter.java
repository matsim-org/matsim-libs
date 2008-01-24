package playground.david.vis.data;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

public abstract class OTFDataWriter<SrcData> implements Serializable {
	protected transient SrcData src;
	
	abstract public void writeConstData(ByteBuffer out) throws IOException;
	abstract public void writeDynData(ByteBuffer out) throws IOException;
	
	public void setSrc(SrcData src) {
		this.src = src;
	}

}
