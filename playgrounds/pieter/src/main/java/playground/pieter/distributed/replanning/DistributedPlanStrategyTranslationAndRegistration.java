package playground.pieter.distributed.replanning;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.modules.*;
import playground.pieter.distributed.replanning.factories.DistributedPlanMutatorStrategyFactory;
import playground.pieter.distributed.replanning.factories.DistributedPlanSelectorStrategyFactory;
import playground.pieter.distributed.replanning.factories.TransitLocationChoiceFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fouriep
 *         <p/>
 *         Essentially a lookup used in config translation. Also registers
 *         extended strategies with the controler.
 *         <p/>
 *         If a mutating strategy is sent for pseudo-simulation, it needs to be
 *         marked as such, and registered with the {@link playground.pieter.pseudosimulation.controler.PSimControler}.
 *         Non-mutating strategies, e.g. selector strategies, should be disabled
 *         during PSim iterations, and only run during QSim iterations.
 *         <p/>
 *         <p/>
 *         This class records strategies that should work with PSim. It extends
 *         their factories by appending a {@link playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule} at the
 *         end of each strategy. Each factory is registered during controler
 *         construction, and the config entries are changed to refer to their
 *         PSim equivalents in the controler's substituteStrategies() method.
 *         <p/>
 *         <p/>
 *         Each strategy name is taken from the enum in the
 *         {@link org.matsim.core.replanning.PlanStrategyRegistrar} to ensure future consistency
 *         <p/>
 *         <p/>
 *         <B>NOTE:</B> to save processing overhead, selector strategies are set
 *         up to always return the person's current selected plan during
 *         non-QSim iterations.
 */
public class DistributedPlanStrategyTranslationAndRegistration {
    public static Map<String, Class<? extends PlanStrategyFactory>> SupportedSelectors = new HashMap<>();

    public static Map<String, Class<? extends PlanStrategyFactory>> SupportedMutators = new HashMap<>();

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
        SupportedMutators.put("SubtourModeChoice", SubtourModeChoiceStrategyFactory.class);
        SupportedMutators.put("ChangeTripMode", ChangeTripModeStrategyFactory.class);
        SupportedMutators.put("TripSubtourModeChoice", TripSubtourModeChoiceStrategyFactory.class);
        SupportedMutators.put("TransitLocationChoice", TransitLocationChoiceFactory.class);

    }

    public static final String SUFFIX = "PSIM";

    public DistributedPlanStrategyTranslationAndRegistration(Controler  controler, PlanCatcher slave, boolean quickReplanning, int selectionInflationFactor) {

        for (Map.Entry<String, Class<? extends PlanStrategyFactory>> e : SupportedSelectors.entrySet()) {
            try {
                controler.addPlanStrategyFactory(e.getKey() + SUFFIX,
                        new DistributedPlanSelectorStrategyFactory<>( slave,
                                (PlanStrategyFactory) e.getValue().getConstructors()[0].newInstance(), quickReplanning, selectionInflationFactor));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }

        for (Map.Entry<String, Class<? extends PlanStrategyFactory>> e : SupportedMutators.entrySet()) {
            if(e.getKey().equals( "TransitLocationChoice")){
                TransitLocationChoiceFactory factory = new TransitLocationChoiceFactory(slave);
                controler.addPlanStrategyFactory("TransitLocationChoicePSIM", factory);
                continue;
            }
            try {
                controler.addPlanStrategyFactory(e.getKey() + SUFFIX,
                        new DistributedPlanMutatorStrategyFactory<>(slave,
                                (PlanStrategyFactory) e.getValue().getConstructors()[0].newInstance()));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }


    }

    public static boolean isStrategySupported(String name) {
        if (SupportedSelectors.size() == 0)
            initMaps();
        if (SupportedSelectors.containsKey(name))
            return true;
        else if (SupportedMutators.containsKey(name))
            return true;
        else return false;
    }

    public static void substituteSelectorStrategies(Config config, boolean quickReplanning, int selectionInflationFactor) {
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
                if (SupportedSelectors.containsKey(classname)){
                    settings.setStrategyName(classname + DistributedPlanStrategyTranslationAndRegistration.SUFFIX);
                    //implement quick replanning by simply multiplying the selector weights by the number of PSim Iterations
                    if(quickReplanning){
                        settings.setWeight(settings.getWeight() * (double) selectionInflationFactor);
                    }
                }
            }


        }

    }
}





