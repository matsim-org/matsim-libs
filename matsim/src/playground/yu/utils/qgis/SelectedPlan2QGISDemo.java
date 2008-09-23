/**
 * 
 */
package playground.yu.utils.qgis;

import java.io.IOException;

import org.jfree.util.Log;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape and can
 * convert a MATSim-population to a QGIS .shp-file (acts or legs)
 * 
 * @author ychen
 * 
 */
public class SelectedPlan2QGISDemo implements X2QGIS {
	public static void main(final String[] args) {
		// final String populationFilename = "./examples/equil/plans100.xml";
		final String populationFilename = "../runs/run628/it.500/500.plans.xml.gz";
		// final String networkFilename = "./examples/equil/network.xml";
		final String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String outputDir = "../runs/run628/it.500/";

		Gbl.createConfig(null);
		Gbl.createWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);

		Gbl.getWorld().setNetworkLayer(network);

		Population population = new Population();
		new MatsimPopulationReader(population).readFile(populationFilename);
		/*
		 * ----------------------------------------------------------------------
		 */
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population,
				crs, outputDir);
		sp.setOutputSample(0.05);
		sp.setActBlurFactor(100);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(true);

		try {
			sp.write();
		} catch (IOException e) {
			Log.error(e.getMessage(), e);
		}
		/*
		 * ----------------------------------------------------------------------
		 */
	}
}
