package playground.gregor.snapshots.writers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;


public class TableSnapshotWriter {

	private final Writer writer;
	
	public TableSnapshotWriter(String filename) {
		try {
			this.writer = IOUtils.getBufferedWriter(filename,true);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			this.writer.append("time\tagent_id\teasting\tnorthing\tazimuth\tspeed\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addVehicle(double time, AgentSnapshotInfo pos) throws IOException {
		StringBuffer buff = new StringBuffer();
		buff.append(time);
		buff.append("\t");
		buff.append(pos.getId());
		buff.append("\t");
		buff.append(pos.getEasting());
		buff.append("\t");
		buff.append(pos.getNorthing());
		buff.append("\t");
		buff.append(pos.getAzimuth());
		buff.append("\t");
		buff.append(pos.getColorValueBetweenZeroAndOne());
		buff.append("\n");
		this.writer.append(buff.toString());
		
	}

	public void finish() {
		try {
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	

	
	
	
}
