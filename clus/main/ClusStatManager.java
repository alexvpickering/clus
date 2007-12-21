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

package clus.main;

import jeans.io.ini.INIFileNominalOrDoubleOrVector;

import clus.util.*;
import clus.data.ClusData;
import clus.data.attweights.*;
import clus.data.type.*;
import clus.data.rows.*;
import clus.error.*;
import clus.error.multiscore.*;
import clus.heuristic.*;
import clus.statistic.*;
import clus.pruning.*;

import clus.ext.hierarchical.*;
import clus.ext.sspd.*;
import clus.ext.timeseries.*;
import clus.ext.beamsearch.*;
import clus.ext.ilevelc.*;

import clus.algo.rules.*;

import java.io.*;
import java.util.*;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.DistributionFactory;

public class ClusStatManager implements Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public final static int SUBM_DEFAULT = 0;

	public final static int MODE_NONE = -1;

	public final static int MODE_CLASSIFY = 0;

	public final static int MODE_REGRESSION = 1;

	public final static int MODE_HIERARCHICAL = 2;

	public final static int MODE_SSPD = 3;

	public final static int MODE_CLASSIFY_AND_REGRESSION = 4;

	public final static int MODE_TIME_SERIES = 5;
	
	public final static int MODE_ILEVELC = 6;	

	protected int m_Mode = MODE_NONE;

	protected transient ClusHeuristic m_Heuristic;

	protected transient ClusStatistic m_TargetStatistic, m_AllStatistic;

	protected TargetSchema m_Target;

	protected ClusSchema m_Schema;

	protected boolean m_BeamSearch;

	protected boolean m_RuleInduce;

	protected Settings m_Settings;

	protected ClusStatistic m_GlobalStat;

	protected ClusStatistic m_TrainSetStat;

	protected ClusStatistic[] m_StatisticAttrUse;

	protected ClusAttributeWeights m_NormalizationWeights;

	protected ClusAttributeWeights m_ClusteringWeights;

	protected ClusNormalizedAttributeWeights m_CompactnessWeights;

	protected ClassHierarchy m_HierN, m_HierF, m_Hier;

	protected SSPDMatrix m_SSPDMtrx;

	protected double[] m_ChiSquareInvProb;

	public ClusStatManager(ClusSchema schema, Settings sett)
			throws ClusException, IOException {
		this(schema, sett, true);
	}

	public ClusStatManager(ClusSchema schema, Settings sett, boolean docheck)
			throws ClusException, IOException {
		m_Schema = schema;
		m_Target = schema.getTargetSchema();
		m_Settings = sett;
		if (docheck) {
			check();
			initStructure();
		}
	}

	public Settings getSettings() {
		return m_Settings;
	}

	public int getCompatibility() {
		return getSettings().getCompatibility();
	}

	public final ClusSchema getSchema() {
		return m_Schema;
	}

	public final int getMode() {
		return m_Mode;
	}

	public final TargetSchema getTargetSchema() {
		return m_Target;
	}

	public final ClassHierarchy getHier() {
		// System.out.println("ClusStatManager.getHier/0 called");
		return m_Hier;
	}

	public final ClassHierarchy getNormalHier() {
		return m_HierN;
	}

	public void initSH() throws ClusException, IOException {
		initWeights();
		initStatistic();
		initHierarchySettings();
	}

	public ClusAttributeWeights getClusteringWeights() {
		return m_ClusteringWeights;
	}

	public ClusNormalizedAttributeWeights getCompactnessWeights() {
		return m_CompactnessWeights;
	}

	public ClusAttributeWeights getNormalizationWeights() {
		return m_NormalizationWeights;
	}

	public static boolean hasBitEqualToOne(boolean[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i])
				return true;
		}
		return false;
	}

	public void initWeights(ClusNormalizedAttributeWeights result,
			NumericAttrType[] num, NominalAttrType[] nom,
			INIFileNominalOrDoubleOrVector winfo) throws ClusException {
		result.setAllWeights(0.0);
		int nbattr = result.getNbAttributes();
		if (winfo.hasArrayIndexNames()) {
			// Weights given for target, non-target, numeric and nominal
			double target_weight = winfo.getDouble(Settings.TARGET_WEIGHT);
			double non_target_weight = winfo
					.getDouble(Settings.NON_TARGET_WEIGHT);
			double num_weight = winfo.getDouble(Settings.NUMERIC_WEIGHT);
			double nom_weight = winfo.getDouble(Settings.NOMINAL_WEIGHT);
			System.out.println("  Target weight     = " + target_weight);
			System.out.println("  Non target weight = " + non_target_weight);
			System.out.println("  Numeric weight    = " + num_weight);
			System.out.println("  Nominal weight    = " + nom_weight);
			for (int i = 0; i < num.length; i++) {
				NumericAttrType cr_num = num[i];
				double tw = cr_num.getStatus() == ClusAttrType.STATUS_TARGET ? target_weight
						: non_target_weight;
				result.setWeight(cr_num, num_weight * tw);
			}
			for (int i = 0; i < nom.length; i++) {
				NominalAttrType cr_nom = nom[i];
				double tw = cr_nom.getStatus() == ClusAttrType.STATUS_TARGET ? target_weight
						: non_target_weight;
				result.setWeight(cr_nom, nom_weight * tw);
			}
		} else if (winfo.isVector()) {
			// Explicit vector of weights given
			if (nbattr != winfo.getVectorLength()) {
				throw new ClusException("Number of attributes is " + nbattr
						+ " but weight vector has only "
						+ winfo.getVectorLength() + " components");
			}
			for (int i = 0; i < nbattr; i++) {
				result.setWeight(i, winfo.getDouble(i));
			}
		} else {
			// One single constant weight given
			result.setAllWeights(winfo.getDouble());
		}
		// Normalize the weights
		double sum = 0;
		for (int i = 0; i < num.length; i++) {
			NumericAttrType cr_num = num[i];
			sum += result.getWeight(cr_num);
		}
		for (int i = 0; i < nom.length; i++) {
			NominalAttrType cr_nom = nom[i];
			sum += result.getWeight(cr_nom);
		}
		if (sum <= 0) {
			/*
			 * Valentin: I must comment this, because of when I have only time
			 * series as clustering attributes, this doesn't work. sorry bernard :(
			 */
			// throw new ClusException("initWeights(): Sum of
			// clustering/compactness weights must be > 0!");
		}
/*		for (int i = 0; i < num.length; i++) {
			NumericAttrType cr_num = num[i];
			result.setWeight(cr_num, result.getWeight(cr_num) / sum);
		}
		for (int i = 0; i < nom.length; i++) {
			NominalAttrType cr_nom = nom[i];
			result.setWeight(cr_nom, result.getWeight(cr_nom) / sum);
		}*/
	}

	public void initCompactnessWeights() throws ClusException {
		NumericAttrType[] num = m_Schema
				.getNumericAttrUse(ClusAttrType.ATTR_USE_ALL);
		NominalAttrType[] nom = m_Schema
				.getNominalAttrUse(ClusAttrType.ATTR_USE_ALL);
		initWeights(m_CompactnessWeights, num, nom, getSettings()
				.getCompactnessWeights());
		System.out.println("Compactness:   "
				+ m_CompactnessWeights.getName(m_Schema
						.getAllAttrUse(ClusAttrType.ATTR_USE_ALL)));
	}

	public void initClusteringWeights() throws ClusException {
		if (getMode() == MODE_HIERARCHICAL) {
			int nb_attrs = m_Schema.getNbAttributes();
			m_ClusteringWeights = new ClusAttributeWeights(nb_attrs
					+ m_Hier.getTotal());
			double[] weights = m_Hier.getWeights();
			NumericAttrType[] dummy = m_Hier.getDummyAttrs();
			for (int i = 0; i < weights.length; i++) {
				m_ClusteringWeights.setWeight(dummy[i], weights[i]);
			}
			return;
		}
		NumericAttrType[] num = m_Schema
				.getNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		NominalAttrType[] nom = m_Schema
				.getNominalAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		initWeights((ClusNormalizedAttributeWeights) m_ClusteringWeights, num,
				nom, getSettings().getClusteringWeights());
		System.out.println("Clustering: "
				+ m_ClusteringWeights.getName(m_Schema
						.getAllAttrUse(ClusAttrType.ATTR_USE_CLUSTERING)));
	}

	public void initNormalizationWeights(ClusStatistic stat)
			throws ClusException {
		m_GlobalStat = stat;
		m_TrainSetStat = stat; // When no XVal
		int nbattr = m_Schema.getNbAttributes();
		m_NormalizationWeights.setAllWeights(1.0);
		boolean[] shouldNormalize = new boolean[nbattr];
		INIFileNominalOrDoubleOrVector winfo = getSettings()
				.getNormalizationWeights();
		if (winfo.isVector()) {
			if (nbattr != winfo.getVectorLength()) {
				throw new ClusException("Number of attributes is " + nbattr
						+ " but weight vector has only "
						+ winfo.getVectorLength() + " components");
			}
			for (int i = 0; i < nbattr; i++) {
				if (winfo.isNominal(i))
					shouldNormalize[i] = true;
				else
					m_NormalizationWeights.setWeight(i, winfo.getDouble(i));
			}
		} else {
			if (winfo.isNominal()
					&& winfo.getNominal() == Settings.NORMALIZATION_DEFAULT) {
				Arrays.fill(shouldNormalize, true);
			} else {
				m_NormalizationWeights.setAllWeights(winfo.getDouble());
			}
		}
		if (hasBitEqualToOne(shouldNormalize)) {
			CombStat cmb = (CombStat) stat;
			RegressionStat rstat = cmb.getRegressionStat();
			rstat.initNormalizationWeights(m_NormalizationWeights,
					shouldNormalize);
		}
	}

	public void initWeights() {
		int nbattr = m_Schema.getNbAttributes();
		m_NormalizationWeights = new ClusAttributeWeights(nbattr);
		m_NormalizationWeights.setAllWeights(1.0);
		m_ClusteringWeights = new ClusNormalizedAttributeWeights(
				m_NormalizationWeights);
		m_CompactnessWeights = new ClusNormalizedAttributeWeights(
				m_NormalizationWeights);
	}

	public ClusStatistic getGlobalStat() {
		return m_GlobalStat;
	}

	public ClusStatistic getTrainSetStat() {
		return m_TrainSetStat;
	}

	public void setTrainSetStat(ClusStatistic stat) {
		m_TrainSetStat = stat;
	}
	
	public void computeTrainSetStat(RowData trainset) {
		ClusStatistic tr_stat = createStatistic(ClusAttrType.ATTR_USE_ALL);
		trainset.calcTotalStat(tr_stat);
		setTrainSetStat(tr_stat);
	}

	public void check() throws ClusException {
		int nb_types = 0;
		int nb_nom = m_Schema
				.getNbNominalAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		int nb_num = m_Schema
				.getNbNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		int nb_int = m_Target.getNbType(IntegerAttrType.THIS_TYPE);

		if (nb_nom > 0 && nb_num > 0) {
			m_Mode = MODE_CLASSIFY_AND_REGRESSION;
			nb_types++;
		} else if (nb_nom > 0) {
			m_Mode = MODE_CLASSIFY;
			nb_types++;
		} else if (nb_num > 0) {
			m_Mode = MODE_REGRESSION;
			nb_types++;
		}
		if (m_Schema.hasAttributeType(ClusAttrType.ATTR_USE_TARGET,
				ClassesAttrType.THIS_TYPE)) {
			m_Mode = MODE_HIERARCHICAL;
			getSettings().setSectionHierarchicalEnabled(true);
			nb_types++;
		}
		if (nb_int > 0 || m_Settings.checkHeuristic("SSPD")) {
			m_Mode = MODE_SSPD;
			nb_types++;
		}
		if (m_Settings.isSectionTimeSeriesEnabled()) {
			m_Mode = MODE_TIME_SERIES;
			nb_types++;
		}
		if (m_Settings.isSectionILevelCEnabled()) {
			m_Mode = MODE_ILEVELC;
		}
		if (nb_types == 0) {
			System.err.println("No target value defined");
		}
		if (nb_types > 1) {
			throw new ClusException(
					"Incompatible combination of clustering attribute types");
		}
	}

	public void initStructure() throws IOException {
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			createHierarchy();
			break;
		case MODE_SSPD:
			m_SSPDMtrx = SSPDMatrix.read(getSettings().getAppName() + ".dist",
					getSettings());
			break;
		}
	}

	public ClusStatistic createSuitableStat(NumericAttrType[] num,
			NominalAttrType[] nom) {
		if (num.length == 0) {
			return new ClassificationStat(nom);
		} else if (nom.length == 0) {
			return new RegressionStat(num);
		} else {
			return new CombStat(this, num, nom);
		}
	}

	public boolean heuristicNeedsCombStat() {
		if (isRuleInduce()) {
			return (getSettings().getHeuristic() == Settings.HEURISTIC_DISPERSION_ADT
					|| getSettings().getHeuristic() == Settings.HEURISTIC_DISPERSION_MLT
					|| getSettings().getHeuristic() == Settings.HEURISTIC_WR_DISPERSION_ADT || getSettings()
					.getHeuristic() == Settings.HEURISTIC_WR_DISPERSION_MLT);
		} else {
			return false;
		}
	}

	public void initStatistic() throws ClusException {
		m_StatisticAttrUse = new ClusStatistic[ClusAttrType.NB_ATTR_USE];
		// Statistic over all attributes
		NumericAttrType[] num1 = m_Schema
				.getNumericAttrUse(ClusAttrType.ATTR_USE_ALL);
		NominalAttrType[] nom1 = m_Schema
				.getNominalAttrUse(ClusAttrType.ATTR_USE_ALL);
		m_StatisticAttrUse[ClusAttrType.ATTR_USE_ALL] = new CombStat(this,
				num1, nom1);
		// Statistic over all target attributes
		NumericAttrType[] num2 = m_Schema
				.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom2 = m_Schema
				.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		m_StatisticAttrUse[ClusAttrType.ATTR_USE_TARGET] = createSuitableStat(
				num2, nom2);
		// Statistic over clustering attributes
		NumericAttrType[] num3 = m_Schema
				.getNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		NominalAttrType[] nom3 = m_Schema
				.getNominalAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		if (num3.length != 0 || nom3.length != 0) {
			if (heuristicNeedsCombStat()) {
				m_StatisticAttrUse[ClusAttrType.ATTR_USE_CLUSTERING] = new CombStat(
						this, num3, nom3);
			} else {
				m_StatisticAttrUse[ClusAttrType.ATTR_USE_CLUSTERING] = createSuitableStat(
						num3, nom3);
			}
		}
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			setClusteringStatistic(new WHTDStatistic(m_Hier, getCompatibility()));
			setTargetStatistic(new WHTDStatistic(m_Hier, getCompatibility()));
			break;
		case MODE_SSPD:
			setClusteringStatistic(new SSPDStatistic(m_SSPDMtrx));
			setTargetStatistic(new SSPDStatistic(m_SSPDMtrx));
			break;
		case MODE_TIME_SERIES:
			switch (Settings.timeSeriesDM.getValue()) {
			case Settings.TIME_SERIES_DISTANCE_MEASURE_DTW:
				setClusteringStatistic(new DTWTimeSeriesStat());
				setTargetStatistic(new DTWTimeSeriesStat());
				break;
			case Settings.TIME_SERIES_DISTANCE_MEASURE_QDM:
				setClusteringStatistic(new QDMTimeSeriesStat());
				setTargetStatistic(new QDMTimeSeriesStat());
				break;
			case Settings.TIME_SERIES_DISTANCE_MEASURE_TSC:
				setClusteringStatistic(new TSCTimeSeriesStat());
				setTargetStatistic(new TSCTimeSeriesStat());
				break;
			}
			break;
		case MODE_ILEVELC:
			setTargetStatistic(new ILevelCStatistic(num2));
			setClusteringStatistic(new ILevelCStatistic(num3));						
			break;
		}
	}

	public ClusHeuristic createHeuristic(int type) {
		switch (type) {
		case Settings.HEURISTIC_GAIN:
			return new GainHeuristic();
		default:
			return null;
		}
	}
	
	public void initRuleHeuristic() throws ClusException {
		if ((getSettings().getHeuristic() == Settings.HEURISTIC_GAIN)) {
			throw new ClusException("Gain heuristic not supported for rule induction!");
		}
		if (m_Mode == MODE_CLASSIFY) {
			switch (getSettings().getHeuristic()) {
			case Settings.HEURISTIC_REDUCED_ERROR:
				m_Heuristic = new ClusRuleHeuristicError(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_MESTIMATE:
				m_Heuristic = new ClusRuleHeuristicMEstimate(getSettings().getMEstimate());
				break;
			case Settings.HEURISTIC_DISPERSION_ADT:
				m_Heuristic = new ClusRuleHeuristicDispersionAdt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_DISPERSION_MLT:
				m_Heuristic = new ClusRuleHeuristicDispersionMlt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_WR_DISPERSION_ADT:
				m_Heuristic = new ClusRuleHeuristicWRDispersionAdt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_WR_DISPERSION_MLT:
				m_Heuristic = new ClusRuleHeuristicWRDispersionMlt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_DEFAULT:
				m_Heuristic = new ClusRuleHeuristicError(this, getClusteringWeights());
				break;
			}
		} else {
			switch (getSettings().getHeuristic()) {
			case Settings.HEURISTIC_REDUCED_ERROR:
				m_Heuristic = new ClusRuleHeuristicError(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_MESTIMATE:
				throw new ClusException("MEstimate heuristic: regression and/or classification with multiple clustering attributes not supported!");
			case Settings.HEURISTIC_DISPERSION_ADT:
				m_Heuristic = new ClusRuleHeuristicDispersionAdt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_DISPERSION_MLT:
				m_Heuristic = new ClusRuleHeuristicDispersionMlt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_WR_DISPERSION_ADT:
				m_Heuristic = new ClusRuleHeuristicWRDispersionAdt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_WR_DISPERSION_MLT:
				m_Heuristic = new ClusRuleHeuristicWRDispersionMlt(this, getClusteringWeights());
				break;
			case Settings.HEURISTIC_DEFAULT:
				m_Heuristic = new ClusRuleHeuristicError(this, getClusteringWeights());
				break;
			}
		}	
	}
	
	public void initBeamSearchHeuristic() throws ClusException {
		if (getSettings().getHeuristic() == Settings.HEURISTIC_REDUCED_ERROR) {
			m_Heuristic = new ClusBeamHeuristicError(createClusteringStat());
		} else if (getSettings().getHeuristic() == Settings.HEURISTIC_MESTIMATE) {
			m_Heuristic = new ClusBeamHeuristicMEstimate(createClusteringStat(), getSettings().getMEstimate());
		} else if (getSettings().getHeuristic() == Settings.HEURISTIC_MORISHITA) {
			m_Heuristic = new ClusBeamHeuristicMorishita(createClusteringStat());
		} else {
			m_Heuristic = new ClusBeamHeuristicSS(createClusteringStat(), getClusteringWeights());
		}
	}

	public void initHeuristic() throws ClusException {
		if (isRuleInduce()) {
			initRuleHeuristic();
			return;
		}
		if (isBeamSearch()) {
			initBeamSearchHeuristic();
			return;
		}
		if (m_Mode == MODE_HIERARCHICAL) {
			String name = "Weighted Hierarchical Tree Distance";
			m_Heuristic = new SSDHeuristic(name, createClusteringStat(), getClusteringWeights());
			return;
		}
		if (m_Mode == MODE_SSPD) {
			m_Heuristic = new SSPDHeuristic(m_SSPDMtrx);
			return;
		}
		if (m_Mode == MODE_TIME_SERIES) {
			String name = "Time Series Intra-Cluster Variation Heuristic";
			m_Heuristic = new SSDHeuristic(name, createClusteringStat(), getClusteringWeights());
			return;
		}
		/* Set heuristic for trees */
		NumericAttrType[] num = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		NominalAttrType[] nom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);		
		if (num.length > 0 && nom.length > 0) {
			throw new ClusException("Combined heuristic not yet implemented for trees!");
		} else if (num.length > 0) {
			// TODO: Is this true?
			if (getSettings().getHeuristic() != Settings.HEURISTIC_DEFAULT) {
				throw new ClusException("Only SS-Reduction (default) heuristic can be used for regression trees!");
			}
			m_Heuristic = new SSReductionHeuristic(getClusteringWeights(), m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING));
		} else if (nom.length > 0) {
			if ((getSettings().getHeuristic() != Settings.HEURISTIC_DEFAULT)
					&& (getSettings().getHeuristic() != Settings.HEURISTIC_REDUCED_ERROR)
					&& (getSettings().getHeuristic() != Settings.HEURISTIC_GAIN)) {
				throw new ClusException("Only Gain (default) or Reduced Error heuristic can be used for classification trees!");
			}
			if (getSettings().getHeuristic() == Settings.HEURISTIC_REDUCED_ERROR) {
				m_Heuristic = new ReducedErrorHeuristic(createClusteringStat());
			} else {
				m_Heuristic = new GainHeuristic();
			}
		}
	}

	/**
	 * Initializes a table with Chi Squared inverse probabilities used in
	 * significance testing of rules.
	 * 
	 * @throws MathException
	 * 
	 */
	public void initSignifcanceTestingTable() {
		int max_nom_val = 0;
		int num_nom_atts = m_Schema.getNbNominalAttrUse(ClusAttrType.ATTR_USE_ALL);
		for (int i = 0; i < num_nom_atts; i++) {
			if (m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_ALL)[i].m_NbValues > max_nom_val) {
				max_nom_val = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_ALL)[i].m_NbValues;
			}
		}
		if (max_nom_val == 0) { // If no nominal attributes in data set
			max_nom_val = 1;
		}
		double[] table = new double[max_nom_val];
		table[0] = 1.0 - getSettings().getRuleSignificanceLevel(); 
		// Not really used except below
		for (int i = 1; i < table.length; i++) {
			DistributionFactory distributionFactory = DistributionFactory.newInstance();
			ChiSquaredDistribution chiSquaredDistribution = distributionFactory.createChiSquareDistribution(i);
			try {
				table[i] = chiSquaredDistribution.inverseCumulativeProbability(table[0]);
			} catch (MathException e) {
				e.printStackTrace();
			}
		}
		m_ChiSquareInvProb = table;
	}

	public ClusErrorList createErrorMeasure(MultiScore score) {
		ClusErrorList parent = new ClusErrorList(this);
		NumericAttrType[] num = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		TimeSeriesAttrType[] ts = m_Schema.getTimeSeriesAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			parent.addError(new ContingencyTable(parent, nom));
		}
		if (num.length != 0) {
			parent.addError(new AbsoluteError(parent, num));
			parent.addError(new RMSError(parent, num));
			if (getSettings().hasNonTrivialWeights()) {
				parent.addError(new RMSError(parent, num, m_NormalizationWeights));
			}
			parent.addError(new PearsonCorrelation(parent, num));
		}
		if (ts.length != 0) {
			parent.addError(new QDMRMSError(parent, ts));
		}
		switch (m_Mode) {
			case MODE_HIERARCHICAL:
				parent.addError(new HierClassWiseAccuracy(parent, m_Hier));
				break;
			case MODE_ILEVELC:
				NominalAttrType cls = (NominalAttrType)getSchema().getAttrType(getSchema().getNbAttributes()-1);
				parent.addError(new ILevelCRandIndex(parent, cls));
				break;
		}
		return parent;
	}

	public ClusErrorList createEvalError() {
		ClusErrorList parent = new ClusErrorList(this);
		NumericAttrType[] num = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		TimeSeriesAttrType[] ts = m_Schema.getTimeSeriesAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			parent.addError(new Accuracy(parent, nom));
		}
		if (num.length != 0) {
			parent.addError(new RMSError(parent, num));
		}
		if (ts.length != 0) {
			parent.addError(new QDMRMSError(parent, ts));
		}
		return parent;
	}

	public ClusErrorList createDefaultError() {
		ClusErrorList parent = new ClusErrorList(this);
		NumericAttrType[] num = m_Schema
				.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = m_Schema
				.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			parent.addError(new MisclassificationError(parent, nom));
		}
		if (num.length != 0) {
			parent.addError(new RMSError(parent, num));
		}
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			parent.addError(new HierClassWiseAccuracy(parent, m_Hier));
			break;
		}
		return parent;
	}

	// additive and weighted targets
	public ClusErrorList createAdditiveError() {
		ClusErrorList parent = new ClusErrorList(this);
		NumericAttrType[] num = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			parent.addError(new MisclassificationError(parent, nom));
		}
		if (num.length != 0) {
			parent.addError(new MSError(parent, num));
		}
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			parent.addError(new HierClassWiseAccuracy(parent, m_Hier));
			break;
		case MODE_TIME_SERIES:
			TimeSeriesAttrType[] ts = m_Schema.getTimeSeriesAttrUse(ClusAttrType.ATTR_USE_TARGET);
			parent.addError(new QDMRMSError(parent, ts));
			break;
		}
		parent.setWeights(getClusteringWeights());
		return parent;
	}
	
	public ClusErrorList createExtraError(int train_err) {
		ClusErrorList parent = new ClusErrorList(this);
		if (m_Mode == MODE_TIME_SERIES) {
			parent.addError(new SSPDICVError(parent, new QDMTimeSeriesStat()));
		}
		return parent;
	}	

	public PruneTree getTreePrunerNoVSB() throws ClusException {
		Settings sett = getSettings();
		if (isBeamSearch() && sett.isBeamPostPrune()) {
			sett.setPruningMethod(Settings.PRUNING_METHOD_GAROFALAKIS);
			return new SizeConstraintPruning(sett.getBeamTreeMaxSize(),
                                             getClusteringWeights());
		}
		int err_nb = sett.getMaxErrorConstraintNumber();
		int size_nb = sett.getSizeConstraintPruningNumber();
		if (size_nb > 0 || err_nb > 0) {
			int[] sizes = sett.getSizeConstraintPruningVector();
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_CART_MAXSIZE) {
				return new CartPruning(sizes, getClusteringWeights());
			} else {
				sett.setPruningMethod(Settings.PRUNING_METHOD_GAROFALAKIS);
				SizeConstraintPruning sc_prune = new SizeConstraintPruning(sizes, getClusteringWeights());
				if (err_nb > 0) {
					double[] max_err = sett.getMaxErrorConstraintVector();
					sc_prune.setMaxError(max_err);
					sc_prune.setErrorMeasure(createDefaultError());
				}
				if (m_Mode == MODE_TIME_SERIES) {
					sc_prune.setAdditiveError(createAdditiveError());
				}
				return sc_prune;
			}
		}
		INIFileNominalOrDoubleOrVector class_thr = sett.getClassificationThresholds();
		if (class_thr.hasVector()) {
			return new HierClassTresholdPruner(class_thr.getDoubleVector());
		}
		if (m_Mode == MODE_REGRESSION) {
			double mult = sett.getM5PruningMult();
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_M5_MULTI) {
				return new M5PrunerMulti(getClusteringWeights(), mult);
			}
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_DEFAULT
					|| sett.getPruningMethod() == Settings.PRUNING_METHOD_M5) {
				sett.setPruningMethod(Settings.PRUNING_METHOD_M5);
				return new M5Pruner(getClusteringWeights(), mult);
			}
		} else if (m_Mode == MODE_CLASSIFY) {			
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_DEFAULT
					|| sett.getPruningMethod() == Settings.PRUNING_METHOD_C45) {
				sett.setPruningMethod(Settings.PRUNING_METHOD_C45);
				return new C45Pruner();
			}
		} else if (m_Mode == MODE_HIERARCHICAL) {
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_M5) {
				double mult = sett.getM5PruningMult();
				return new M5Pruner(m_NormalizationWeights, mult);
			}
		}
		sett.setPruningMethod(Settings.PRUNING_METHOD_NONE);
		return new PruneTree();
	}

	public PruneTree getTreePruner(ClusData pruneset) throws ClusException {
		Settings sett = getSettings();
		int pm = sett.getPruningMethod();
		if (pm == Settings.PRUNING_METHOD_NONE) {
			// Don't prune if pruning method is set to none, even if validation
			// set is given
			return new PruneTree();
		}
		if (m_Mode == MODE_HIERARCHICAL && pruneset != null) {
			PruneTree pruner = getTreePrunerNoVSB();
			boolean bonf = sett.isUseBonferroni();
			HierRemoveInsigClasses hierpruner = new HierRemoveInsigClasses(
					pruneset, pruner, bonf, m_Hier);
			hierpruner.setSignificance(sett.isHierPruneInSig());
			hierpruner.setNoRootPreds(sett.isHierNoRootPreds());
			sett.setPruningMethod(Settings.PRUNING_METHOD_DEFAULT);
			return hierpruner;
		}
		if (pruneset != null) {
			if (pm == Settings.PRUNING_METHOD_GAROFALAKIS_VSB
					|| pm == Settings.PRUNING_METHOD_CART_VSB) {
				ClusErrorList parent = createAdditiveError();
				SequencePruningVSB pruner = new SequencePruningVSB(
						(RowData) pruneset, parent, getClusteringWeights());
				if (pm == Settings.PRUNING_METHOD_GAROFALAKIS_VSB) {
					int maxsize = sett.getMaxSize();
					pruner.setSequencePruner(new SizeConstraintPruning(maxsize,
							getClusteringWeights()));
				} else {
					pruner.setSequencePruner(new CartPruning(
							getClusteringWeights()));
				}
				pruner.setOutputFile(sett.getFileAbsolute("prune.dat"));
				pruner.set1SERule(sett.get1SERule());
				pruner.setHasMissing(m_Schema.hasMissing());
				return pruner;
			} else if (pm == Settings.PRUNING_METHOD_REDERR_VSB
					|| pm == Settings.PRUNING_METHOD_DEFAULT) {
				ClusErrorList parent = createEvalError();
				sett.setPruningMethod(Settings.PRUNING_METHOD_REDERR_VSB);
				return new BottomUpPruningVSB(parent, (RowData) pruneset);
			} else {
				return getTreePrunerNoVSB();
			}
		} else {
			return getTreePrunerNoVSB();
		}
	}

	public void setTargetStatistic(ClusStatistic stat) {
		System.out.println("Setting target statistic: "
				+ stat.getClass().getName());
		m_StatisticAttrUse[ClusAttrType.ATTR_USE_TARGET] = stat;
	}

	public void setClusteringStatistic(ClusStatistic stat) {
		System.out.println("Setting clustering statistic: "
				+ stat.getClass().getName());
		m_StatisticAttrUse[ClusAttrType.ATTR_USE_CLUSTERING] = stat;
	}

	public boolean hasClusteringStat() {
		return m_StatisticAttrUse[ClusAttrType.ATTR_USE_CLUSTERING] != null;
	}

	public ClusStatistic createClusteringStat() {
		return m_StatisticAttrUse[ClusAttrType.ATTR_USE_CLUSTERING].cloneStat();
	}

	public ClusStatistic createTargetStat() {
		return m_StatisticAttrUse[ClusAttrType.ATTR_USE_TARGET].cloneStat();
	}

	/**
	 * @param attType
	 *            attribute use type (eg., ClusAttrType.ATTR_USE_TARGET)
	 * @return the statistic
	 */

	public ClusStatistic createStatistic(int attType) {
		return m_StatisticAttrUse[attType].cloneStat();
	}

	/**
	 * @param attType
	 *            attribute use type (eg., ClusAttrType.ATTR_USE_TARGET)
	 * @return the statistic
	 */
	public ClusStatistic getStatistic(int attType) {
		return m_StatisticAttrUse[attType];
	}

	public ClusStatistic getStatistic() {
		return m_TargetStatistic;
	}

	public ClusHeuristic getHeuristic() {
		return m_Heuristic;
	}

	public String getHeuristicName() {
		return m_Heuristic.getName();
	}

	public void getPreprocs(DataPreprocs pps) {
	}

	public boolean needsHierarchyProcessors() {
		if (m_Mode == MODE_SSPD)
			return false;
		else
			return true;
	}

	public void setRuleInduce(boolean rule) {
		m_RuleInduce = rule;
	}

	public boolean isRuleInduce() {
		return m_RuleInduce;
	}

	public void setBeamSearch(boolean beam) {
		m_BeamSearch = beam;
	}

	public boolean isBeamSearch() {
		return m_BeamSearch;
	}

	/**
	 * @return Returns the ChiSquare inverse probability for specified
	 *         significance level and degrees of freedom.
	 */
	public double getChiSquareInvProb(int df) {
		return m_ChiSquareInvProb[df];
	}

	public void updateStatistics(ClusModel model) throws ClusException {
		if (m_Hier != null) {
			ArrayList stats = new ArrayList();
			model.retrieveStatistics(stats);
			for (int i = 0; i < stats.size(); i++) {
				WHTDStatistic stat = (WHTDStatistic) stats.get(i);
				stat.setHier(m_Hier);
			}
		}
	}

	private void createHierarchy() {
		int idx = 0;
		for (int i = 0; i < m_Schema.getNbAttributes(); i++) {
			ClusAttrType type = m_Schema.getAttrType(i);
			if (!type.isDisabled() && type instanceof ClassesAttrType) {
				ClassesAttrType cltype = (ClassesAttrType) type;
				System.out.println("Classes type: " + type.getName());
				if (idx == 0)
					m_HierN = cltype.getHier();
				else
					m_HierF = cltype.getHier();
				idx++;
			}
		}
		if (Settings.HIER_FLAT.getValue())
			m_Hier = m_HierF;
		else
			m_Hier = m_HierN;
	}

	public void initHierarchySettings() throws ClusException, IOException {
		if (m_Hier != null) {
			if (getSettings().hasHierEvalClasses()) {
				ClassesTuple tuple = ClassesTuple.readFromFile(getSettings()
						.getHierEvalClasses(), m_Hier);
				m_Hier.setEvalClasses(tuple);
			}
		}
	}

	/**
	 * Initializes/checks/overrides some inter-dependent settings for rule
	 * induction.
	 * 
	 * @throws ClusException
	 * 
	 */
	public void initRuleSettings() throws ClusException {
		Settings sett = getSettings();
		int covering = sett.getCoveringMethod();
		int prediction = sett.getRulePredictionMethod();
		// General
		if (((sett.getHeuristic() != Settings.HEURISTIC_DISPERSION_ADT)
				|| (sett.getHeuristic() != Settings.HEURISTIC_DISPERSION_MLT)
				|| (sett.getHeuristic() != Settings.HEURISTIC_WR_DISPERSION_ADT) || (sett
				.getHeuristic() != Settings.HEURISTIC_WR_DISPERSION_MLT))
				&& sett.isCompHeurRuleDist()) {
			sett.setCompHeurRuleDistPar(0.0);
		}
		if (sett.isRuleSignificanceTesting()) { // Is this faster than calling
			// isRuleSignificanceTesting()
			Settings.IS_RULE_SIG_TESTING = true; // from Compactness
			// heuristic each time?
		}
		// Random rules
		if (sett.isRandomRules()) {
			sett.setCoveringMethod(Settings.COVERING_METHOD_STANDARD);
			// sett.setRulePredictionMethod(Settings.RULE_PREDICTION_METHOD_DECISION_LIST);
			// ???
			sett.setCoveringWeight(0);
			// Ordered rules
		} else if (covering == Settings.COVERING_METHOD_STANDARD) {
			// sett.setRulePredictionMethod(Settings.RULE_PREDICTION_METHOD_DECISION_LIST);
			sett.setCoveringWeight(0);
			// Unordered rules - Heuristic covering
		} else if (covering == Settings.COVERING_METHOD_HEURISTIC_ONLY) {
			if ((prediction == Settings.RULE_PREDICTION_METHOD_DECISION_LIST)
					|| (prediction == Settings.RULE_PREDICTION_METHOD_UNION)) {
				sett
						.setRulePredictionMethod(Settings.RULE_PREDICTION_METHOD_COVERAGE_WEIGHTED);
			}
			sett.setCoveringWeight(0.0);
			if (getSettings().getCompHeurRuleDistPar() < 0) {
				throw new ClusException("Clus heuristic covering: CompHeurRuleDistPar must be >= 0!");
			}
			if ((sett.getHeuristic() != Settings.HEURISTIC_DISPERSION_ADT)
					|| (sett.getHeuristic() != Settings.HEURISTIC_DISPERSION_MLT)
					|| (sett.getHeuristic() != Settings.HEURISTIC_WR_DISPERSION_ADT)
					|| (sett.getHeuristic() != Settings.HEURISTIC_WR_DISPERSION_MLT)) {
				throw new ClusException(
						"Clus heuristic covering: Only Dispersion-based heuristics supported!");
			}
			// Unordered rules - Weighted coverings
		} else if ((covering == Settings.COVERING_METHOD_WEIGHTED_ADDITIVE)
				|| (covering == Settings.COVERING_METHOD_WEIGHTED_MULTIPLICATIVE)
				|| (covering == Settings.COVERING_METHOD_WEIGHTED_ERROR)
				|| (covering == Settings.COVERING_METHOD_BEAM_RULE_DEF_SET)
				|| (covering == Settings.COVERING_METHOD_RANDOM_RULE_SET)) {
			if ((prediction == Settings.RULE_PREDICTION_METHOD_DECISION_LIST)
					|| (prediction == Settings.RULE_PREDICTION_METHOD_UNION)) {
				sett
						.setRulePredictionMethod(Settings.RULE_PREDICTION_METHOD_COVERAGE_WEIGHTED);
			}
			if (sett.getCoveringWeight() < 0) {
				throw new ClusException(
						"Clus weighted covering: Covering weight must be >= 0!");
			}
			// Rule induction from bootstrap sampled data, optimized ...
		} else if (covering == Settings.COVERING_METHOD_STANDARD_BOOTSTRAP) {
			sett
					.setRulePredictionMethod(Settings.RULE_PREDICTION_METHOD_OPTIMIZED);
			// Multi-label classification
		} else if (covering == Settings.COVERING_METHOD_UNION) {
			sett.setRulePredictionMethod(Settings.RULE_PREDICTION_METHOD_UNION);
		}
	}
}
