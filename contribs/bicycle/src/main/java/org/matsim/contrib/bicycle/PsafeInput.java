package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

public final class PsafeInput {

	static double computePsafeScore(Link link,String mode, int thre) {
		int varpsafe = 4;
		if (mode.equals("car")) {
			varpsafe = (int)link.getAttributes().getAttribute(PsafeNewAttrib.PERCEIVED_SAFETY_CAR);}
		if (mode.equals("ebike")) {
			varpsafe = (int)link.getAttributes().getAttribute(PsafeNewAttrib.PERCEIVED_SAFETY_EBIKE);}
		if (mode.equals("escoot")) {
			varpsafe = (int)link.getAttributes().getAttribute(PsafeNewAttrib.PERCEIVED_SAFETY_ESCOOT);}
		if (mode.equals("walk")) {
			varpsafe = (int)link.getAttributes().getAttribute(PsafeNewAttrib.PERCEIVED_SAFETY_WALK);}
		double score = varpsafe - thre;
		return(score);
	}
}