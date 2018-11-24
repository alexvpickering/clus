/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.algo.tdidt;

import jeans.tree.*;
import jeans.util.*;
import jeans.util.compound.*;

import java.util.*;
import java.io.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import clus.util.*;
import clus.main.ClusRun;
import clus.main.ClusStatManager;
import clus.main.Global;
import clus.main.Settings;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.model.processor.ClusModelProcessor;
import clus.model.test.*;
import clus.statistic.*;
import clus.algo.split.CurrentBestTestAndHeuristic;
import clus.data.rows.*;
import clus.data.type.ClusAttrType;
import clus.data.attweights.*;
import clus.error.multiscore.*;
import clus.selection.OOBSelection;

public class ClusNode extends MyNode implements ClusModel {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public final static int YES = 0;
	public final static int NO = 1;
	public final static int UNK = 2;

	public int m_ID;
	public NodeTest m_Test;
	public ClusStatistic m_ClusteringStat;
	public ClusStatistic m_TargetStat;
	public transient Object m_Visitor;
	public long m_Time;
	public String[] m_Alternatives;

	public MyNode cloneNode() {
		ClusNode clone = new ClusNode();
		clone.m_Test = m_Test;
		clone.m_ClusteringStat = m_ClusteringStat;
		clone.m_TargetStat = m_TargetStat;
		clone.m_Alternatives = m_Alternatives;
		return clone;
	}

	public ClusNode cloneNodeWithVisitor() {
		ClusNode clone = (ClusNode)cloneNode();
		clone.setVisitor(getVisitor());
		return clone;
	}

	public final ClusNode cloneTreeWithVisitors(ClusNode n1, ClusNode n2) {
		if (n1 == this) {
			return n2;
		} else {
			ClusNode clone = (ClusNode)cloneNode();
			clone.setVisitor(getVisitor());
			int arity = getNbChildren();
			clone.setNbChildren(arity);
			for (int i = 0; i < arity; i++) {
				ClusNode node = (ClusNode)getChild(i);
				clone.setChild(node.cloneTreeWithVisitors(n1, n2), i);
			}
			return clone;
		}
	}

	public final ClusNode cloneTreeWithVisitors() {
		ClusNode clone = (ClusNode)cloneNode();
		clone.setVisitor(getVisitor());
		int arity = getNbChildren();
		clone.setNbChildren(arity);
		for (int i = 0; i < arity; i++) {
			ClusNode node = (ClusNode)getChild(i);
			clone.setChild(node.cloneTreeWithVisitors(), i);
		}
		return clone;
	}


	public void inverseTests() {
		if (getNbChildren() == 2) {
			setTest(getTest().getBranchTest(ClusNode.NO));
			ClusNode ch1 = (ClusNode)getChild(0);
			ClusNode ch2 = (ClusNode)getChild(1);
			ch1.inverseTests();
			ch2.inverseTests();
			setChild(ch2, 0);
			setChild(ch1, 1);
		} else {
			for (int i = 0; i < getNbChildren(); i++) {
				ClusNode node = (ClusNode)getChild(i);
				node.inverseTests();
			}
		}
	}

	public ClusNode[] getChildren(){
		ClusNode[] temp = new ClusNode[m_Children.size()];
		for(int i=0; i<m_Children.size(); i++)
			temp[i] = (ClusNode)getChild(i);
		return temp;
	}

	public double checkTotalWeight() {
		if (atBottomLevel()) {
			return getClusteringStat().getTotalWeight();
		} else {
			double sum = 0.0;
			for (int i = 0; i < getNbChildren(); i++) {
				ClusNode child = (ClusNode)getChild(i);
				sum += child.checkTotalWeight();
			}
			if (Math.abs(getClusteringStat().getTotalWeight() - sum) > 1e-6) {
				System.err.println("ClusNode::checkTotalWeight() error: "+getClusteringStat().getTotalWeight()+" <> "+sum);
			}
			return sum;
		}
	}

	public final void setVisitor(Object visitor) {
		m_Visitor = visitor;
	}

	public final Object getVisitor() {
		return m_Visitor;
	}

	public final void clearVisitors() {
		m_Visitor = null;
		int arity = getNbChildren();
		for (int i = 0; i < arity; i++) {
			ClusNode child = (ClusNode)getChild(i);
			child.clearVisitors();
		}
	}

	public final int getID() {
		return m_ID;
	}


