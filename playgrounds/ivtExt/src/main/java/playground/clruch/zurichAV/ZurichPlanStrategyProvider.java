package playground.clruch.zurichAV;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import java.util.Collection;

public class ZurichPlanStrategyProvider implements Provider<PlanStrategy> {
    @Inject private Provider<TripRouter> tripRouterProvider;
    @Inject private GlobalConfigGroup globalConfigGroup;
    @Inject private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
    @Inject private ActivityFacilities facilities;
    @Inject private Network network;
    @Inject @Named("zurich") Collection<Link> permissibleLinks;

    @Override
    public PlanStrategy get() {
        PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector());
        builder.addStrategyModule(new ZurichSubtourModeChoiceReplanningModule(tripRouterProvider, globalConfigGroup, subtourModeChoiceConfigGroup, network, permissibleLinks));
        builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup));
        return builder.build();
    }
}
