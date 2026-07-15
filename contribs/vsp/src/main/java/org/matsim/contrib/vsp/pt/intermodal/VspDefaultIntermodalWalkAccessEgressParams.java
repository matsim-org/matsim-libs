package org.matsim.contrib.vsp.pt.intermodal;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

import java.util.HashSet;
import java.util.Set;

public class VspDefaultIntermodalWalkAccessEgressParams {
	private static final Logger log = LogManager.getLogger(VspDefaultIntermodalWalkAccessEgressParams.class);

	public static void setAndAddExplicitIntermodalityParamsForRuralWalkToPt(SwissRailRaptorConfigGroup srrConfig) {
		srrConfig.setUseIntermodalAccessEgress(true);
		srrConfig.setIntermodalAccessEgressModeSelection(SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection.CalcLeastCostModePerStop);

		Set<SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet> paramsToDelete = new HashSet<>();

		deleteExistingWalkIntermodalAccessEgressParams(srrConfig, paramsToDelete);

//		add walk as access egress mode to pt
		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet accessEgressWalkParam = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		accessEgressWalkParam.setMode(TransportMode.walk);
//			initial radius for pt stop search
		accessEgressWalkParam.setInitialSearchRadius(10000);
		accessEgressWalkParam.setMaxRadius(100000);
//			with this, initialSearchRadius gets extended by the set value until maxRadius is reached
		accessEgressWalkParam.setSearchExtensionRadius(1000);
		srrConfig.addIntermodalAccessEgress(accessEgressWalkParam);
	}

	public static void setAndAddExplicitIntermodalityParamsForUrbanWalkToPt(SwissRailRaptorConfigGroup srrConfig) {
		srrConfig.setUseIntermodalAccessEgress(true);
		srrConfig.setIntermodalAccessEgressModeSelection(SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection.CalcLeastCostModePerStop);

		Set<SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet> paramsToDelete = new HashSet<>();

		deleteExistingWalkIntermodalAccessEgressParams(srrConfig, paramsToDelete);

//		add walk as access egress mode to pt
		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet accessEgressWalkParam = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		accessEgressWalkParam.setMode(TransportMode.walk);
//			initial radius for pt stop search
//		this is the default value from class RaptorParameters, which we assume to be valid for urban scenarios
		accessEgressWalkParam.setInitialSearchRadius(1000);
//		sth high. might be better to set it even higher?
		accessEgressWalkParam.setMaxRadius(10000);
//			with this, initialSearchRadius gets extended by the set value until maxRadius is reached
//		this is the default value from class RaptorParameters, which we assume to be valid for urban scenarios
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
