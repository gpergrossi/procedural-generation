package com.gpergrossi.util.data.btree;

public interface BinaryTree<T extends BinaryNode<T>> {

	public boolean isRoot(T node);	
	public void setRoot(T node);
	public T getRoot();
	
}
