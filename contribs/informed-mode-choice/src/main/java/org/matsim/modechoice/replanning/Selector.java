package org.matsim.modechoice.replanning;

import java.util.Collection;
import javax.annotation.Nullable;

interface Selector<T> {

  /** Select one entity from a list of candidates. Return null when not applicable. */
  @Nullable
  T select(Collection<T> candidates);
}
