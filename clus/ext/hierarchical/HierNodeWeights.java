
package clus.ext.hierarchical;

import java.util.*;

public class HierNodeWeights {
	
	double[] m_Weights;
	String m_Name;
	
	public final double getWeight(int nodeidx) {
		return m_Weights[nodeidx];
	}
	
	public final double[] getWeights() {
		return m_Weights;
	}
	
	public final String getName() {
		return m_Name;
	}
	
/*
 * The key issue is assigning weights. I suggest the following procedure.
 * For the root, a weight of 1 is assigned.
 * For all other nodes Y, the weight should be
 * w * Sum( weight (X) ) where X is a parent of Y.
 * This is equivalent to flattening the lattice into a tree
 * (by copying the subtrees that have multiple parents).
 * 
 * */
	
	public boolean allParentsOk(ClassTerm term, boolean[] computed) {
		for (int j = 0; j < term.getNbParents(); j++) {
			ClassTerm parent = term.getParent(j);
			if (parent.getIndex() != -1 && !computed[parent.getIndex()]) return false;			
		}
		return true;
	}
	
	public void initExponentialDepthWeightsDAG(ClassHierarchy hier, double w0) {
		boolean[] weight_computed = new boolean[hier.getTotal()];
		ArrayList todo = new ArrayList();
		for (int i = 0; i < hier.getTotal(); i++) {
			ClassTerm term = hier.getTermAt(i);
			todo.add(term);
		}
		int nb_done = 0;
		while (nb_done < hier.getTotal()) {
			for (int i = todo.size()-1; i >= 0; i--) {
				ClassTerm term = (ClassTerm)todo.get(i);
				if (allParentsOk(term, weight_computed)) {
					
					
					weight_computed[term.getIndex()] = true;
					todo.remove(i);
					nb_done++;
				}
			}
		}		
	}
	
	public void initExponentialDepthWeightsRec(ClassTerm node, int depth, double w0) {
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClassTerm child = (ClassTerm)node.getChild(i);
			m_Weights[child.getIndex()] = calcExponentialDepthWeight(depth, w0);
			initExponentialDepthWeightsRec(child, depth+1, w0);
		}
	}
	
	public void initExponentialDepthWeights(ClassHierarchy hier, double w0) {
		m_Weights = new double[hier.getTotal()];
		ClassTerm root = hier.getRoot();
		if (hier.isTree()) {
			initExponentialDepthWeightsRec(root, 0, w0);
			m_Name = "Exponential depth weights (tree) "+w0;
		} else {
			initExponentialDepthWeightsDAG(hier, w0);
			m_Name = "Exponential depth weights (DAG) "+w0;
		}
	}
	
	private final static double calcExponentialDepthWeight(int depth, double w0) {
		return Math.pow(w0, (double)depth);
	}	
}
