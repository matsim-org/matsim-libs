package org.matsim.freight.logistics;

@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface HasBackpointer<T> {
  // In general, we set backpointers when we add to the container.

  // yy maybe also have interface HasSettableBackpointer?
  void setEmbeddingContainer(T pointer);

  //	T getEmbeddingContainer();

}
