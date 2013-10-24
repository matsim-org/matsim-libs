/**
 * 
 */
package josmMatsimPlugin;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import josmMatsimPlugin.Defaults.OsmHighwayDefaults;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
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
	protected void finish()
	{
		JOptionPane.showMessageDialog(Main.main.parent, "Export finished.");
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
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						Defaults.targetSystem);
		
		Layer layer = Main.main.getActiveLayer();
		
		if (layer instanceof OsmDataLayer)
		{
			if (layer instanceof NetworkLayer)
			{
				net = ((NetworkLayer) layer).getMatsimNetwork();
				if(!(((NetworkLayer) layer).getCoordSystem().equals(Defaults.targetSystem)))
				{
					for(Node node: ((NetworkImpl)net).getNodes().values())
					{
						Coord temp=ct.transform(node.getCoord());
						node.getCoord().setXY(temp.getX(), temp.getY());
					}
				}
			}
			else
			{
				Converter converter = new Converter(((OsmDataLayer) layer).data, net, Defaults.defaults, Defaults.keepPaths);
				converter.convert();
				if (!(Defaults.targetSystem.equals("WGS84")))
				{
					for(Node node: ((NetworkImpl)net).getNodes().values())
					{
						Coord temp=ct.transform(node.getCoord());
						node.getCoord().setXY(temp.getX(), temp.getY());
					}
				}
			}
			
			if(Defaults.cleanNet==true)
			{	
				new NetworkCleaner().run(net);
			}
			new NetworkWriter(net).write(Defaults.exportPath+".xml");
		}
		System.out.println("schreibe: "+Defaults.exportPath+".xml"+" von WGS84 nach "+Defaults.targetSystem);
	}

}