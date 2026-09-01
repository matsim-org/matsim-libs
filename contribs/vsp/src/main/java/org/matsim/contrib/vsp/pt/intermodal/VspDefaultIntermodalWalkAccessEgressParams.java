package org.matsim.contrib.vsp.pt.intermodal;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

import java.util.HashSet;
import java.util.Set;

/**
 * This class sets intermodal access/egress parameters for walk. If intermodal routing is used in a policy case, the pt+walk routes are routed using
 * the intermodal raptor router. The resulting routes can differ from the raptor in the non-intermodal setup. Therefore, it is best to use the
 * intermodal raptor router with access/egress parameters for walk in the base case, too.
 * vsp-gleich july'26
 */
public class VspDefaultIntermodalWalkAccessEgressParams {
	private static final Logger log = LogManager.getLogger(VspDefaultIntermodalWalkAccessEgressParams.class);

	/*
	 * This starts from the assumption: rural area -> few agents to route and sparse pt network
	 * We can invest more computation time into calculating better routes.
	 * Since the pt network is sparse and some stops will have very infrequent pt service it makes sense to explore more stops and potentially find
	 * a stop with much better service, e.g. a more frequent or faster pt line or a bus line going closer to the destination than the stops found
	 * before.
	 *
	 * The following values are taken from the matsim-lausitz scenario. GL and SM discussed and set them.
	 * vsp-gleich july'26
	 */
	public static void setAndAddExplicitIntermodalityParamsForRuralWalkToPt(SwissRailRaptorConfigGroup srrConfig) {
		srrConfig.setUseIntermodalAccessEgress(true);
		srrConfig.setIntermodalAccessEgressModeSelection(SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection.CalcLeastCostModePerStop);

		Set<SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet> paramsToDelete = new HashSet<>();

		deleteExistingWalkIntermodalAccessEgressParams(srrConfig, paramsToDelete);

//		add walk as access egress mode to pt
		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet accessEgressWalkParam = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		accessEgressWalkParam.setMode(TransportMode.walk);
	/*
	 * Set a large initial search radius for pt stop search, so even more distant stops on different pt lines are explored no matter how many stops
	 * are located in the vicinity. The searchExtensionRadius is larger than in the urban case, because the pt network is sparser.
	 * InitialSearchRadius gets extended by searchExtensionRadius until maxRadius is reached.
	 */
		accessEgressWalkParam.setInitialSearchRadius(10000);
		accessEgressWalkParam.setMaxRadius(100000);
		accessEgressWalkParam.setSearchExtensionRadius(1000);
		srrConfig.addIntermodalAccessEgress(accessEgressWalkParam);
	}

	/*
	 * This starts from the assumption: urban area -> many agents to route and dense pt network
	 * We have to save computation time.
	 * Since the pt network is dense and more frequent, boarding at a stop close to the trip origin is likely to be better than walking to more
	 * distant pt stops. While the latter might have somewhat better service, it is probably not worth the additional walk time.
	 *
	 * InitialSearchRadius and searchExtensionRadius are taken from the defaults in TransitRouterConfigGroup and RaptorParameters.
	 * vsp-gleich july'26
	 */
	public static void setAndAddExplicitIntermodalityParamsForUrbanWalkToPt(SwissRailRaptorConfigGroup srrConfig) {
		srrConfig.setUseIntermodalAccessEgress(true);
		srrConfig.setIntermodalAccessEgressModeSelection(SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection.CalcLeastCostModePerStop);

		Set<SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet> paramsToDelete = new HashSet<>();

		deleteExistingWalkIntermodalAccessEgressParams(srrConfig, paramsToDelete);

//		add walk as access egress mode to pt
		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet accessEgressWalkParam = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		accessEgressWalkParam.setMode(TransportMode.walk);
		accessEgressWalkParam.setInitialSearchRadius(1000);
		// Default behaviour of the non-intermodal raptor was not to have any max radius.
		// We do not actually want to limit the search radius, so set a high value.
		accessEgressWalkParam.setMaxRadius(100000);
		accessEgressWalkParam.setSearchExtensionRadius(200);
		srrConfig.addIntermodalAccessEgress(accessEgressWalkParam);
	}

	private static void deleteExistingWalkIntermodalAccessEgressParams(SwissRailRaptorConfigGroup srrConfig, Set<SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet> paramsToDelete) {
		for (SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet p : srrConfig.getIntermodalAccessEgressParameterSets()) {
			if (p.getMode().equals(TransportMode.walk)) {
				log.warn("Found existing intermodal raptor access egress params for {} with initialSearchRadius={}, maxRadius={} and searchExtensionRadius={}." +
					"Will delete them and replace by own implementation of rural intermodal walk access egress params.", TransportMode.walk, p.getInitialSearchRadius(), p.getMaxRadius(), p.getSearchExtensionRadius());
				paramsToDelete.add(p);
			}
		}

		for (SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet p : paramsToDelete) {
			srrConfig.removeParameterSet(p);
		}
	}

}
