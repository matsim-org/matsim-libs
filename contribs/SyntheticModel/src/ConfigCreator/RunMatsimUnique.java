package ConfigCreator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.analysis.VehicleTracker;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.analysis.TransitRouteAccessEgressAnalysis;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;


public class RunMatsimUnique {

	public static void main(String[] args) throws IOException {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "examples/scenarios/UrbanLine/3x1km/config.xml", new MultiModeDrtConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup() );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory("examples/scenarios/UrbanLine/output");
		// possibly modify config here
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation((QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime));

		Controler controler = DrtControlerCreator.createControler(config, false);

		Scenario scenario = controler.getScenario();
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		TransitRoute transitRoute = transitSchedule.getTransitLines().get(Id.create("Shuttle", TransitRoute.class)).getRoutes().get(Id.create("Suburbs", TransitRoute.class));
		VehicleTracker vehicleTracker = new VehicleTracker();

		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(transitRoute, vehicleTracker);

		// Add the analysis and vehicle tracker as event handlers
		controler.getEvents().addHandler(analysis);
		controler.getEvents().addHandler(vehicleTracker);

		// Run the simulation
		controler.run();

		// Print the statistics after the simulation
		analysis.printStats();

		Desktop.getDesktop().open(new File(config.controler().getOutputDirectory() + "/modestats_stackedbar.png"));


	}
}
