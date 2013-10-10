/**
 * 
 */
package josmMatsimPlugin;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import josmMatsimPlugin.ExportDefaults.OsmHighwayDefaults;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
	static String targetSystem="WGS84";
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
	protected void finish()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException, UncheckedIOException
	{
		boolean keepPaths = MATSimExportDialog.keepPaths.isSelected();
		Map<String, OsmHighwayDefaults> defaults = ExportDefaults.defaults;
		
		
		AbstractJosm2Network writer;
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						targetSystem);
		
		Layer layer = Main.main.getActiveLayer();
		
		if (layer instanceof OsmDataLayer)
		{
			if (layer instanceof NetworkLayer)
			{
				writer = new Network2Network(net, ct, keepPaths);
			}
			else
			{
				writer = new Osm2Network(net, ct, keepPaths, targetSystem, defaults);
			}
			writer.writeLayer((OsmDataLayer) layer);
			writer.write(MATSimExportDialog.path.getText()+".xml");
		}
		
		new NetworkCleaner().run(net);
		System.out.println("schreibe: "+MATSimExportDialog.path.getText()+".xml"+" von WGS84 nach "+targetSystem);
	}

}