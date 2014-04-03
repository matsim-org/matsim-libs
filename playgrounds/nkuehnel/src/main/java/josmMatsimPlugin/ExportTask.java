/**
 * 
 */
package josmMatsimPlugin;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which which writes out the network xml file
 * 
 * @author nkuehnel
 * 
 */

public class ExportTask extends PleaseWaitRunnable {
	protected int exportResult;

	private File file;

	public ExportTask(File file) {
		super("MATSim Export");
		this.file = file;
		if (!this.file.getAbsolutePath().endsWith(".xml")) {
			this.file = new File(this.file.getAbsolutePath() + ".xml");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish() {
		JOptionPane.showMessageDialog(Main.parent,
				"Export finished. File written to: " + file.getPath());
	}

	/*
	 * (non-Javadoc)
	 * 
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

		Layer layer = Main.main.getActiveLayer();

		if (layer instanceof OsmDataLayer) {
			if (layer instanceof NetworkLayer) {
				this.progressMonitor.setTicks(1);
				this.progressMonitor.setCustomText("rearranging data..");

				for (Node node : ((NetworkLayer) layer).getMatsimNetwork()
						.getNodes().values()) {
					Node newNode = network.getFactory().createNode(
							new IdImpl(((NodeImpl) node).getOrigId()),
							node.getCoord());
					network.addNode(newNode);
				}
				for (Link link : ((NetworkLayer) layer).getMatsimNetwork()
						.getLinks().values()) {
					Link newLink = network.getFactory().createLink(
							new IdImpl(((LinkImpl) link).getOrigId()),
							network.getNodes().get(
									new IdImpl(((NodeImpl) link.getFromNode())
											.getOrigId())),
							network.getNodes().get(
									new IdImpl(((NodeImpl) link.getToNode())
											.getOrigId())));
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
			this.progressMonitor.setCustomText("writing out xml file..");
			new NetworkWriter(network).write(file.getAbsolutePath());
		}
	}
}