package com.gpergrossi.util.data.btree;

import java.util.Iterator;

/**
 * The AbstractBinaryNode class provides all features you need for a binary node and probably more.
 * The idea is to extend this class with a child class to attach data to each node.
 *
 * @param <T> the type of the child class, so that all nodes returned are of the right type.
 */
public abstract class AbstractBinaryNode<T extends AbstractBinaryNode<T>> implements BinaryNode<T>, Iterable<T> {
		
	
	T parent;
	T leftChild, rightChild;
	
	@Override
	public T getParent() {
		return parent;
	}
	
	@Override
	public void removeFromParent() {
		if (!this.hasParent()) return;
		if (this.isLeftChild()) {
			parent.leftChild = null;
			this.parent = null;
		} else if (this.isRightChild()) {
			parent.rightChild = null;
			this.parent = null;
		} else throw new IllegalStateException("Parent node does not reference this node as a child");
	}
	
	@Override
	public T getLeftChild() {
		return leftChild;
	}
	
	@Override
	public void setLeftChild(T child) throws IllegalStateException {
		if (this.hasLeftChild()) this.getLeftChild().removeFromParent();
		if (child == null) return;
		
		assertChildAllowed(child);

		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		this.leftChild = child;
		child.parent = self;
	}
	
	@Override
	public T getRightChild() {
		return rightChild;
	}
	
	@Override
	public void setRightChild(T child) throws IllegalStateException {		
		if (this.hasRightChild()) this.getRightChild().removeFromParent();
		if (child == null) return;
		
		assertChildAllowed(child);

		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		this.rightChild = child;
		child.parent = self;
	}
	
	
	
	
	
	
	/**
	 * In order to avoid cycles in the tree, the following conditions must be met by children to be attached to a node: <br />
	 * 1. The child to be attached must be a root node (have no parent). <br />
	 * 2. The child to be attached must not be an ancestor of the node to which it will be attached (this would cause a cycle). <br />
	 * @param child - the child to be attached to this node
	 * @throws IllegalStateException if either of the above conditions is not met.
	 */
	protected void assertChildAllowed(T child) {
		if (child.hasParent()) throw new IllegalStateException("Argument node must be a root node! (I.E. No parent)");
		if (this.hasAncestor(child)) throw new IllegalStateException("Argument node must not be an ancestor of this node.");
	}
	
	/**
	 * Returns the tree as a formatted string. Hard to read, but could be useful anyway.
	 * @return
	 */
	public String treeString() {
		StringBuilder sb = new StringBuilder();

		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		treeString(sb, self, "", "root");
		return sb.toString();
	}

	private void treeString(StringBuilder sb, T node, String indent, String name) {
		if (node == null) return;
		treeString(sb, node.getLeftChild(), indent + "      ", "left");
		sb.append(indent).append(name).append(": ").append(node.toString()).append('\n');
		treeString(sb, node.getRightChild(), indent + "      ", "right");
	}
	
	@Override
	public Iterator<T> iterator() {
		@SuppressWarnings("unchecked")
		T self = (T) this;
		return new TreeNodeIterator<T>(self);
	}
	
}
