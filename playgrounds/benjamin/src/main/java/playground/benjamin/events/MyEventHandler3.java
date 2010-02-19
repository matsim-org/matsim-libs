package playground.benjamin.events;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

/**
 * This EventHandler implementation counts the traffic volume on the link with
 * id number 6 and provides a method to write the hourly volumes to a chart png.
 * 
 * @author dgrether
 * 
 */
public class MyEventHandler3 implements LinkEnterEventHandler {

	private double[] volumeLink6;

	public MyEventHandler3() {
		reset(0);
	}

	public double getTravelTime(int slot) {
		return this.volumeLink6[slot];
	}

	private int getSlot(double time) {
		return (int) time / 3600;
	}

	public void reset(int iteration) {
		this.volumeLink6 = new double[24];
	}

	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(new IdImpl("6"))) {
			this.volumeLink6[getSlot(event.getTime())]++;
		}
	}

	public void writeChart(String filename) {
		double[] hours = new double[24];

		for (double i = 0.0; i < 24.0; i++) {
			hours[(int) i] = i;
		}
		XYLineChart chart = new XYLineChart("Traffic link 6", "hour", "departures");
		chart.addSeries("departures", hours, this.volumeLink6);
		chart.saveAsPng(filename, 800, 600);
	}

	public void writeTxt(String filename) {
		int[] hours = new int[24];
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < 24; i++) {
			hours[(int) i] = i;

			buffer.append(hours[i]);
			buffer.append("\t");
			buffer.append(volumeLink6[i]);
			buffer.append("\n");
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.write(buffer.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}