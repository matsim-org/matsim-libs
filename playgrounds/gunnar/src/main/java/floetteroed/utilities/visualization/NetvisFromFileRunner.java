/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

import static floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader.MATSIM_NETWORK_TYPE;
import static floetteroed.utilities.networks.containerloaders.OpenStreetMapNetworkContainerLoader.OPENSTREETMAP_NETWORK_TYPE;
import static floetteroed.utilities.networks.containerloaders.SUMONetworkContainerLoader.SUMO_NETWORK_TYPE;

import java.awt.FileDialog;
import java.awt.Frame;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.ErrorMsgPrinter;
import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;
import floetteroed.utilities.networks.construction.NetworkContainer;
import floetteroed.utilities.networks.construction.NetworkPostprocessor;
import floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader;
import floetteroed.utilities.networks.containerloaders.OpenStreetMapNetworkContainerLoader;
import floetteroed.utilities.networks.containerloaders.SUMONetworkContainerLoader;


/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class NetvisFromFileRunner {

	// -------------------- CONSTANTS --------------------

	public static final String NETVISCONFIG_ELEMENT = "config";

	public static final String NETWORKTYPE_ELEMENT = "networktype";

	// consistent with bioroute.networkloader.AbstractNetworkLoader
	public static final String NETWORKFILENAME_ELEMENT = "networkfile";

	// consistent with bioroute.networkloader.SUMONetworkLoader
	public static final String NODEFILE_ELEMENT = "nodefile";

	// consistent with bioroute.networkloader.SUMONetworkLoader
	public static final String EDGEFILE_ELEMENT = "edgefile";

	// consistent with bioroute.networkloader.SUMONetworkLoader
	public static final String CONNECTIONFILE_ELEMENT = "connectionfile";

	public static final String COLOR_ELEMENT = "color";

	public static final String DELAY_ELEMENT = "delay";

	public static final String LINKDATAFILE_ELEMENT = "linkdatafile";

	public static final String LINKWIDTH_ELEMENT = "linkwidth";

	public static final String LOGO_ELEMENT = "logo";

	public static final String MULTILANE_ELEMENT = "multilane";

	public static final String SHOWLINKLABELS_ELEMENT = "showlinklabels";

	public static final String SHOWNODELABELS_ELEMENT = "shownodelabels";

	public static final String ANTIALIASING_ELEMENT = "antialiasing";

	// -------------------- CONSTRUCTION --------------------

	protected NetvisFromFileRunner() {
		// do not instantiate from outside
	}

	protected void run(final String[] args) {

		try {

			/*
			 * (1) obtain absolute name of config file
			 */
			final String configFile;
			if (args.length == 0) {
				final FileDialog fileDialog = new FileDialog(new Frame(),
						"Select visualization configuration file.");
				fileDialog.setMode(FileDialog.LOAD);
				fileDialog.setVisible(true);
				if (fileDialog.getFile() == null) {
					return;
				}
				configFile = (fileDialog.getDirectory() == null ? ""
						: fileDialog.getDirectory()) + fileDialog.getFile();
			} else {
				configFile = args[0];
			}

			/*
			 * (2) load configuration and network container
			 */
			final Config config = (new ConfigReader()).read(configFile);
			final String netType = config.get(NETVISCONFIG_ELEMENT,
					NETWORKTYPE_ELEMENT);
			final NetworkContainer container;
			if (OPENSTREETMAP_NETWORK_TYPE.equals(netType)) {
				container = (new OpenStreetMapNetworkContainerLoader())
						.load(config.absolutePath(config.get(
								NETVISCONFIG_ELEMENT, NETWORKFILENAME_ELEMENT)));
			} else if (MATSIM_NETWORK_TYPE.equals(netType)) {
				container = (new MATSimNetworkContainerLoader()).load(config
						.absolutePath(config.get(NETVISCONFIG_ELEMENT,
								NETWORKFILENAME_ELEMENT)));
			} else if (SUMO_NETWORK_TYPE.equals(netType)) {
				final String nodeFile = config.absolutePath(config.get(
						NETVISCONFIG_ELEMENT, NODEFILE_ELEMENT));
				final String edgeFile = config.absolutePath(config.get(
						NETVISCONFIG_ELEMENT, EDGEFILE_ELEMENT));
				String connectionFile = config.get(NETVISCONFIG_ELEMENT,
						CONNECTIONFILE_ELEMENT);
				if (connectionFile != null) {
					connectionFile = config.absolutePath(connectionFile);
				}
				container = (new SUMONetworkContainerLoader())
						.loadNetworkContainer(nodeFile, edgeFile,
								connectionFile);
			} else {
				container = this.loadUnknownNetworkContainer(netType, config);
				if (container == null) {
					throw new IllegalArgumentException("unknown network type");
				}
			}

			/*
			 * (3) create network data structure
			 */
			final VisNetworkFactory factory = new VisNetworkFactory();

			if (MATSimNetworkContainerLoader.MATSIM_NETWORK_TYPE
					.equals(container.getNetworkType())) {
				factory.setNetworkPostprocessor(new MATSim2VisNetwork());
			} else if (OpenStreetMapNetworkContainerLoader.OPENSTREETMAP_NETWORK_TYPE
					.equals(container.getNetworkType())) {
				factory.setNetworkPostprocessor(new OpenStreetMap2VisNetwork());
			} else if (SUMONetworkContainerLoader.SUMO_NETWORK_TYPE
					.equals(container.getNetworkType())) {
				factory.setNetworkPostprocessor(new SUMO2VisNetwork());
			} else {
				final NetworkPostprocessor<VisNetwork> postprocessor = this
						.getUnknownNetworkPostprocessorHook();
				if (postprocessor != null) {
					factory.setNetworkPostprocessor(postprocessor);
				} else {
					throw new RuntimeException("unknown network type: "
							+ container.getNetworkType());
				}
			}

			final VisNetwork net = factory.newNetwork(container);

			/*
			 * (4) move data into the "old" vis configuration class
			 */
			final VisConfig visConfig = new VisConfig();
			visConfig.setColorDef(config.get(NETVISCONFIG_ELEMENT,
					COLOR_ELEMENT));
			visConfig.setDelay_ms(Integer.parseInt(config.get(
					NETVISCONFIG_ELEMENT, DELAY_ELEMENT)));
			if (config.containsKeys(NETVISCONFIG_ELEMENT, LINKDATAFILE_ELEMENT)) {
				visConfig.setLinkDataFile(config.absolutePath(config.get(
						NETVISCONFIG_ELEMENT, LINKDATAFILE_ELEMENT)));
			}
			visConfig.setLinkWidthFactor(Integer.parseInt(config.get(
					NETVISCONFIG_ELEMENT, LINKWIDTH_ELEMENT)));
			visConfig.setLogo(config.get(NETVISCONFIG_ELEMENT, LOGO_ELEMENT));
			visConfig.setMultiLane(Boolean.parseBoolean(config.get(
					NETVISCONFIG_ELEMENT, MULTILANE_ELEMENT)));
			visConfig.setShowLinkLabels(Boolean.parseBoolean(config.get(
					NETVISCONFIG_ELEMENT, SHOWLINKLABELS_ELEMENT)));
			visConfig.setShowLinkLabels(Boolean.parseBoolean(config.get(
					NETVISCONFIG_ELEMENT, SHOWNODELABELS_ELEMENT)));
			visConfig.setUseAntiAliasing(Boolean.parseBoolean(config.get(
					NETVISCONFIG_ELEMENT, ANTIALIASING_ELEMENT)));

			/*
			 * (4) load link data
			 */
			final RenderableDynamicData<VisLink> renderData;
			if (visConfig.getLinkDataFile() != null) {
				final LinkDataIO<VisLink, VisNetwork> linkDataLoader = new LinkDataIO<VisLink, VisNetwork>(
						net);
				final DynamicData<VisLink> linkData = linkDataLoader
						.read(visConfig.getLinkDataFile());
				renderData = new RenderableDynamicData<VisLink>(linkData);
			} else {
				renderData = null;
			}

			/*
			 * (5) display
			 */
			final NetVis vis = new NetVis(visConfig, net, renderData);
			vis.run();

		} catch (Exception e) {
			ErrorMsgPrinter.toStdOut(e);
			ErrorMsgPrinter.toErrOut(e);
		}
	}

	protected NetworkContainer loadUnknownNetworkContainer(
			final String networkType, final Config config) {
		return null;
	}

	protected NetworkPostprocessor<VisNetwork> getUnknownNetworkPostprocessorHook() {
		return null;
	}

	// -------------------- MAIN FUNCTION --------------------

	public static void main(String[] args) {
		final NetvisFromFileRunner runner = new NetvisFromFileRunner();
		runner.run(args);
	}

}
