package clus.model.processor;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.*;
import clus.util.*;

import jeans.util.*;

import java.io.*;

public class ModelProcessorCollection extends MyArray {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public final void addModelProcessor(ClusModelProcessor proc) {
		addElement(proc);
	}
	
	public final boolean addCheckModelProcessor(ClusModelProcessor proc) {
		// only add model processor if not yet in list
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc2 = (ClusModelProcessor)elementAt(j);
			if (proc == proc2) return false;
		}		
		addElement(proc);
		return true;
	}	
	
	public final void initialize(ClusModel model, ClusSchema schema) throws IOException, ClusException {
		if (model != null) {
			for (int i = 0; i < size(); i++) {
				ClusModelProcessor proc = (ClusModelProcessor)elementAt(i);
				proc.initialize(model, schema);
			}
		}
	}
	
	public final void initializeAll(ClusSchema schema) throws IOException, ClusException {
		for (int i = 0; i < size(); i++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(i);
			proc.initializeAll(schema);
		}
	}	
	
	public final void terminate(ClusModel model) throws IOException {
		if (model != null) {
			for (int i = 0; i < size(); i++) {
				ClusModelProcessor proc = (ClusModelProcessor)elementAt(i);
				proc.terminate(model);
			}
		}
	}
	
	public final void terminateAll() throws IOException {
		for (int i = 0; i < size(); i++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(i);
			proc.terminateAll();
		}
	}	
	
	public final void modelDone() throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			proc.modelDone();
		}
	}
	
	public final void exampleUpdate(DataTuple tuple) throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			proc.exampleUpdate(tuple);
		}	
	}

	public final void exampleDone() throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			proc.exampleDone();
		}	
	}
	
	public final void exampleUpdate(DataTuple tuple, ClusStatistic distr) throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			proc.exampleUpdate(tuple, distr);
		}	
	}
	
	public final boolean needsModelUpdate() throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			if (proc.needsModelUpdate()) return true;
		}
		return false;
	}	
	
	public final ClusModelProcessor getModelProcessor(int i) {
		return (ClusModelProcessor)elementAt(i);
	}	
}
