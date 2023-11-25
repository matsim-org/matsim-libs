package org.matsim.core.utils.timing;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

public class TimeInterpretationModule extends AbstractModule {
  @Override
  public void install() {}

  @Provides
  @Singleton
  public TimeInterpretation provideTimeInterpretation() {
    return TimeInterpretation.create(getConfig());
  }
}
