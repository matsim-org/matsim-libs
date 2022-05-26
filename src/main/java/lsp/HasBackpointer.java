package lsp;

public interface HasBackpointer<T> {

	// yy maybe also have interface HasSettableBackpointer?
	default void setEmbeddingContainer(T pointer) {};

	T getEmbeddingContainer();

}
