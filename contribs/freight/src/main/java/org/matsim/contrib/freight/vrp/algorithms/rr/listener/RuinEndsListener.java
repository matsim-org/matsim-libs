package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;
import org.matsim.core.controler.listener.ControlerListener;

public interface RuinEndsListener extends ControlerListener{

	public void informRuinEnds(RuinAndRecreateSolution solution);

}
