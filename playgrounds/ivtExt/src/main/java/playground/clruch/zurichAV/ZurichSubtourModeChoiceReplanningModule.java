package playground.clruch.zurichAV;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.TripRouter;
import playground.sebhoerl.avtaxi.framework.AVModule;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ZurichSubtourModeChoiceReplanningModule extends AbstractMultithreadedModule {
    final private Network network;
    final private Collection<Link> permissibleLinks;

    final private Provider<TripRouter> tripRouterProvider;
    final private PermissibleModesCalculator permissibleModesCalculator;

    final private List<String> modes;
    final private List<String> modesWithoutAV;
    final private List<String> chainBasedModes;

    public ZurichSubtourModeChoiceReplanningModule(
            Provider<TripRouter> tripRouterProvider, GlobalConfigGroup globalConfigGroup, SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup,
            Network network, Collection<Link> permissibleLinks) {
        super(globalConfigGroup.getNumberOfThreads());

        this.tripRouterProvider = tripRouterProvider;
        this.modes = Arrays.asList(subtourModeChoiceConfigGroup.getModes());
        this.modesWithoutAV = this.modes.stream().filter((s) -> !s.equals(AVModule.AV_MODE)).collect(Collectors.toList());
        this.chainBasedModes = Arrays.asList(subtourModeChoiceConfigGroup.getChainBasedModes());

        this.permissibleModesCalculator = new PermissibleModesCalculatorImpl((String[]) this.modes.toArray(), subtourModeChoiceConfigGroup.considerCarAvailability());

        this.network = network;
        this.permissibleLinks = permissibleLinks;
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {
        TripRouter tripRouter = tripRouterProvider.get();

        String[] modes = new String[this.modes.size()];
        String[] modesWithoutAV = new String[this.modesWithoutAV.size()];
        String[] chainBasedModes = new String[this.chainBasedModes.size()];

        modes = this.modes.toArray(modes);
        modesWithoutAV = this.modesWithoutAV.toArray(modesWithoutAV);
        chainBasedModes = this.chainBasedModes.toArray(chainBasedModes);

        PlanAlgorithm withAVsAlgorithm = new ChooseRandomLegModeForSubtour(
                tripRouter.getStageActivityTypes(),
                tripRouter.getMainModeIdentifier(),
                permissibleModesCalculator,
                modes,
                chainBasedModes,
                MatsimRandom.getLocalInstance());

        PlanAlgorithm withoutAVsAlgorithm = new ChooseRandomLegModeForSubtour(
                tripRouter.getStageActivityTypes(),
                tripRouter.getMainModeIdentifier(),
                permissibleModesCalculator,
                modesWithoutAV,
                chainBasedModes,
                MatsimRandom.getLocalInstance());

        return new ZurichSubtourModeChoiceAlgorithm(network, tripRouter.getStageActivityTypes(), withAVsAlgorithm, withoutAVsAlgorithm, permissibleLinks);
    }
}
