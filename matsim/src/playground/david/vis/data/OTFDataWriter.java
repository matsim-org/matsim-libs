package playground.david.vis.data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public abstract class OTFDataWriter<SrcData> implements Serializable {
	protected transient SrcData src;
	
	abstract public void writeConstData(DataOutputStream out) throws IOException;
	abstract public void writeDynData(DataOutputStream out) throws IOException;
	
	public void setSrc(SrcData src) {
		this.src = src;
	}

}
