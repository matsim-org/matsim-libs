package playground.gregor.sim2d.otfdebug.readerwriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;

public class ForceArrowWriter  extends OTFDataWriter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4080248168232578370L;

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		
		List<double []> data = null; 
		if (this.src instanceof ArrayList) {
			if (((ArrayList)this.src).get(0) instanceof double [] ) {
				data = (ArrayList<double []>)this.src;
			}
		}
		
		if (data == null) {
			out.putInt(0);
			return;
		}
		out.putInt(data.size()); 
		for (double [] force : data) {
			out.putFloat((float) (force[0]- OTFServerQuad2.offsetEast));
			out.putFloat((float) (force[1] - OTFServerQuad2.offsetNorth));
			out.putFloat((float) force[2]);
			out.putFloat((float) force[3]);
			out.putFloat((float) force[4]);
		}
		
	}

}
