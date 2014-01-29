/**
 * 
 */
package josmMatsimPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which is executed after confirming the MATSimExportDialog
 * 
 * @author nkuehnel
 * 
 */

public class ExportTask extends PleaseWaitRunnable {
	static Properties matsimConvertProperties = new Properties();
	private int exportResult;
	private String path;
	private String targetSystem;
	
	private final int SUCCESS = 0;
	private final int VALIDATION_ERROR = 1;
	private List<TestError> validationErrors = new ArrayList<TestError>();

	public ExportTask() {
		super("MATSim Export");
		this.exportResult = 0;
		this.path = ExportDialog.exportFilePath.getText() + ".xml";
		this.targetSystem = (String) ExportDialog.exportSystem
				.getSelectedItem();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel() {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish() {
		if(exportResult == SUCCESS) {
			JOptionPane.showMessageDialog(Main.parent, "Export finished. File written to: "+path+" (WGS84 to "+ targetSystem + ")");
		} else if (exportResult == VALIDATION_ERROR) {
			JOptionPane.showMessageDialog(Main.parent, "Export failed due to validation errors. See validation layer for details.");
			OsmValidator.initializeErrorLayer();
            Main.map.validatorDialog.unfurlDialog();
	        Main.main.getEditLayer().validationErrors.clear();
	        Main.main.getEditLayer().validationErrors.addAll(this.validationErrors);
	        Main.map.validatorDialog.tree.setErrors(this.validationErrors );
		}
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException, UncheckedIOException {
		
		this.progressMonitor.setTicksCount(4);
		this.progressMonitor.setTicks(0);
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();
		CoordinateTransformation osmCt = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						targetSystem);

		Layer layer = Main.main.getActiveLayer();
		if (layer instanceof OsmDataLayer) {
			if (layer instanceof NetworkLayer) {
				this.progressMonitor.setTicks(1);
				this.progressMonitor.setCustomText("validating data..");
				
				DuplicateId test = new DuplicateId();
				test.startTest(NullProgressMonitor.INSTANCE);
				test.visit(((OsmDataLayer) layer).data.allPrimitives());
				test.endTest();

				if (test.getErrors().size() > 0) {
			        this.exportResult = VALIDATION_ERROR;
			        this.validationErrors.addAll(test.getErrors());
			        return;
			        
			    
				} else {
					this.progressMonitor.setTicks(2);
					this.progressMonitor.setCustomText("rearranging data..");

					for (Node node : ((NetworkLayer) layer).getMatsimNetwork()
							.getNodes().values()) {
						Node newNode = network.getFactory().createNode(
								new IdImpl(((NodeImpl) node).getOrigId()),
								node.getCoord());
						CoordinateTransformation customCt = TransformationFactory
								.getCoordinateTransformation(
										((NetworkLayer) layer).getCoordSystem(),
										targetSystem);
						Coord temp = customCt.transform(node.getCoord());
						node.getCoord().setXY(temp.getX(), temp.getY());
						network.addNode(newNode);
					}
					for (Link link : ((NetworkLayer) layer).getMatsimNetwork()
							.getLinks().values()) {
						Link newLink = network.getFactory().createLink(
								new IdImpl(((LinkImpl) link).getOrigId()),
								network.getNodes().get(
										new IdImpl(((NodeImpl) link
												.getFromNode()).getOrigId())),
								network.getNodes().get(
										new IdImpl(
												((NodeImpl) link.getToNode())
														.getOrigId())));
						newLink.setFreespeed(link.getFreespeed());
						newLink.setCapacity(link.getCapacity());
						newLink.setLength(link.getLength());
						newLink.setNumberOfLanes(link.getNumberOfLanes());
						newLink.setAllowedModes(link.getAllowedModes());
						network.addLink(newLink);
					}
				}
			} else {
				this.progressMonitor.setTicks(1);
				this.progressMonitor.setCustomText("converting osm data..");
				
				Converter converter = new Converter(
						((OsmDataLayer) layer).data, network);
				converter.convert();
				if (!(targetSystem.equals(TransformationFactory.WGS84))) {
					for (Node node : ((NetworkImpl) network).getNodes()
							.values()) {
						Coord temp = osmCt.transform(node.getCoord());
						node.getCoord().setXY(temp.getX(), temp.getY());
					}
				}
			}

			if (Main.pref.getBoolean("matsim_cleanNetwork")) {
				this.progressMonitor.setTicks(3);
				this.progressMonitor.setCustomText("cleaning network..");
				new NetworkCleaner().run(network);
			}
			((NetworkImpl) network).setCapacityPeriod(Double
					.parseDouble(ExportDialog.capacityPeriod.getText()));
			((NetworkImpl) network).setEffectiveLaneWidth(Double
					.parseDouble(ExportDialog.effectiveLaneWidth.getText()));
			this.progressMonitor.setTicks(4);
			this.progressMonitor.setCustomText("writing out xml file..");
			new NetworkWriter(network).write(path);
		}
	}
}