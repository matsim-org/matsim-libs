/**
 * 
 */
package josmMatsimPlugin;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which is executed after confirming the MATSimExportDialog
 * @author nkuehnel
 * 
 */

public class ExportTask extends PleaseWaitRunnable
{
	static Properties matsimConvertProperties = new Properties();
	public ExportTask()
	{
		super("matsimexport");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel()
	{
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish() {
		JOptionPane.showMessageDialog(Main.main.parent, "Export finished.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
	OsmTransferException, UncheckedIOException {
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();
		String targetSystem = (String) ExportDialog.exportSystem.getSelectedItem();
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						targetSystem);

		Layer layer = Main.main.getActiveLayer();
		if (layer instanceof OsmDataLayer) {
			if (layer instanceof NetworkLayer) {
				for(Node node : ((NetworkLayer) layer).getMatsimNetwork().getNodes().values()) {
					Node newNode = network.getFactory().createNode(new IdImpl(((NodeImpl)node).getOrigId()), node.getCoord());
					network.addNode(newNode);
				}
				for(Link link : ((NetworkLayer) layer).getMatsimNetwork().getLinks().values()) {
					Link newLink = network.getFactory().createLink(
							new IdImpl(((LinkImpl) link).getOrigId()), 
							network.getNodes().get(new IdImpl(((NodeImpl) link.getFromNode()).getOrigId())), 
							network.getNodes().get(new IdImpl(((NodeImpl) link.getToNode()).getOrigId())));
					newLink.setFreespeed(link.getFreespeed());
					newLink.setCapacity(link.getCapacity());
					newLink.setLength(link.getLength());
					newLink.setNumberOfLanes(link.getNumberOfLanes());
					newLink.setAllowedModes(link.getAllowedModes());
					network.addLink(newLink);
				}
			} else {
				Converter converter = new Converter(((OsmDataLayer) layer).data, network);
				converter.convert();
				if (!(targetSystem.equals("WGS84"))) {
					for(Node node: ((NetworkImpl) network).getNodes().values()) {
						Coord temp=ct.transform(node.getCoord());
						node.getCoord().setXY(temp.getX(), temp.getY());
					}
				}
			}
			if (Main.pref.getBoolean("matsim_cleanNetwork")) {
				new NetworkCleaner().run(network);
			}
			String path = ExportDialog.exportFilePath.getText()+".xml";
			new NetworkWriter(network).write(path);
			System.out.println("schreibe: " + path + " von WGS84 nach "+targetSystem);
		}
	}

}