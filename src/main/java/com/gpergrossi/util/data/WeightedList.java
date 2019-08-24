package com.gpergrossi.util.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.gpergrossi.util.data.btree.AbstractBinaryNode;
import com.gpergrossi.util.data.btree.BinaryNode;
import com.gpergrossi.util.data.btree.BinaryTree;

public class WeightedList<T> {

	private WeightedTreeNode root;
	private Map<T, WeightedTreeNode> nodeMap;
	
	public WeightedList() {
		this.root = null;
		this.nodeMap = new HashMap<>();
	}
	
	public int size() {
		return nodeMap.size();
	}
	
	public boolean isEmpty() {
		return nodeMap.isEmpty();
	}
	
	public void add(T item, int weight) {
		if (weight <= 0) throw new IllegalArgumentException("weight must be greater than 0!");
		
		WeightedTreeNode node = nodeMap.get(item);
		if (node != null) {
			node.setWeight(node.weight+weight);
			return;
		}
		
		WeightedTreeNode newNode = new WeightedTreeNode(item, weight);
		if (this.root == null) {
			this.root = newNode;
		} else {
			this.root.insert(newNode);
		}
		nodeMap.put(item, newNode);
	}
	
	public void remove(T item) {
		WeightedTreeNode node = nodeMap.get(item);
		if (node == null) return;
		removeNode(node);
	}
	
	private void removeNode(WeightedTreeNode node) {
		T item = node.item;
		nodeMap.remove(item);
		node.remove();
	}
	
	public void clear() {
		nodeMap.clear();
		root = null;
	}
	
	public int getWeight(T item) {
		WeightedTreeNode node = nodeMap.get(item);
		if (node == null) return 0;
		return node.weight;
	}

	public int getTotalWeight() {
		if (root == null) return 0;
		return root.getSubtreeWeight();
	}
	
	public T getRandom(Random random) {
		WeightedTreeNode node = getRandomNode(random);
		if (node == null) return null;
		return node.item;
	}
	
	public Optional<WeightedItem> removeRandom(Random random) {
		WeightedTreeNode node = getRandomNode(random);
		if (node == null) return Optional.empty();
		WeightedItem result = new WeightedItem(node.item, node.weight);
		node.removeFromParent();
		nodeMap.remove(node.item);
		return Optional.of(result);
	}
	
	public void debugPrintTree() {
		System.out.println(root.treeString());
	}
	
	private WeightedTreeNode getRandomNode(Random random) {
		if (root == null) return null;
		int roll = random.nextInt(root.getSubtreeWeight());
		return root.getByWeight(roll);
	}
	
	public class WeightedItem {
		private final T item;
		private final int weight;
		
		public WeightedItem(T item, int weight) {
			super();
			this.item = item;
			this.weight = weight;
		}

		public T getItem() {
			return item;
		}

		public int getWeight() {
			return weight;
		}		
	}
	
	private class WeightedTreeNode extends AbstractBinaryNode<WeightedTreeNode> {
		private T item;
		private int weight;
		
		private boolean needsUpdate;
		private int subtreeWeight;
		
		private WeightedTreeNode(T item, int weight) {
			this.item = item;
			this.setWeight(weight);
		}

		private int getSubtreeWeight() {
			if (this.needsUpdate) {
				this.subtreeWeight = this.weight;
				if (this.hasLeftChild()) this.subtreeWeight += this.getLeftChild().getSubtreeWeight();
				if (this.hasRightChild()) this.subtreeWeight += this.getRightChild().getSubtreeWeight();
				this.needsUpdate = false;
			}
			return this.subtreeWeight;
		}

		private void recalculateWeight() {
			if (this.needsUpdate) return;
			this.needsUpdate = true;
			if (this.hasParent()) this.getParent().recalculateWeight();
		}
		
		private void setWeight(int weight) {
			this.weight = weight;
			this.recalculateWeight();
		}

		@Override
		public void replaceWith(WeightedTreeNode child) {
			super.replaceWith(child);
			if (this == root) root = child;
		}
		
		@Override
		public void removeFromParent() {
			WeightedTreeNode parent = getParent();
			super.removeFromParent();
			if (this == root) root = null;
			if (parent != null) parent.recalculateWeight();
		}
		
		@Override
		public void setLeftChild(WeightedTreeNode child) throws IllegalStateException {
			super.setLeftChild(child);
			this.recalculateWeight();
		}
		
		@Override
		public void setRightChild(WeightedTreeNode child) throws IllegalStateException {
			super.setRightChild(child);
			this.recalculateWeight();
		}

		private void insert(WeightedTreeNode node) {
			WeightedTreeNode leftChild = this.getLeftChild();
			WeightedTreeNode rightChild = this.getRightChild();
			
			if (leftChild == null) this.setLeftChild(node);
			else if (rightChild == null) this.setRightChild(node);
			else if (leftChild.getSubtreeWeight() < rightChild.getSubtreeWeight()) leftChild.insert(node);
			else rightChild.insert(node);
		}
		
		private void remove() {			
			WeightedTreeNode leftChild = this.getLeftChild();
			WeightedTreeNode rightChild = this.getRightChild();
			
			if (leftChild == null) {
				if (rightChild == null) {
					this.removeFromParent();
					return;
				} else {
					rightChild.removeFromParent();
					this.replaceWith(rightChild);
				}
			} else {
				if (rightChild == null) {
					leftChild.removeFromParent();
					this.replaceWith(leftChild);
				} else {
					// Two children: replace with successor/predecessor
					WeightedTreeNode toRemove = null;
					if (leftChild.getSubtreeWeight() < leftChild.getSubtreeWeight()) {
						toRemove = this.getSuccessor();
					} else {
						toRemove = this.getPredecessor();
					}
					BinaryNode.swapNodes(this, toRemove);
					this.removeFromParent();
				}
			}
		}
		
		private WeightedTreeNode getByWeight(int weight) {
			WeightedTreeNode leftChild = this.getLeftChild();
			WeightedTreeNode rightChild = this.getRightChild();
			
			if (leftChild != null) {
				if (weight <= leftChild.getSubtreeWeight()) {
					return leftChild.getByWeight(weight);
				} else {
					weight -= leftChild.getSubtreeWeight();
				}
			}
			
			if (weight <= this.weight) return this;
			else weight -= this.weight;
			
			if (rightChild != null && weight <= rightChild.getSubtreeWeight()) {
				return rightChild.getByWeight(weight);
			}
			return null;
		}
		
		@Override
		public String toString() {
			return item+" ("+weight+")"+" ["+getSubtreeWeight()+"]";
		}

		@Override
		public BinaryTree<WeightedList<T>.WeightedTreeNode> getTree() {
			return null;
		}
	}
	
}
