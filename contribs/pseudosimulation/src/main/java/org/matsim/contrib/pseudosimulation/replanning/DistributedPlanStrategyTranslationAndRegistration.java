package org.matsim.contrib.pseudosimulation.replanning;

import org.matsim.contrib.pseudosimulation.PSimControler;
import org.matsim.contrib.pseudosimulation.replanning.factories.DistributedPlanMutatorStrategyFactory;
import org.matsim.contrib.pseudosimulation.replanning.factories.DistributedPlanSelectorStrategyFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.modules.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fouriep
 *         <p/>
 *         Essentially a lookup used in config translation. Also registers
 *         extended strategies with the controler.
 *         <p/>
 *         If a mutating strategy is sent for pseudo-simulation, it needs to be
 *         marked as such, and registered with the {@link PSimControler}.
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
    public static Map<String, Class<? extends PlanStrategyFactory>> SupportedSelectors = new HashMap<>();
    public static Map<String, Class<? extends PlanStrategyFactory>> SupportedMutators = new HashMap<>();
    public static Map<String, Character> SupportedMutatorGenes = new HashMap<>();
    public static boolean TrackGenome = false;

    private DistributedPlanStrategyTranslationAndRegistration() {
    }

    static void initMaps() {
        SupportedSelectors.put("KeepLastSelected", KeepLastSelectedPlanStrategyFactory.class);
        SupportedSelectors.put("BestScore", SelectBestPlanStrategyFactory.class);
        SupportedSelectors.put("ChangeExpBeta", ChangeExpBetaPlanStrategyFactory.class);
        SupportedSelectors.put("SelectExpBeta", SelectExpBetaPlanStrategyFactory.class);
        SupportedSelectors.put("SelectRandom", SelectRandomStrategyFactory.class);
        SupportedSelectors.put("SelectPathSizeLogit", SelectPathSizeLogitStrategyFactory.class);

        SupportedMutators.put("ReRoute", ReRoutePlanStrategyFactory.class);
        SupportedMutators.put("TimeAllocationMutator", TimeAllocationMutatorPlanStrategyFactory.class);
        SupportedMutators.put("TimeAllocationMutator_ReRoute", TimeAllocationMutatorReRoutePlanStrategyFactory.class);
        SupportedMutators.put("ChangeLegMode", ChangeLegModeStrategyFactory.class);
        SupportedMutators.put("ChangeSingleLegMode", ChangeSingleLegModeStrategyFactory.class);
        SupportedMutators.put("ChangeSingleTripMode", ChangeSingleTripModeStrategyFactory.class);
        SupportedMutators.put("SubtourModeChoice", org.matsim.core.replanning.modules.SubtourModeChoiceStrategyFactory.class);
        SupportedMutators.put("ChangeTripMode", ChangeTripModeStrategyFactory.class);
        SupportedMutators.put("TripSubtourModeChoice", TripSubtourModeChoiceStrategyFactory.class);

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

    public static void registerStrategiesWithControler(Controler controler, PlanCatcher slave, boolean quickReplanning, int selectionInflationFactor) {
        for (Map.Entry<String, Class<? extends PlanStrategyFactory>> e : SupportedSelectors.entrySet()) {
            controler.addPlanStrategyFactory(e.getKey() + SUFFIX,
                    new DistributedPlanSelectorStrategyFactory(slave,quickReplanning,selectionInflationFactor,controler,e.getKey()));

        }

        for (Map.Entry<String, Class<? extends PlanStrategyFactory>> e : SupportedMutators.entrySet()) {
            controler.addPlanStrategyFactory(e.getKey() + SUFFIX,
                    new DistributedPlanMutatorStrategyFactory(slave, SupportedMutatorGenes.get(e.getKey()),TrackGenome,controler,e.getKey())
                    );
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





