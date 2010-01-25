/* *********************************************************************** *
 * project: org.matsim.*
 * DgScenario
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether.scenario;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;


/**
 * @author dgrether
 *
 */
public class DgScenario extends ScenarioImpl {

  /**
   * 
   */
  public DgScenario() {
    super();
  }

  /**
   * @param config
   */
  public DgScenario(Config config) {
    super(config);
  }

  
  private Map<Class, Object> elements = new HashMap<Class, Object>();
  
  //this is a bit dangerous due to overwritten elements if the same interface is implemented
  //thus a warning should be given in case of overwritten stuff
  public void addScenarioElement(Object o){
    this.elements.put(o.getClass(), o);
    for (Class i : o.getClass().getInterfaces()){
      this.elements.put(i, o);
    }
  }
  
  public <T> T getScenarioElement(Class<? extends T> klas){
    return (T) this.elements.get(klas);
  }
  
  public static void main(String[] args){
    DgScenario sc = new DgScenario();
    sc.addScenarioElement(new KnowledgesImpl());    
    Knowledges k = sc.getScenarioElement(Knowledges.class);
  }
  
}
