package playground.gregor.multidestpeds.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;

public class ASCIITableWriter implements XYVxVyEventsHandler {


	private final String outputFile;

	public boolean fileInitialized = false;

	private BufferedWriter writer;

	private final Set<Id> handledIds = new HashSet<Id>();


	public ASCIITableWriter(String outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		if (!this.fileInitialized) {
			init();
		}

		if (this.handledIds.contains(event.getPersonId())) {
			return;
		}


		try {
			String time = String.format("%12.6f", event.getTime());
			String x = String.format("%12.6f", event.getX());
			String y = String.format("%12.6f", event.getY());
			String vx = String.format("%12.6f", event.getVX());
			String vy = String.format("%12.6f", event.getVY());
			this.writer.append(time + "\t" + event.getPersonId() + "\t" + x + "\t" + y + "\t" + vx + "\t" + vy + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-3);
		}
		this.handledIds.add(event.getPersonId());

	}

	private void init() {
		try {
			this.writer = new BufferedWriter(new FileWriter(new File(this.outputFile)));
			this.writer.append("# time_stamp\t id\t x\t y\t vx\t vy\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		this.fileInitialized = true;
	}

	public void finish() {
		try {
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}

	public static void main(String [] args) {
		if (args.length != 2) {
			System.out.println("Usage:");
			System.out.println("\t ASCIITabelwriter <input.xml> <output.ascii>");
			System.exit(1);
		}
		String inputFile = args[0];
		String outputFile = args[1];
		ASCIITableWriter tableWriter = new ASCIITableWriter(outputFile);

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(tableWriter);

		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(manager);
		reader.parse(inputFile);

		tableWriter.finish();

	}
}
