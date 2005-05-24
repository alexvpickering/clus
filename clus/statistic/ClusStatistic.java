package clus.statistic;

import clus.util.*;
import clus.data.cols.*; 
import clus.data.rows.*; 

import java.io.Serializable;

public abstract class ClusStatistic implements Serializable {

	public final static long serialVersionUID=-5224931046473825824L;

	public double m_SumWeight;
	
	public abstract ClusStatistic cloneStat();
		
	public ClusStatistic cloneSimple() {
		// Statistic with only memory allocated for storing mean
		// not variance, see e.g. RegressionStat.java
		return cloneStat();
	}

	public void printDebug() {
	}
	
    public void setSumWeight(double weight){
        m_SumWeight = weight;
    }

	public void setSDataSize(int nbex) {
	}
	
	public void optimizePreCalc(RowData data) {
	}
	
	public void showRootInfo() {
	}	
	
	public void update(ColTarget target, int idx) {
		System.err.println(getClass().getName()+": update(ColTarget target, int idx): Not yet implemented");
	}
	
	public abstract void updateWeighted(DataTuple tuple, int idx);
	
	public void updateWeighted(DataTuple tuple, double weight) {
	}

	public abstract void calcMean();
	
	public abstract String getString();

	public abstract void reset();

	public abstract void copy(ClusStatistic other);	
	
	public abstract void addPrediction(ClusStatistic other, double weight);	
	
	public abstract void add(ClusStatistic other);
	
	public void addScaled(double scale, ClusStatistic other) {
		System.err.println(getClass().getName()+": addScaled(): Not yet implemented");		
	}
	
	public abstract void subtractFromThis(ClusStatistic other);
	
	public abstract void subtractFromOther(ClusStatistic other);	
	
	public double[] getNumericPred() {
		System.err.println(getClass().getName()+": getNumericPred(): Not yet implemented");
		return null;
	}
	
	public int[] getNominalPred() {
		System.err.println(getClass().getName()+": getNumericPred(): Not yet implemented");
		return null;
	}	
		
	public String getString2() {
		return "";
	}
	
	public String getClassString() {
		return getString();
	}	
	
	public String getSimpleString() {
		return ClusFormat.ONE_AFTER_DOT.format(m_SumWeight);
	}
	
	public double getTotalWeight() {
		return m_SumWeight;
	}
	
	public String getDebugString() {
		return String.valueOf(m_SumWeight);
	}
	
	public boolean samePrediction(ClusStatistic other) {
		return false;
	}
	
/*
 * getError() and getErrorDiff() methods 
 * - with scaling
 * - without scaling (only works for classification now!)
 **/	
	
	public double getError() {
		return getError(null);
	}

	public double getErrorDiff(ClusStatistic other) {
		return getErrorDiff(null, other);
	}
	
	public double getError(TargetWeightProducer scale) {
		System.err.println(getClass().getName()+": getError(): Not yet implemented");
		return Double.POSITIVE_INFINITY;
	}

	public double getErrorDiff(TargetWeightProducer scale, ClusStatistic other) {
		System.err.println(getClass().getName()+": getErrorDiff(): Not yet implemented");		
		return Double.POSITIVE_INFINITY;
	}

/*
 * getSS() and getSSDiff() methods, always with scaling
 * also version available that needs access to the data 
 **/	
	
	public double getSS(TargetWeightProducer scale) {
		System.err.println(getClass().getName()+": getSS(): Not yet implemented");		
		return Double.POSITIVE_INFINITY;
	}

	public double getSSDiff(TargetWeightProducer scale, ClusStatistic other) {
		System.err.println(getClass().getName()+": getSSDiff(): Not yet implemented");
		return Double.POSITIVE_INFINITY;
	}
	
	public double getSS(TargetWeightProducer scale, RowData data) {
		return getSS(scale);
	}

	public double getSSDiff(TargetWeightProducer scale, ClusStatistic other, RowData data) {
		return getSSDiff(scale, other);
	}
	
	public static void reset(ClusStatistic[] stat) {
		for (int i = 0; i < stat.length; i++) stat[i].reset();
	}
	
	public String toString() {
		return getString();
	}
}
