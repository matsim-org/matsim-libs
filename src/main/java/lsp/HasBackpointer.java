package lsp;

public interface HasBackpointer<T> {
	void setEmbeddingContainer(T pointer);
	T getEmbeddingContainer() ;
}
