package tutorial.unsupported.mobsimPassingVehicleQ;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example to show how the standard queue can be replaced by something else.  Search for PassingVehicleQ in the code below.
 * <p/>
 * I have a version of this that was running about a year ago in my playground.  This "tutorial" version has never been tested (but please
 * feel free to test, report back, and fix).
 * 
 * @author nagel
 *
 */
class MyControler {

	public static void main ( String[] args ) {
		Logger.getLogger("blabla").warn("here") ;

		// prepare the config:
		@SuppressWarnings("unchecked")
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true) ;

		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;
		controler.setMobsimFactory(new PatnaMobsimFactory()) ;

		// run everything:
		controler.run();

	}

	/**
	 * Look into {@link QSimFactory} for the default matsim qsim factory.  This is copy and paste (and somewhat reduced).
	 * 
	 * @author nagel
	 *
	 */
	static class PatnaMobsimFactory implements MobsimFactory {

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

			QSimConfigGroup conf = sc.getConfig().qsim();
			if (conf == null) {
				throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
			}

			// construct the QSim:
			QSim qSim = new QSim(sc, eventsManager);

			// add the actsim engine:
			ActivityEngine activityEngine = new ActivityEngine();
			qSim.addMobsimEngine(activityEngine);
			qSim.addActivityHandler(activityEngine);

			// add the netsim engine:
			NetsimNetworkFactory<QNode, QLinkImpl> netsimNetworkFactory = new NetsimNetworkFactory<QNode, QLinkImpl>() {
				@Override
				public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
					// !! this is the important line (note the PassingVehicleQ) !!:
					return new QLinkImpl(link, network, toQueueNode, new PassingVehicleQ());
				}
				@Override
				public QNode createNetsimNode(final Node node, QNetwork network) {
					return new QNode(node, network);
				}
			};
			QNetsimEngine netsimEngine = new QNetsimEngine(qSim, netsimNetworkFactory) ;
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

			TeleportationEngine teleportationEngine = new TeleportationEngine();
			qSim.addMobsimEngine(teleportationEngine);

			// The following is only necessary if you want to add vehicles of different length.
//			AgentFactory agentFactory;
//
//			agentFactory = new DefaultAgentFactory(qSim);
//
//			PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
//			Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();
//
//			VehicleType car = VehicleUtils.getFactory().createVehicleType(new IdImpl("car"));
//			car.setMaximumVelocity(60.0/3.6);
//			car.setPcuEquivalents(1.0);
//			modeVehicleTypes.put("car", car);
//
//			VehicleType bike = VehicleUtils.getFactory().createVehicleType(new IdImpl("bike"));
//			bike.setMaximumVelocity(60.0/3.6);
//			bike.setPcuEquivalents(0.25);
//			modeVehicleTypes.put("bike", bike);
//
//			VehicleType bicycles = VehicleUtils.getFactory().createVehicleType(new IdImpl("bicycle"));
//			bicycles.setMaximumVelocity(15.0/3.6);
//			bicycles.setPcuEquivalents(0.05);
//			modeVehicleTypes.put("bicycle", bicycles);
//
//			VehicleType walks = VehicleUtils.getFactory().createVehicleType(new IdImpl("walk"));
//			walks.setMaximumVelocity(1.5);
//			walks.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
//			modeVehicleTypes.put("walk", walks);
//
//			agentSource.setModeVehicleTypes(modeVehicleTypes);
//
//			qSim.addAgentSource(agentSource);

			return qSim ;
		}
	}
}
