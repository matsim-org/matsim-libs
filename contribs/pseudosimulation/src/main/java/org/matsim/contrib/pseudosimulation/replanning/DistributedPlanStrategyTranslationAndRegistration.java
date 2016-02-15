package org.matsim.contrib.pseudosimulation.replanning;

import org.matsim.contrib.pseudosimulation.RunPSim;
import org.matsim.contrib.pseudosimulation.replanning.factories.DistributedPlanMutatorStrategyFactory;
import org.matsim.contrib.pseudosimulation.replanning.factories.DistributedPlanSelectorStrategyFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.strategies.*;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fouriep
 *         <p/>
 *         Essentially a lookup used in config translation. Also registers
 *         extended strategies with the controler.
 *         <p/>
 *         If a mutating strategy is sent for pseudo-simulation, it needs to be
 *         marked as such, and registered with the {@link RunPSim}.
 *         Non-mutating strategies, e.g. selector strategies, should be disabled
 *         during PSim iterations, and only run during QSim iterations.
 *         <p/>
 *         <p/>
 *         This class records strategies that should work with PSim. It creates staretgy delegate instances
 *         and adds mutated strategies to PSim.
 *         Each factory is registered during controler
 *         construction, and the config entries are changed to refer to their
 *         PSim equivalents in the controler's substituteStrategies() method.
 *         <p/>
 *         <p/>
 *         <p/>
 *         <p/>
 *         <B>NOTE:</B> to save processing overhead, selector strategies are set
 *         up to always return the person's current selected plan during
 *         non-QSim iterations.
 */
public class DistributedPlanStrategyTranslationAndRegistration {
    public static final String SUFFIX = "PSIM";
    public static Map<String, Class<? extends Provider<PlanStrategy>>> SupportedSelectors = new HashMap<>();
    public static Map<String, Class<? extends Provider<PlanStrategy>>> SupportedMutators = new HashMap<>();
    public static Map<String, Character> SupportedMutatorGenes = new HashMap<>();
    public static boolean TrackGenome = false;

    private DistributedPlanStrategyTranslationAndRegistration() {
    }

    static void initMaps() {
        SupportedSelectors.put("KeepLastSelected", KeepLastSelectedPlanStrategyProvider.class);
        SupportedSelectors.put("BestScore", SelectBestPlanStrategyProvider.class);
        SupportedSelectors.put("ChangeExpBeta", ChangeExpBetaPlanStrategyProvider.class);
        SupportedSelectors.put("SelectExpBeta", SelectExpBetaPlanStrategyProvider.class);
        SupportedSelectors.put("SelectRandom", SelectRandomPlanStrategyProvider.class);
        SupportedSelectors.put("SelectPathSizeLogit", SelectPathSizeLogitPlanStrategyProvider.class);

        SupportedMutators.put("ReRoute", ReRoutePlanStrategyProvider.class);
        SupportedMutators.put("TimeAllocationMutator", TimeAllocationMutatorPlanStrategyProvider.class);
        SupportedMutators.put("TimeAllocationMutator_ReRoute", TimeAllocationMutatorReRoutePlanStrategyProvider.class);
        SupportedMutators.put("ChangeSingleTripMode", ChangeSingleTripModePlanStrategyProvider.class);
        SupportedMutators.put("SubtourModeChoice", SubtourModeChoicePlanStrategyProvider.class);
        SupportedMutators.put("ChangeTripMode", ChangeTripModePlanStrategyProvider.class);

        SupportedMutatorGenes.put("ReRoute", new Character('A'));
        SupportedMutatorGenes.put("TimeAllocationMutator", new Character('B'));
        SupportedMutatorGenes.put("TimeAllocationMutator_ReRoute", new Character('C'));
        SupportedMutatorGenes.put("ChangeLegMode", new Character('D'));
        SupportedMutatorGenes.put("ChangeSingleLegMode", new Character('E'));
        SupportedMutatorGenes.put("ChangeSingleTripMode", new Character('F'));
        SupportedMutatorGenes.put("SubtourModeChoice", new Character('G'));
        SupportedMutatorGenes.put("ChangeTripMode", new Character('H'));
        SupportedMutatorGenes.put("TripSubtourModeChoice", new Character('J'));
        SupportedMutatorGenes.put("TransitLocationChoice", new Character('K'));

    }

    public static void registerStrategiesWithControler(final Controler controler, final PlanCatcher slave, final boolean quickReplanning, final int selectionInflationFactor) {
        for (final Map.Entry<String, Class<? extends Provider<PlanStrategy>>> e : SupportedSelectors.entrySet()) {
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addPlanStrategyBinding(e.getKey() + SUFFIX).toProvider(new DistributedPlanSelectorStrategyFactory(slave, quickReplanning, selectionInflationFactor, controler, e.getKey()));
                }
            });

        }

        for (final Map.Entry<String, Class<? extends Provider<PlanStrategy>>> e : SupportedMutators.entrySet()) {
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addPlanStrategyBinding(e.getKey() + SUFFIX).toProvider(new DistributedPlanMutatorStrategyFactory(slave, SupportedMutatorGenes.get(e.getKey()), TrackGenome, controler, e.getKey()));
                }
            });
        }

    }


    public static boolean isStrategySupported(String name) {
        if (SupportedSelectors.size() == 0)
            initMaps();
        if (SupportedSelectors.containsKey(name))
            return true;
        else return SupportedMutators.containsKey(name);
    }

    public static void substituteStrategies(Config config, boolean quickReplanning, int selectionInflationFactor) {
        for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {

            String classname = settings.getStrategyName();

            if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
                classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
                settings.setStrategyName(classname);
            }
            if (!DistributedPlanStrategyTranslationAndRegistration.isStrategySupported(classname)) {
                throw new RuntimeException("Strategy " + classname + " not known to be compatible with (Distributed) PSim. Exiting.");
            } else {
                if (SupportedMutators.containsKey(classname))
                    settings.setStrategyName(classname + DistributedPlanStrategyTranslationAndRegistration.SUFFIX);
                if (SupportedSelectors.containsKey(classname)) {
                    settings.setStrategyName(classname + DistributedPlanStrategyTranslationAndRegistration.SUFFIX);
                    //implement quick replanning by simply multiplying the selector weights by the number of PSim Iterations
                    if (quickReplanning) {
                        settings.setWeight(settings.getWeight() * (double) selectionInflationFactor);
                    }
                }
            }


        }

    }
}





