/*
 * Created on Jul 22, 2005
 */
package clus.pruning;

import java.io.IOException;

import clus.data.rows.*;
import clus.error.ClusError;
import clus.error.ClusErrorParent;
import clus.main.*;
import clus.statistic.ClusStatistic;

public class SizeConstraintErrorComputer  extends ClusModelProcessor {

	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
		ClusNode tree = (ClusNode)model;
		SizeConstraintVisitor visitor = (SizeConstraintVisitor)tree.getVisitor();
		visitor.testerr.addExample(tuple, tree.getTargetStat());					
	}
	
	public boolean needsModelUpdate() {
		return true;
	}		
	
	public boolean needsInternalNodes() {
		return true;
	}

	public static ClusError computeErrorOptimized(ClusNode tree, RowData test, ClusErrorParent error, boolean miss) {
		error.reset();
		error.setNbExamples(test.getNbRows());
		ClusError child_err = error.getFirstError().getErrorClone();
		SizeConstraintErrorComputer.computeErrorOptimized(tree, test, child_err, miss);
		return child_err;
	}	
	
	public static void computeErrorOptimized(ClusNode tree, RowData test, ClusError error, boolean miss) {
		if (miss) {
			computeErrorStandard(tree, test, error);
		} else {
			computeErrorSimple(tree, error);
			// Debug?
			// ClusError clone = error.getErrorClone();
			// computeErrorStandard(tree, test, clone);
			// System.out.println("Simple = "+error.getModelError()+" standard = "+clone.getModelError());
		}
	}
	
	public static void computeErrorStandard(ClusNode tree, RowData test, ClusError error) {
		for (int i = 0; i < test.getNbRows(); i++) {
			DataTuple tuple = test.getTuple(i);
			ClusStatistic pred = tree.predictWeighted(tuple);			
			error.addExample(tuple, pred);
		}
	}
	
	public static void initializeTestErrorsData(ClusNode tree, RowData test, ClusError error) throws IOException {
		SizeConstraintErrorComputer comp = new SizeConstraintErrorComputer(); 
		initializeTestErrors(tree, error);
		for (int i = 0; i < test.getNbRows(); i++) {
			DataTuple tuple = test.getTuple(i);
			tree.applyModelProcessor(tuple, comp);
		}	
	}

	public static void initializeTestErrors(ClusNode node, ClusError error) {
		SizeConstraintVisitor visitor = (SizeConstraintVisitor)node.getVisitor();
		visitor.testerr = error.getErrorClone(error.getParent());
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClusNode child = (ClusNode)node.getChild(i);
			initializeTestErrors(child, error);
		}
	}

	public static void computeErrorSimple(ClusNode node, ClusError sum) {		
		if (node.atBottomLevel()) {
			SizeConstraintVisitor visitor = (SizeConstraintVisitor)node.getVisitor();
			sum.add(visitor.testerr);
		} else {
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				computeErrorSimple(child, sum);
			}		
		}
	}
}
