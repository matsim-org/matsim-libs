package playground.wrashid.parkingChoice.freeFloatingCarSharing;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.qsimParkingModule.FreeFloatingQsimFactory;
import playground.balac.freefloating.routerparkingmodule.FreeFloatingRoutingModule;
import playground.balac.freefloating.scoring.FreeFloatingScoringFunctionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FreeFloatingWithParkingControler extends Controler {

	public FreeFloatingWithParkingControler(Scenario scenario) {
		super(scenario);
	}

	public void init(Config config, Network network, Scenario sc) {
		FreeFloatingScoringFunctionFactory ffScoringFunctionFactory = new FreeFloatingScoringFunctionFactory(config, network, sc);
		this.setScoringFunctionFactory(ffScoringFunctionFactory);

	}

//	@Override
//	protected void loadControlerListeners() {
//
//		super.loadControlerListeners();
//		// this.addControlerListener(new FFListener(
//		// this.getConfig().getModule("FreeFloating").getValue("statsFileName")));
//	}

	public static void main(final String[] args) {

		final Config config = ConfigUtils.loadConfig(args[0]);
		FreeFloatingConfigGroup configGroup = new FreeFloatingConfigGroup();
		config.addModule(configGroup);
		final Scenario sc = ScenarioUtils.loadScenario(config);

		final FreeFloatingWithParkingControler controler = new FreeFloatingWithParkingControler(sc);
		try {

			final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup) sc.getConfig().getModule(
					FreeFloatingConfigGroup.GROUP_NAME);

			BufferedReader reader;
			String s;

			controler.setTripRouterFactory(new TripRouterFactory() {
				@Override
				public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
					// this factory initializes a TripRouter with default
					// modules,
					// taking into account what is asked for in the config

					// This allows us to just add our module and go.
					final TripRouterFactory delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(controler
							.getScenario());

					final TripRouter router = delegate.instantiateAndConfigureTripRouter(routingContext);

					// add our module to the instance
					router.setRoutingModule("freefloating", new FreeFloatingRoutingModule());

					// we still need to provide a way to identify our trips
					// as being freefloating trips.
					// This is for instance used at re-routing.
					final MainModeIdentifier defaultModeIdentifier = router.getMainModeIdentifier();
					router.setMainModeIdentifier(new MainModeIdentifier() {
						@Override
						public String identifyMainMode(final List<PlanElement> tripElements) {
							for (PlanElement pe : tripElements) {
								if (pe instanceof Leg && ((Leg) pe).getMode().equals("freefloating")) {
									return "freefloating";
								}
							}
							// if the trip doesn't contain a freefloating leg,
							// fall back to the default identification method.
							return defaultModeIdentifier.identifyMainMode(tripElements);
						}
					});

					return router;
				}

			});
			controler.init(config, sc.getNetwork(), sc);

			if (configGroupff.useFeeFreeFloating()) {
				reader = IOUtils.getBufferedReader(configGroupff.getvehiclelocations());
				s = reader.readLine();
				int i = 1;

				final ArrayList<ParkingCoordInfo> freefloatingCars = new ArrayList<ParkingCoordInfo>();
				while (s != null) {

					String[] arr = s.split("\t", -1);

					Link l = controler.getScenario().getNetwork().getLinks().get(Id.create(arr[0], Link.class));

					for (int k = 0; k < Integer.parseInt(arr[1]); k++) {
						ParkingCoordInfo parkingInfo = new ParkingCoordInfo(Id.create(i, Vehicle.class), l.getCoord());
						freefloatingCars.add(parkingInfo);
						i++;
					}

					s = reader.readLine();

				}

				final ParkingModuleWithFFCarSharingZH parkingModule = new ParkingModuleWithFFCarSharingZH(controler, freefloatingCars);
				// ParkingModuleWithFreeFloatingCarSharing parkingModule = new
				// ParkingModuleWithFFCarSharing();

				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bindMobsim().toProvider(new Provider<Mobsim>() {
							@Override
							public Mobsim get() {
								return new FreeFloatingQsimFactory(sc, controler, parkingModule, freefloatingCars).createMobsim(controler.getScenario(), controler.getEvents());
							}
						});
					}
				});
			}

			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

			controler.run();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

}