	public boolean equals(Object other) {
		ClusNode o = (ClusNode)other;
		if (m_Test != null && o.m_Test != null) {
			if (!m_Test.equals(o.m_Test)) return false;
		} else {
			if (m_Test != null || o.m_Test != null) return false;
		}
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			if (!getChild(i).equals(o.getChild(i))) return false;
		}
		return true;
	}

	public int hashCode() {
		int hashCode = 1234;
		if (m_Test != null) {
			hashCode += m_Test.hashCode();
		} else {
			hashCode += 4567;
		}
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			hashCode += getChild(i).hashCode();
		}
		return hashCode;
	}

	/***************************************************************************
	 * Insprectors concenring test
	 ***************************************************************************/

	public final boolean hasBestTest() {
		return m_Test != null;
	}

	public final NodeTest getTest() {
		return m_Test;
	}

	public final void setTest(NodeTest test) {
		m_Test = test;
	}

	public final String getTestString() {
		return m_Test != null ? m_Test.getString() : "None";
	}

	public final void testToNode(CurrentBestTestAndHeuristic best) {
		setTest(best.updateTest());
	}

	public int getModelSize() {
		return getNbNodes();
	}

	public String getModelInfo() {
		return "Nodes = "+getNbNodes()+" (Leaves: "+getNbLeaves()+")";
	}

	public final boolean hasUnknownBranch() {
		return m_Test.hasUnknownBranch();
	}

	public String[] getAlternatives() {
		return m_Alternatives;
	}

	/***************************************************************************
	 * Insprectors concenring statistics
	 ***************************************************************************/

	public final ClusStatistic getClusteringStat() {
		return m_ClusteringStat;
	}

	public final ClusStatistic getTargetStat() {
		return m_TargetStat;
	}

	public final double getTotWeight() {
		return m_ClusteringStat.m_SumWeight;
	}

	// Weight of unknown examples over total weight
	public final double getUnknownFreq() {
		return m_Test.getUnknownFreq();
	}

	/***************************************************************************
	 * Mutators
	 ***************************************************************************/

	public final void setClusteringStat(ClusStatistic stat) {
		m_ClusteringStat = stat;
	}

	public final void setTargetStat(ClusStatistic stat) {
		m_TargetStat = stat;
	}

	public final void computePrediction() {
		if (getClusteringStat() != null) getClusteringStat().calcMean();
		if (getTargetStat() != null) getTargetStat().calcMean();
	}

	public final int updateArity() {
		int arity = m_Test.updateArity();
		setNbChildren(arity);
		return arity;
	}

	public final ClusNode postProc(MultiScore score) {
		updateTree();
		safePrune();
		return this;
	}

	public final void cleanup() {
		if (m_ClusteringStat != null) m_ClusteringStat.setSDataSize(0);
		if (m_TargetStat != null) m_TargetStat.setSDataSize(0);
	}

	public void makeLeaf() {
		m_Test = null;
		cleanup();
		removeAllChildren();
	}

	public final void updateTree() {
		cleanup();
		computePrediction();
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.updateTree();
		}
	}

	public void setAlternatives(ArrayList alt) {
		m_Alternatives = new String[alt.size()];
		for (int i=0; i<alt.size(); i++) {
			m_Alternatives[i] = alt.get(i).toString();
		}
	}

	/***************************************************************************
	 * Code for safe package clus.pruning the tree
	 ***************************************************************************/

	// Test if two nodes predict the same
	public final boolean samePrediction(ClusNode other) {
		return m_TargetStat.samePrediction(other.m_TargetStat);
	}

	// Test if all children are leaves that predict the same
	public final boolean allSameLeaves() {
		int nb_c = getNbChildren();
		if (nb_c == 0) return false;
		ClusNode cr = (ClusNode)getChild(0);
		if (!cr.atBottomLevel()) return false;
		for (int i = 1; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			if (!info.atBottomLevel()) return false;
			if (!info.samePrediction(cr)) return false;
		}
		return true;
	}

	public void pruneByTrainErr(ClusAttributeWeights scale) {
		if (!atBottomLevel()){
			double errorsOfSubtree = estimateErrorAbsolute(scale);
			double errorsOfLeaf = getTargetStat().getError(scale);
			if (errorsOfSubtree >= errorsOfLeaf-1E-3) {
				makeLeaf();
			} else {
				for (int i = 0; i < getNbChildren(); i++) {
					ClusNode child = (ClusNode)getChild(i);
					child.pruneByTrainErr(scale);
			    }
			}
		}
	}

	// Safe prune this tree (using predictions in leaves)
	public final void safePrune() {
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.safePrune();
		}
		if (allSameLeaves()) makeLeaf();
	}

	public final boolean allInvalidLeaves() {
		int nb_c = getNbChildren();
		if (nb_c == 0) return false;
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			if (!info.atBottomLevel()) return false;
			if (info.getTargetStat().isValidPrediction()) return false;
		}
		return true;
	}

	public final void pruneInvalid() {
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.pruneInvalid();
		}
		if (allInvalidLeaves()) makeLeaf();
	}

    public ClusModel prune(int prunetype) {
    	if (prunetype == PRUNE_INVALID) {
    		ClusNode pruned = (ClusNode)cloneTree();
    		pruned.pruneInvalid();
    		return pruned;
    	}
		return this;
	}

	/***************************************************************************
	 * Multi score code - this should be made more general!
	 ***************************************************************************/

	public final void multiScore(MultiScore score) {
		m_ClusteringStat = new MultiScoreStat(m_ClusteringStat, score);
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.multiScore(score);
		}
	}

	/***************************************************************************
	 * Code to attach another dataset to the tree
	 ***************************************************************************/

	public final void attachModel(HashMap table) throws ClusException {
		int nb_c = getNbChildren();
		if (nb_c > 0) m_Test.attachModel(table);
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.attachModel(table);
		}
	}

	/***************************************************************************
	 * Code for making predictions
	 ***************************************************************************/

	public ClusStatistic predictWeighted(DataTuple tuple) {
		if (atBottomLevel()) {
			return getTargetStat();
		} else {
			int n_idx = m_Test.predictWeighted(tuple);
			if (n_idx != -1) {
				ClusNode info = (ClusNode)getChild(n_idx);
				return info.predictWeighted(tuple);
			} else {
				int nb_c = getNbChildren();
				ClusNode ch_0 = (ClusNode)getChild(0);
				ClusStatistic ch_0s = ch_0.predictWeighted(tuple);
				ClusStatistic stat = ch_0s.cloneSimple();
				stat.addPrediction(ch_0s, m_Test.getProportion(0));
				for (int i = 1; i < nb_c; i++) {
					ClusNode ch_i = (ClusNode)getChild(i);
					ClusStatistic ch_is = ch_i.predictWeighted(tuple);
					stat.addPrediction(ch_is, m_Test.getProportion(i));
				}
				stat.computePrediction();
				return stat;
			}
		}
	}

	public ClusStatistic clusterWeighted(DataTuple tuple) {
		if (atBottomLevel()) {
			return getClusteringStat();
		} else {
			int n_idx = m_Test.predictWeighted(tuple);
			if (n_idx != -1) {
				ClusNode info = (ClusNode)getChild(n_idx);
				return info.clusterWeighted(tuple);
			} else {
				int nb_c = getNbChildren();
				ClusStatistic stat = getClusteringStat().cloneSimple();
				for (int i = 0; i < nb_c; i++) {
					ClusNode node = (ClusNode)getChild(i);
					ClusStatistic nodes = node.clusterWeighted(tuple);
					stat.addPrediction(nodes, m_Test.getProportion(i));
				}
				stat.computePrediction();
				return stat;
			}
		}
	}

	public final void applyModelProcessor(DataTuple tuple, ClusModelProcessor proc) throws IOException {
		int nb_c = getNbChildren();
		if (nb_c == 0 || proc.needsInternalNodes()) proc.modelUpdate(tuple, this);
		if (nb_c != 0) {
			int n_idx = m_Test.predictWeighted(tuple);
			if (n_idx != -1) {
				ClusNode info = (ClusNode)getChild(n_idx);
				info.applyModelProcessor(tuple, proc);
			} else {
				for (int i = 0; i < nb_c; i++) {
					ClusNode node = (ClusNode)getChild(i);
					double prop = m_Test.getProportion(i);
					node.applyModelProcessor(tuple.multiplyWeight(prop), proc);
				}
			}
		}
	}

	public final void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
		int nb_c = getNbChildren();
		for (int i = 0; i < mproc.size(); i++) {
			ClusModelProcessor proc = (ClusModelProcessor)mproc.elementAt(i);
			if (nb_c == 0 || proc.needsInternalNodes()) proc.modelUpdate(tuple, this);
		}
		if (nb_c != 0) {
			int n_idx = m_Test.predictWeighted(tuple);
			if (n_idx != -1) {
				ClusNode info = (ClusNode)getChild(n_idx);
				info.applyModelProcessors(tuple, mproc);
			} else {
				for (int i = 0; i < nb_c; i++) {
					ClusNode node = (ClusNode)getChild(i);
					double prop = m_Test.getProportion(i);
					node.applyModelProcessors(tuple.multiplyWeight(prop), mproc);
				}
			}
		}
	}

	/***************************************************************************
	 * Change the total statistic of the tree?
	 ***************************************************************************/

	public final void initTargetStat(ClusStatManager smgr, RowData subset) {
		m_TargetStat = smgr.createTargetStat();
		subset.calcTotalStatBitVector(m_TargetStat);
	}

	public final void initClusteringStat(ClusStatManager smgr, RowData subset) {
		m_ClusteringStat = smgr.createClusteringStat();
		subset.calcTotalStatBitVector(m_ClusteringStat);
	}
	
	public final void initTargetStat(ClusStatManager smgr, ClusStatistic train, RowData subset) {
		m_TargetStat = smgr.createTargetStat();
		m_TargetStat.setTrainingStat(train);
		subset.calcTotalStatBitVector(m_TargetStat);
	}
	
	public final void initClusteringStat(ClusStatManager smgr, ClusStatistic train, RowData subset) {
		m_ClusteringStat = smgr.createClusteringStat();
		m_ClusteringStat.setTrainingStat(train);
		subset.calcTotalStatBitVector(m_ClusteringStat);
	}

	public final void reInitTargetStat(RowData subset) {
		if (m_TargetStat != null) {
			m_TargetStat.reset();
			subset.calcTotalStatBitVector(m_TargetStat);
		}
	}

	public final void reInitClusteringStat(RowData subset) {
		if (m_ClusteringStat != null) {
			m_ClusteringStat.reset();
			subset.calcTotalStatBitVector(m_ClusteringStat);
		}
	}

	public final void initTotStats(ClusStatistic stat) {
		m_ClusteringStat = stat.cloneStat();
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode node = (ClusNode)getChild(i);
			node.initTotStats(stat);
		}
	}

	public final void numberTree() {
		numberTree(new IntObject(1,null));
	}

	public final void numberTree(IntObject count) {
		int arity = getNbChildren();
		if (arity > 0) {
			m_ID = 0;
			for (int i = 0; i < arity; i++) {
				ClusNode child = (ClusNode)getChild(i);
				child.numberTree(count);
			}
		} else {
			m_ID = count.getValue();
			count.incValue();
		}
	}

	public final void numberCompleteTree() {
		numberCompleteTree(new IntObject(1,null));
	}
	
	public final void numberCompleteTree(IntObject count) {
		m_ID = count.getValue();
		count.incValue();
		int arity = getNbChildren();
		for (int i = 0; i < arity; i++) {
			ClusNode child = (ClusNode)getChild(i);
			child.numberCompleteTree(count);
		}
	}
	
	/*
	 * Returns the total number of nodes (incl leaves) in the tree rooted at this node
	 */
	public final int getTotalTreeSize() {
		int childrensize = 0;
		int arity = getNbChildren();
		for (int i = 0; i < arity; i++) {
			ClusNode child = (ClusNode)getChild(i);
			childrensize += child.getTotalTreeSize();
		}
		return (childrensize + 1);
	}
	
	public final void addChildStats() {
		int nb_c = getNbChildren();
		if (nb_c > 0) {
			ClusNode ch0 = (ClusNode)getChild(0);
			ch0.addChildStats();
			ClusStatistic stat = ch0.getClusteringStat();
			ClusStatistic root = stat.cloneSimple();
			root.addPrediction(stat, 1.0);
			for (int i = 1; i < nb_c; i++) {
				ClusNode node = (ClusNode)getChild(i);
				node.addChildStats();
				root.addPrediction(node.getClusteringStat(), 1.0);
			}
			root.calcMean();
			setClusteringStat(root);
		}
	}

	public double estimateErrorAbsolute(ClusAttributeWeights scale) {
		return estimateErrorRecursive(this, scale);
	}

	public double estimateError(ClusAttributeWeights scale) {
		return estimateErrorRecursive(this, scale) / getTargetStat().getTotalWeight();
	}

	public double estimateClusteringSS(ClusAttributeWeights scale) {
		return estimateClusteringSSRecursive(this, scale);
	}

	public double estimateClusteringVariance(ClusAttributeWeights scale) {
		return estimateClusteringSSRecursive(this, scale) / getClusteringStat().getTotalWeight();
	}

	public static double estimateClusteringSSRecursive(ClusNode tree, ClusAttributeWeights scale) {
		if (tree.atBottomLevel()) {
			ClusStatistic total = tree.getClusteringStat();
			return total.getSVarS(scale);
		} else {
			double result = 0.0;
			for (int i = 0; i < tree.getNbChildren(); i++) {
				ClusNode child = (ClusNode)tree.getChild(i);
				result += estimateClusteringSSRecursive(child, scale);
			}
			return result;
		}
	}

	public static double estimateErrorRecursive(ClusNode tree, ClusAttributeWeights scale) {
		if (tree.atBottomLevel()) {
			ClusStatistic total = tree.getTargetStat();
			return total.getError(scale);
		} else {
			double result = 0.0;
			for (int i = 0; i < tree.getNbChildren(); i++) {
				ClusNode child = (ClusNode)tree.getChild(i);
				result += estimateErrorRecursive(child, scale);
			}
			return result;
		}
	}

	//if all the weight are equal to one
	// cpt count the number of leaf
	public static double estimateErrorRecursive(ClusNode tree) {
		if (tree.atBottomLevel()) {
			ClusStatistic total = tree.getTargetStat();
			//System.out.println("CLUSNODE error at leaf is "+total.getErrorRel());
			return total.getError();
		} else {
			double result = 0.0;
			for (int i = 0; i < tree.getNbChildren(); i++) {
				ClusNode child = (ClusNode)tree.getChild(i);
				result += estimateErrorRecursive(child);
			}
			return result;
		}
	}

	public int getNbLeaf(){
		int nbleaf =0;
		if (atBottomLevel()) {nbleaf++;}
		else {for (int i = 0; i < getNbChildren(); i++) {
			ClusNode child = (ClusNode)getChild(i);
			nbleaf += child.getNbLeaf();
		}}
		return nbleaf;
	}

	/***************************************************************************
	 * Printing the tree ?
	 ***************************************************************************/

	// FIXME - what for NominalTests with only two possible outcomes?

	public void printModel(PrintWriter wrt) {
		printTree(wrt, StatisticPrintInfo.getInstance(), "");
	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		printTree(wrt, info, "");
	}

	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
		printTree(wrt, info, "", examples);
	}



	public void printModelToPythonScript(PrintWriter wrt){
		printTreeToPythonScript(wrt, "\t");
	}

	public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem, boolean exhaustive){
		int lastmodel = cr.getNbModels()-1;
		System.out.println("The number of models to print is:"+lastmodel);
		String [][] tabitem = new String[lastmodel+1][10000]; //table of item
		int [][] tabexist = new int[lastmodel+1][10000]; //table of booleen for each item
		Global.set_treecpt(starttree);
		Global.set_itemsetcpt(startitem);
		ClusModelInfo m = cr.getModelInfo(0);//cr.getModelInfo(lastmodel);

		if(exhaustive){
		for (int i = 0; i < cr.getNbModels(); i++) {
		ClusModelInfo mod = cr.getModelInfo(i);
		ClusNode tree = (ClusNode)cr.getModel(i);
		if(tree.getNbChildren() != 0){
		tree.printTreeInDatabase(wrt,tabitem[i],tabexist[i], 0,"all_trees");
		}
//		print the statitistics here (the format depend on the needs of the plsql program)
		if(tree.getNbNodes() <= 1){ //we only look for the majority class in the data
		double error_rate = (tree.m_ClusteringStat).getErrorRel();
		wrt.println("#"+(tree.m_ClusteringStat).getPredictedClassName(0));
		wrt.println(mod.getModelSize()+", "+error_rate+", "+(1-error_rate));
		}else{
		//writer.println("INSERT INTO trees_charac VALUES(T1,"+size+error+accuracy+constraint);
		wrt.println(mod.getModelSize()+", "+(mod.m_TrainErr).getErrorClassif()+", "+(mod.m_TrainErr).getErrorAccuracy());
		}
		Global.inc_treecpt();
		}//end for
		}//end if
		else { //greedy search
		ClusModelInfo mod = cr.getModelInfo(lastmodel);
		ClusNode tree = (ClusNode)cr.getModel(lastmodel);
		tabitem[lastmodel][0] = "null";
		tabexist[lastmodel][0] = 1;
		wrt.println("INSERT INTO trees_sets VALUES("+Global.get_itemsetcpt()+", '"+tabitem[lastmodel][0]+"', "+tabexist[lastmodel][0]+")");
		wrt.println("INSERT INTO greedy_trees VALUES("+Global.get_treecpt()+", "+Global.get_itemsetcpt()+",1)");
		Global.inc_itemsetcpt();
		if(tree.getNbChildren() != 0){
		printTreeInDatabase(wrt,tabitem[lastmodel],tabexist[lastmodel], 1,"greedy_trees");
		}
		wrt.println("INSERT INTO trees_charac VALUES("+Global.get_treecpt()+", "+mod.getModelSize()+", "+(mod.m_TrainErr).getErrorClassif()+", "+(mod.m_TrainErr).getErrorAccuracy()+", NULL)");
		Global.inc_treecpt();
		}
	}


	public final void printTree() {
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(System.out));
		printTree(wrt, StatisticPrintInfo.getInstance(), "");
		wrt.flush();
	}

	public final void writeDistributionForInternalNode(PrintWriter writer, StatisticPrintInfo info) {
		if (info.INTERNAL_DISTR) {
			if (m_TargetStat != null) {
				writer.print(": "+m_TargetStat.getString(info));
			}
		}
		writer.println();
	}

	public final void printTree(PrintWriter writer, StatisticPrintInfo info, String prefix) {
		printTree( writer,  info,  prefix, null);
	}

	public final void printTree(PrintWriter writer, StatisticPrintInfo info, String prefix, RowData examples) {
		int arity = getNbChildren();
		if (arity > 0) {			
			int delta = hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2) {
				writer.print(m_Test.getTestString());
				RowData examples0 = null;
				RowData examples1 = null;
				if (examples!=null){
					examples0 = examples.apply(m_Test, 0);
					examples1 = examples.apply(m_Test, 1);
				}				
				showAlternatives(writer);
				writeDistributionForInternalNode(writer, info);
				writer.print(prefix + "+--yes: ");
				((ClusNode)getChild(YES)).printTree(writer, info, prefix+"|       ",examples0);
				writer.print(prefix + "+--no:  ");
				if (hasUnknownBranch()) {
					((ClusNode)getChild(NO)).printTree(writer, info, prefix+"|       ",examples1);
					writer.print(prefix + "+--unk: ");
					((ClusNode)getChild(UNK)).printTree(writer, info, prefix+"        ",examples0);
				} else {
					((ClusNode)getChild(NO)).printTree(writer, info, prefix+"        ",examples1);
				}
			} else {				
				writer.println(m_Test.getTestString());				
				for (int i = 0; i < arity; i++) {
					ClusNode child = (ClusNode)getChild(i);
					String branchlabel = m_Test.getBranchLabel(i);
					RowData examplesi = null;
					if (examples!=null){
						examples.apply(m_Test, i);
					}
					writer.print(prefix + "+--" + branchlabel + ": ");
					String suffix = StringUtils.makeString(' ', branchlabel.length()+4);
					if (i != arity-1) {
						child.printTree(writer, info, prefix+"|"+suffix,examplesi);
					} else {
						child.printTree(writer, info, prefix+" "+suffix,examplesi);
					}
				}
			}
		} else {//on the leaves
			if (m_TargetStat == null) {
				writer.print("?");
			} else {				
				writer.print(m_TargetStat.getString(info));				
			}
			if (getID() != 0 && info.SHOW_INDEX) writer.println(" ("+getID()+")");
			else writer.println();			
			if (examples!=null && examples.getNbRows()>0){
				writer.println(examples.toString(prefix));
				writer.println(prefix+"Summary:");
				writer.println(examples.getSummary(prefix));
			}

		}
	}
	
	@Override
	public Element printModelToXML(Document doc, StatisticPrintInfo info,
			RowData examples)
	{	
		int arity = getNbChildren();
		if (arity > 0)
		{			
			Element node = doc.createElement("Node");
			Attr test = doc.createAttribute("test");
			String testString = m_Test.getTestString();
			if (m_Alternatives != null)
			{
				for (int i = 0; i < m_Alternatives.length; i++) {
					testString += " and " + m_Alternatives[i];
				}
			}			
			test.setValue(testString);	
			node.setAttributeNode(test);
			if(m_TargetStat!=null)
			{				
				Element stats = m_TargetStat.getPredictElement(doc);
				node.appendChild(stats);	
			}
			else
			{
				Element unkn = doc.createElement("UnknownStat");
				node.appendChild(unkn);
			}		
			int delta = hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2)
			{				
				RowData examples0 = null;
				RowData examples1 = null;
				if (examples!=null){
					examples0 = examples.apply(m_Test, 0);
					examples1 = examples.apply(m_Test, 1);
				}	
								
				Element yesElement = ((ClusNode)getChild(YES)).printModelToXML(doc, info, examples0);
				Attr yes = doc.createAttribute("choice");
				yes.setValue("yes");
				yesElement.setAttributeNode(yes);
				node.appendChild(yesElement);
				
				Element noElement = ((ClusNode)getChild(NO)).printModelToXML(doc, info, examples1);
				Attr no = doc.createAttribute("choice");
				no.setValue("no");
				noElement.setAttributeNode(no);
				node.appendChild(noElement);
				
				if (hasUnknownBranch()) {
					Element unkElement = ((ClusNode)getChild(UNK)).printModelToXML(doc, info, examples0);
					Attr unk = doc.createAttribute("choice");
					unk.setValue("unk");
					unkElement.setAttributeNode(unk);
					node.appendChild(unkElement);					
				}
			}
			else
			{							
				for (int i = 0; i < arity; i++) {
					ClusNode child = (ClusNode)getChild(i);
					String branchlabel = m_Test.getBranchLabel(i);
					RowData examplesi = null;
					if (examples!=null){
						examplesi = examples.apply(m_Test, i);
					}
					Element element = child.printModelToXML(doc, info, examplesi);
					Attr choice = doc.createAttribute("choice");
					choice.setValue(branchlabel);
					element.setAttributeNode(choice);
					node.appendChild(element);										
				}
			}
			return node;
		}
		else
		{
			Element leaf = doc.createElement("Leaf");
			if(m_TargetStat!=null)
			{
				Element stats = m_TargetStat.getPredictElement(doc);
				leaf.appendChild(stats);	
			}
			else
			{
				Element unkn = doc.createElement("UnknownStat");
				leaf.appendChild(unkn);
			}
			if (getID() != 0)
			{
				Attr id = doc.createAttribute("id");
				id.setValue(getID()+"");
				leaf.setAttributeNode(id);
			}
			if (examples!=null && examples.getNbRows()>0)
			{
				Element examplesEl = doc.createElement("Examples");				
				leaf.appendChild(examplesEl);
				Attr n_examples = doc.createAttribute("examples");
				n_examples.setValue(examples.getNbRows()+"");
				examplesEl.setAttributeNode(n_examples);
				String[] attributes = examples.getSchema().toString().split(",");				
				ClusAttrType[] targets = examples.getSchema().getTargetAttributes();
				HashSet<Integer> targetIds = new HashSet<Integer>();
				for(ClusAttrType t:targets)
				{
					targetIds.add(t.getIndex());
				}
				for(int i=0;i<examples.getNbRows();i++)
				{					
					Element example = doc.createElement("Example");
					examplesEl.appendChild(example);					
					String[] valuesTmp = examples.getTuple(i).toString().split(",");
					ArrayList<String> vals = new ArrayList<String>();
					String together = "";
					for(int j=0;j<valuesTmp.length;j++)
					{
						if(valuesTmp[j].contains("["))
						{
							together+=valuesTmp[j];
						}
						else if(valuesTmp[j].contains("]"))
						{
							together+=","+valuesTmp[j];
							vals.add(together);
							together = "";
						}
						else if(together.equals(""))
						{
							vals.add(valuesTmp[j]);
						}
						else
						{
							together += ","+valuesTmp[j];
						}
					}
					String[] values = vals.toArray(new String[]{});
					for(int j=0;j<values.length;j++)
					{
						Element attribute = doc.createElement("Attribute");
						example.appendChild(attribute);
						Attr name = doc.createAttribute("name");
						attribute.setAttributeNode(name);
						Attr type = doc.createAttribute("type");
						attribute.setAttributeNode(type);
						if(targetIds.contains(j))
						{
							//target attribute
							type.setValue("class");
						}
						else
						{
							//regular attribute
							type.setValue("regular");
						}
						name.setValue(attributes[j]+"");

						attribute.setTextContent(values[j]);
												
					}					
				}				
			}
			return leaf;
		}		
	}
	
	
	/*
	 * Prints for each example the path that is followed in the tree, both with node identifiers, and in boolean format
	 * (used in ICDM 2011 paper on "random forest feature induction")
	 * only binary trees are supported
	 */
	public final void printPaths(PrintWriter writer, String pathprefix, String numberprefix, RowData examples, OOBSelection oob_sel, boolean testset) {
		String newnumberprefix;
		if (numberprefix.equals("")) {
			newnumberprefix = "" + getID();
		}
		else {
			newnumberprefix = numberprefix+"_"+getID();
		}
		int arity = getNbChildren();
		if (arity > 0) {
			if (arity == 2) {
				
				RowData examples0 = null;
				RowData examples1 = null;
				RowData examplesMin1 = null;
				if (examples!=null){
					examples0 = examples.apply(m_Test, 0);
					examples1 = examples.apply(m_Test, 1);
					examplesMin1 = examples.apply(m_Test, -1); // ook -1 testen en die toevoegen aan zowel examples0 en examples1 voor missingvalues
				}	
				examples0.add(examplesMin1);
				examples1.add(examplesMin1);
				((ClusNode)getChild(YES)).printPaths(writer, pathprefix+"0", newnumberprefix, examples0, oob_sel, testset);
				((ClusNode)getChild(NO)).printPaths(writer, pathprefix+"1", newnumberprefix, examples1, oob_sel, testset);
				
			} else {
				System.out.println("PrintPaths error: only binary trees supported");
			}
		} else { //at the leaves
			if (examples!=null){
				for (int i=0; i<examples.getNbRows(); i++) {
					int exampleindex = examples.getTuple(i).getIndex();
					if (testset) {
						writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + "  TEST");
					}
					else if (oob_sel != null) {
						boolean oob = oob_sel.isSelected(exampleindex);
						if (oob) {
							writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + "  OOB");
						}
						else writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix);
					}
					else writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix);
					writer.flush();
                }	
			}
			
		}
	}
	
	
	// only binary trees supported
	// no "unknown" branches supported
	/*public final void printPaths(PrintWriter writer, String pathprefix, String numberprefix, RowData examples, OOBSelection oob_sel) {
		//writer.flush();
		//writer.println("nb ex: " + examples.getNbRows());
		String newnumberprefix;
		if (numberprefix.equals("")) {
			newnumberprefix = "" + getID();
		}
		else {
			newnumberprefix = numberprefix+"_"+getID();
		}
		int arity = getNbChildren();
		if (arity > 0) {
			if (arity == 2) {

				RowData examples0 = null;
				RowData examples1 = null;
				RowData examplesMin1 = null;
				if (examples!=null){
					examples0 = examples.apply(m_Test, 0);
					examples1 = examples.apply(m_Test, 1);
					examplesMin1 = examples.apply(m_Test, -1); // ook -1 testen en die toevoegen aan zowel examples0 en examples1 voor missingvalues
				}	
				examples0.add(examplesMin1);
				examples1.add(examplesMin1);
				((ClusNode)getChild(YES)).printPaths(writer, pathprefix+"0", newnumberprefix, examples0,oob_sel);
				((ClusNode)getChild(NO)).printPaths(writer, pathprefix+"1", newnumberprefix, examples1,oob_sel);
				
			} else {
				System.out.println("PrintPaths error: only binary trees supported");
			}
		} else {//on the leaves
			if (examples!=null){
				//writer.println("LEAF");
                String prediction = m_TargetStat.getPredictString();
                for (int i=0; i<examples.getNbRows(); i++) {
                        int exampleindex = examples.getTuple(i).getIndex();
                        if (oob_sel != null) {
                        boolean oob = oob_sel.isSelected(exampleindex);
                        if (oob)
                               // writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + " " + prediction + "  OOB");
                        		writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + "  OOB");
                       // else writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + " " + prediction);
                        else writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix);
                        }
                       // else writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + " " + prediction);
                        else writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix);
                        writer.flush();
                }	
			}

		}
	}*/
	
	// printing test exs
	/*public final void printPaths(PrintWriter writer, String pathprefix, String numberprefix, RowData examples) {
		//writer.flush();
		//writer.println("nb ex: " + examples.getNbRows());
		String newnumberprefix;
		if (numberprefix.equals("")) {
			newnumberprefix = "" + getID();
		}
		else {
			newnumberprefix = numberprefix+"_"+getID();
		}
		int arity = getNbChildren();
		if (arity > 0) {
			if (arity == 2) {

				RowData examples0 = null;
				RowData examples1 = null;
				RowData examplesMin1 = null;
				if (examples!=null){
					examples0 = examples.apply(m_Test, 0);
					examples1 = examples.apply(m_Test, 1);
					examplesMin1 = examples.apply(m_Test, -1);
				}		
				examples0.add(examplesMin1);
				examples1.add(examplesMin1);
				((ClusNode)getChild(YES)).printPaths(writer, pathprefix+"0", newnumberprefix, examples0);
				((ClusNode)getChild(NO)).printPaths(writer, pathprefix+"1", newnumberprefix, examples1);
				
			} else {
				System.out.println("PrintPaths error: only binary trees supported");
			}
		} else {//on the leaves
			if (examples!=null){
				//writer.println("LEAF");
                String prediction = m_TargetStat.getPredictString();
                for (int i=0; i<examples.getNbRows(); i++) {
                        int exampleindex = examples.getTuple(i).getIndex();
                        if (exampleindex < 8192) {  // added because test and unlabeled set were put together in one testfile
                       // writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + " " + prediction + "  TEST");
                        writer.println(exampleindex + "  " + pathprefix + " " + newnumberprefix + "  TEST");
                        writer.flush();
                        }
                }	
			}

		}
	}*/
	
	

	/*to print the tree directly into an IDB : Elisa Fromont 13/06/2007*/
	public final void printTreeInDatabase(PrintWriter writer, String tabitem[], int tabexist[], int cpt, String typetree) {
		int arity = getNbChildren();
		if (arity > 0) {
			int delta = hasUnknownBranch() ? 1 : 0;
				if (arity - delta == 2) { //the tree is binary
					// in case the test is postive
					tabitem[cpt] = m_Test.getTestString();
					tabexist[cpt] = 1;
					cpt++;
					((ClusNode)getChild(YES)).printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);
					cpt--;//to remove the last test on the father : now the test is negative
					// in ca	se the test is negative
					tabitem[cpt]= m_Test.getTestString();
					//System.out.println("cpt = "+cpt+", tabitem = "+tabitem[cpt]);
					tabexist[cpt] = 0;
					cpt++;
					if (hasUnknownBranch()) {
						((ClusNode)getChild(NO)).printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);

						((ClusNode)getChild(UNK)).printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);
						}
					else {
					((ClusNode)getChild(NO)).printTreeInDatabase(writer, tabitem, tabexist, cpt, typetree);
					}
				}//end if arity- delta ==2

				else{ //arity -delta =/= 2	the tree is not binary
					//Has not beeen modified for databse purpose yet !!!!!!
					writer.println("arity-delta different 2");
					for (int i = 0; i < arity; i++) {
					ClusNode child = (ClusNode)getChild(i);
					String branchlabel = m_Test.getBranchLabel(i);
					writer.print("+--" + branchlabel + ": ");
					if (i != arity-1) {
						child.printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);
					} else {
						child.printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);
					}
					}// end for
				}//end else arity -delta =/= 2
				} //end if arity >0 0

				else {// if arity =0 : on a leaf
					if (m_TargetStat == null) {
						writer.print("?");
					} else {
						tabitem[cpt] = m_TargetStat.getPredictedClassName(0);
						tabexist[cpt] = 1;
						writer.print("#"); //nb leaf
						for(int i =0; i <= (cpt-1); i++){
						writer.print(printTestNode(tabitem[i],tabexist[i])+", ");
						}
						writer.println(printTestNode(tabitem[cpt],tabexist[cpt]));
						cpt++;
					}
				}//end else if arity =0

		}

	public String printTestNode(String a, int pres){
		if(pres == 1) {return a;}
		else {return ("not("+a+")");}
	}

	public final void printTreeToPythonScript(PrintWriter writer, String prefix) {
		int arity = getNbChildren();
		if (arity > 0) {
			int delta = hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2) {
				writer.println(prefix+"if " +m_Test.getTestString()+":");
				((ClusNode)getChild(YES)).printTreeToPythonScript(writer, prefix+"\t");
				writer.println(prefix + "else: ");
				if (hasUnknownBranch()) {
					//TODO anything to do???
				} else {
					((ClusNode)getChild(NO)).printTreeToPythonScript(writer, prefix+"\t");
				}
			} else {
				//TODO what to do?
			}
		} else {
			if (m_TargetStat != null) {
				writer.println(prefix+"return "+m_TargetStat.getArrayOfStatistic());
			}
		}
	}

	public final void showAlternatives(PrintWriter writer) {
		if (m_Alternatives == null) return;
		for (int i = 0; i < m_Alternatives.length; i++) {
			writer.print(" and " + m_Alternatives[i]);
		}
	}

	public String toString() {
		try{
			if (hasBestTest()) return getTestString();
			else return m_TargetStat.getSimpleString();
		}
		catch(Exception e){return "null clusnode ";}
	}

	/**
	 * Returns the majority class for this node.(not so good name)
	 */
	public ClusStatistic predictWeightedLeaf(DataTuple tuple) {
		return getTargetStat();
	}

	public void retrieveStatistics(ArrayList list) {
		if (m_ClusteringStat != null) list.add(m_ClusteringStat);
		if (m_TargetStat != null) list.add(m_TargetStat);
		int arity = getNbChildren();
		for (int i = 0; i < arity; i++) {
			ClusNode child = (ClusNode)getChild(i);
			child.retrieveStatistics(list);
		}
	}

	public int getLargestBranchIndex() {
		double max = 0.0;
		int max_idx = -1;
		for (int i = 0; i < getNbChildren(); i++) {
			ClusNode child = (ClusNode)getChild(i);
			double child_w = child.getTotWeight();
			if (ClusUtil.grOrEq(child_w, max)) {
				max = child_w;
				max_idx = i;
			}
		}
		return max_idx;
	}

	public void adaptToData(RowData data) {
		// sort data into tree
		NodeTest tst = getTest();
		for (int i = 0; i < getNbChildren(); i++) {
			ClusNode child = (ClusNode)getChild(i);
			RowData subset = data.applyWeighted(tst, i);
			child.adaptToData(subset);
		}
		// recompute statistics
		reInitTargetStat(data);
		reInitClusteringStat(data);
	}	
}
