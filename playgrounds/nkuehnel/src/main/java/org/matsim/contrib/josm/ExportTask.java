package org.matsim.contrib.josm;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which which writes out the network xml file
 * 
 * @author Nico
 * 
 */

class ExportTask extends PleaseWaitRunnable {

	private File networkFile;
	private File scheduleFile;

	/**
	 * Creates a new Export task with the given export <code>file</code>
	 * location
	 * 
	 * @param file
	 *            The file to be exported to
	 */
	public ExportTask(File file) {
		super("MATSim Export");
		this.networkFile = new File(file.getAbsolutePath() + "/network.xml");
		this.scheduleFile = new File(file.getAbsolutePath()
				+ "/transit_schedule.xml");
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel() {
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish() {
		JOptionPane.showMessageDialog(Main.parent,
				"Export finished. File written to: " + networkFile.getPath());
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException, UncheckedIOException {

		this.progressMonitor.setTicksCount(3);
		this.progressMonitor.setTicks(0);

		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();
		TransitSchedule schedule = null;

		Layer layer = Main.main.getActiveLayer();

		if (layer instanceof OsmDataLayer) {
			if (layer instanceof MATSimLayer) {
				schedule = ((MATSimLayer) layer).getMatsimScenario()
						.getTransitSchedule();
				this.progressMonitor.setTicks(1);
				this.progressMonitor.setCustomText("rearranging data..");

				for (Node node : ((MATSimLayer) layer).getMatsimScenario()
						.getNetwork().getNodes().values()) {
					Node newNode = network.getFactory().createNode(
							Id.create(((NodeImpl) node).getOrigId(), Node.class),
							node.getCoord());
					network.addNode(newNode);
				}
				for (Link link : ((MATSimLayer) layer).getMatsimScenario()
						.getNetwork().getLinks().values()) {
					Link newLink = network.getFactory().createLink(
							Id.create(((LinkImpl) link).getOrigId(), Link.class),
							network.getNodes().get(
									Id.create(((NodeImpl) link.getFromNode()).getOrigId(), Link.class)),
							network.getNodes().get(
									Id.create(((NodeImpl) link.getToNode()).getOrigId(), Node.class)));
					newLink.setFreespeed(link.getFreespeed());
					newLink.setCapacity(link.getCapacity());
					newLink.setLength(link.getLength());
					newLink.setNumberOfLanes(link.getNumberOfLanes());
					newLink.setAllowedModes(link.getAllowedModes());
					network.addLink(newLink);
				}
			}

			if (Main.pref.getBoolean("matsim_cleanNetwork")) {
				this.progressMonitor.setTicks(2);
				this.progressMonitor.setCustomText("cleaning network..");
				new NetworkCleaner().run(network);
			}
			this.progressMonitor.setTicks(3);
			this.progressMonitor.setCustomText("writing out xml file(s)..");
			new NetworkWriter(network).write(networkFile.getAbsolutePath());
			if (schedule != null) {
				new TransitScheduleWriter(schedule).writeFile(scheduleFile
						.getAbsolutePath());
			}
		}
	}
}