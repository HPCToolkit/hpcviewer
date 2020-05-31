////
//ExperimentBuilder2.java						//
////
//$Id$	//
////
//(c) Copyright 2002-2012 Rice University. All rights reserved.	//
////

package edu.rice.cs.hpc.data.experiment.xml;

import edu.rice.cs.hpc.data.experiment.*;
import edu.rice.cs.hpc.data.experiment.metric.*;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric.AnnotationType;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric.VisibilityType;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.experiment.xml.Token2.TokenXML;
import edu.rice.cs.hpc.data.util.IUserData;

import java.util.List;
import java.util.ArrayList;

/**
 *
 * Builder for an XML parser for HPCViewer experiment files.
 * 
 * This builder will oversee the association between CCT and metrics
 * 
 * @see BaseExperimentBuilder
 */
public class ExperimentBuilder2 extends BaseExperimentBuilder
{
	/** The parsed metric objects. */
	private List<BaseMetric> metricList;
	private List<MetricRaw> metricRawList;

	/** Maximum number of metrics provided by the experiment file.
    We use the maxNumberOfMetrics value to generate short names for the self metrics*/
	final private int maxNumberOfMetrics = 10000;

	final private ArrayList<DerivedMetric> listOfDerivedMetrics;
	/**
	 * 
	 * Creating experiment with metrics
	 * 
	 * @see BaseExperimentBuilder.BaseExperimentBuilder
	 * 
	 * @param need_metrics: do we need metrics ?
	 */
	public ExperimentBuilder2(BaseExperiment experiment, String defaultName, IUserData<String, String> userData) {
		
		super(experiment, defaultName, userData);
		this.metricList = new ArrayList<BaseMetric>();
		
		listOfDerivedMetrics   = new ArrayList<DerivedMetric>(2);
		
		setRemoveInvisibleProcedure(true);
	}


    //====================================== PARSING SEMANTICS ==================//
	
	/*************************************************************************
	 * parsing the beginning of XML element 
	 *************************************************************************/
	public void beginElement(String element, String[] attributes, String[] values) {
		
		TokenXML current = Token2.map(element);

		switch(current)
		{
		case T_NAME_VALUE:
			this.do_NV(attributes, values);
			break;
			
		case T_METRIC_TABLE:
			break;
			
		case T_METRIC_DB_TABLE:
			this.begin_MetricRawTable();
			break;
		case T_METRIC_DB:
			this.do_MetricRaw(attributes, values);
			break;

		case T_METRIC_FORMULA:
			this.do_MetricFormula(attributes, values);
			break;
			
		case T_M:
			this.do_M     (attributes, values);	break;

		case T_METRIC:
			this.do_METRIC(attributes, values);	break;
			
		default:
			super.beginElement(element, attributes, values);
		}
		saveTokenContext(current);
	}

	/*************************************************************************
	 *	Takes notice of the ending of an element.
	 ************************************************************************/
	public void endElement(String element)
	{
		TokenXML current = Token2.map(element);
		switch(current)
		{
		// Data elements
		case T_CALLPATH_PROFILE_DATA:	// @deprecated: semi old format. some data has this kind of tag
		case T_SEC_FLAT_PROFILE_DATA:
		case T_SEC_CALLPATH_PROFILE_DATA:
			this.end_ProfileData();
			// ok, this is ugly: we force the parent class to treat the end of this element
			super.endElement(element);
			break;
			
		case T_METRIC_TABLE:
			this.end_MetricTable();
			break;

		case T_METRIC_DB_TABLE:
			this.end_MetricRawTable();
			break;

			// ignored elements
			// trace database
		case T_TRACE_DB:
		case T_METRIC_DB:
		case T_M:
			break;
		default:
			super.endElement(element);
		break;
		} 
	}


//	------------------------------- BUILDING		---------------------------//

	/*************************************************************************
	 *	Finishes processing a profile element.
	 ************************************************************************/
	private void end_ProfileData()
	{
		boolean is_raw_metric_only = true;
		final BaseExperimentWithMetrics exp = (BaseExperimentWithMetrics) experiment;

		final int nbMetrics = this.metricList.size();
		
		// ----------------------------------------------------------------------------
		// check if all metrics are metric raw.
		// it is unlikely that hpcprof will mix between metric raw and other type of metric,
		// but we cannot trust hpcprof to do properly
		// ----------------------------------------------------------------------------
		for (int i=0; i<nbMetrics; i++) {
			BaseMetric objMetric = this.metricList.get(i);
			is_raw_metric_only &= (objMetric instanceof Metric);
		}
		
		if (is_raw_metric_only)
		{
			// ----------------------------------------------------------------------------	
			// need to reorder the metrics: instead of 0, 10000, 1, 10001, ....
			// 	it should be: 0, 1, 2, 3, ....
			// our xml reader is based on old xml where the number of metrics is unknown
			// since now we know this number, we should change the algo 
			//	to adapt with the new one instead of hacking
			// ----------------------------------------------------------------------------	
			for (int i=0; i<nbMetrics; i++) 
			{
				Metric objMetric = (Metric) this.metricList.get(i);
				objMetric.setIndex(i);
				
				// reset the short name. short name is the key used by formula
				objMetric.setShortName(String.valueOf(i));
				
				// reset the partner: inclusive's partner is exclusive, and vice versa
				final int partner = (objMetric.getMetricType() == MetricType.EXCLUSIVE) ? i-1: i+1;
				objMetric.setPartner(partner);
			}
			// notify the experiment object that we have reordered metric index
			exp.setMetrics(metricList);
		}
	}

	static private final char FORMULA_TYPE 		 = 't';
	static private final char FORMULA_EXPRESSION = 'f';
	static private final char FORMULA_FOR_VIEWER = 'v';
	
	/***
	 * create a derived metric from a base metric
	 * @param metric
	 * @param formula
	 * @return
	 */
	private DerivedMetric createDerivedMetric(BaseMetric metric, String formula)
	{
		
		DerivedMetric dm = new DerivedMetric(metric.getDisplayName(), metric.getShortName(), 
				metric.getIndex(), metric.getAnnotationType(),
				metric.getMetricType());

		dm.setDescription  (DerivedMetric.DESCRIPTION + ": " + metric.getDescription());
		dm.setExpression   (formula);
		dm.setOrder        (metric.getOrder());
		dm.setDisplayFormat(metric.getDisplayFormat());
		dm.setDisplayed	   (metric.getVisibility()); // fix issue #63
		dm.setPartner	   (metric.getPartner());

		listOfDerivedMetrics.add(dm);
		
		return dm;
	}

	/*************************************************************************
	 *	Processes a METRICFORMULA element.
	 *     <!-- MetricFormula represents derived metrics: (t)ype; (frm): formula -->
    <!ELEMENT MetricFormula (Info?)>
    <!ATTLIST MetricFormula
              t   (combine|finalize) "finalize"
              frm CDATA #REQUIRED>
	 ************************************************************************/
	private void do_MetricFormula(String[] attributes, String[] values) 
	{	
		char formula_type = '\0';
		int nbMetrics= this.metricList.size();
		
		for (int i=0; i<attributes.length; i++) {
			if (attributes[i].charAt(0) == FORMULA_TYPE) {
				// type of formula
				formula_type = values[i].charAt(0);
				
			} else if (attributes[i].charAt(0) == FORMULA_EXPRESSION) {
				// formula
				assert (formula_type != '\0');
				BaseMetric objMetric = this.metricList.get(nbMetrics-1);
				
				// corner case: hpcrun derived metric
				if (formula_type == FORMULA_FOR_VIEWER) {
					
					DerivedMetric dm = createDerivedMetric(objMetric, values[i]);
					
					// replace the current metric with the new derived metric
					metricList.set(nbMetrics-1, dm);
					
					if (objMetric instanceof Metric && nbMetrics>1) {
						// raw metric requires a partner. Let's make the derived metric of the partner
						objMetric = metricList.get(nbMetrics - 2);
						
						dm = createDerivedMetric(objMetric, values[i]);
						metricList.set(nbMetrics - 2, dm);
					}
				}
				
				if (objMetric instanceof AggregateMetric) {
					( (AggregateMetric)objMetric).setFormula(formula_type, values[i]);
				
				} else if (objMetric instanceof DerivedMetric) {
					DerivedMetric derived_metric = (DerivedMetric) objMetric;
					derived_metric.setExpression(values[i]);
				}
			}
		}
	}
	
	
	private enum MetricValueDesc {Raw, Final, Derived_Incr, Derived}
	
	/*************************************************************************
	 *	Processes a METRIC element.
	 *  <!ELEMENT Metric (MetricFormula?, Info?)>
        <!ATTLIST Metric
	      i    CDATA #REQUIRED
	      n    CDATA #REQUIRED
	      v    (raw|final|derived-incr|derived) "raw"
	      t    (inclusive|exclusive|nil) "nil"
	      fmt  CDATA #IMPLIED
	      show (1|0) "1">
	 ************************************************************************/
	private void do_METRIC(String[] attributes, String[] values)
	{	
		final char ATTRIBUTE_NAME    = 'n';
		final char ATTRIBUTE_INDEX   = 'i';
		final char ATTRIBUTE_VALUE   = 'v';
		final char ATTRIBUTE_TYPE    = 't';
		final char ATTRIBUTE_FORMAT  = 'f';
		final char ATTRIBUTE_SHOW    = 's';
		final char ATTRIBUTE_PARTNER = 'p';
		final char ATTRIBUTE_EVENT   = 'e';
		final char ATTRIBUTE_ORDER   = 'o';
		
		final char ATTRIBUTE_METRIC_EXT      = 'm';
		final char ATTRIBUTE_METRIC_EXT_DESC = 'd';
		
		int nbMetrics = this.metricList.size();
		int iSelf = -1;
		int partner = 0;	// 2010.06.28: new feature to add partner
		int order = -1;
		
		String sID = null;// = values[nID];
		String sDisplayName = null;
		String sNativeName  = null;
		String sDescription = null;
		
		AnnotationType percent = AnnotationType.NONE;
		MetricType objType = MetricType.UNKNOWN;

		boolean needPartner = isCallingContextTree();
		BaseMetric.VisibilityType visibility = VisibilityType.SHOW;
		
		MetricValueDesc mDesc = MetricValueDesc.Raw; // by default is a raw metric
		String format = null;
		
		for (int i=0; i<attributes.length; i++) {
			if (attributes[i].charAt(0) == ATTRIBUTE_INDEX) {
				// id ?
				sID = values[i];
				// somehow, the ID of the metric is not number, but asterisk
				if (sID.charAt(0) == '*') {
					// parsing an asterisk can throw an exception, which is annoying
					// so we make an artificial ID for this particular case
					iSelf = nbMetrics;
					if (isCallingContextTree()) 
						iSelf = nbMetrics/2;
				} else {
					iSelf = Integer.parseInt(sID);
				}
			} else if (attributes[i].charAt(0) == ATTRIBUTE_NAME) {
				// name ?
				sNativeName = values[i];
			} else if (attributes[i].charAt(0) == ATTRIBUTE_METRIC_EXT) {
				if (attributes[i].charAt(1) == ATTRIBUTE_METRIC_EXT_DESC) {
					sDescription = values[i];
				}
				
			} else if (attributes[i].charAt(0) == ATTRIBUTE_VALUE) {
				// value: raw|final|derived-incr|derived
				if (values[i].equals("final")) {
					mDesc = MetricValueDesc.Final;
					needPartner = false;
				} else if (values[i].equals("derived-incr")) {
					mDesc = MetricValueDesc.Derived_Incr;
					needPartner = false;
				} else if (values[i].equals("derived")) {
					mDesc = MetricValueDesc.Derived;
				} else if (values[i].equals("formula")) {
					mDesc = MetricValueDesc.Derived;
					needPartner = false;
				}
			} else if (attributes[i].charAt(0) == ATTRIBUTE_TYPE) {
				// type: inclusive|exclusive|nil
				objType = getMetricType(values[i]);

			} else if (attributes[i].charAt(0) == ATTRIBUTE_FORMAT) {
				// format to display
				format = values[i];
				
			} else if (attributes[i].equals("show-percent")) {
				if (values[i].charAt(0) == '1') {
					percent = AnnotationType.PERCENT;
				} else {
					percent = AnnotationType.NONE;
				}
			} else if (attributes[i].charAt(0) == ATTRIBUTE_SHOW) {
				// show or not ? 1=yes, 0=no
				int visVal = Integer.valueOf(values[i]);
				visibility = BaseMetric.convertToVisibilityType(visVal);
			} else if (attributes[i].charAt(0) == ATTRIBUTE_PARTNER) {
				// partner
				partner = Integer.valueOf( values[i] );
			} else if (attributes[i].charAt(0) == ATTRIBUTE_ORDER) {
				order = Integer.parseInt(values[i]);
				
			} else if (attributes[i].charAt(0) == ATTRIBUTE_EVENT) {
				//  ep: period mean
				//  em: is the event multiplexed
				//  es: number of total samples
			}
		}
		
		// Laks 2009.01.14: if the database is call path database, then we need
		//	to distinguish between exclusive and inclusive
		if (needPartner) {
			sDisplayName = sNativeName + " (I)";
			objType = MetricType.INCLUSIVE;
			partner = this.maxNumberOfMetrics + iSelf;
		} else {
			// this metric is not for inclusive, the display name should be the same as the native one
			sDisplayName = sNativeName;
		}
		
		// set the metric
		BaseMetric metricInc;
		switch (mDesc) {
			case Final:
				metricInc = new FinalMetric(
						String.valueOf(iSelf),		 // short name
						sDescription,				 // native name
						sDisplayName, 				 // display name
						visibility, format, percent, // displayed ? percent ?
						"",							 // period (not defined at the moment)
						nbMetrics, objType, partner);
				break;
			case Derived_Incr:
				metricInc = new AggregateMetric(sID, sDisplayName, sDescription,
									visibility, format, percent, nbMetrics, partner, objType);
				((AggregateMetric) metricInc).init( (BaseExperimentWithMetrics) this.experiment );
				break;
			case Derived:
				metricInc = new DerivedMetric(sDisplayName, sID, nbMetrics, percent, objType);
				
				metricInc.setPartner(partner);
				metricInc.setOrder  (order);
				
				listOfDerivedMetrics.add( (DerivedMetric) metricInc);
				break;
			case Raw:
			default:
				metricInc = new Metric(
						String.valueOf(iSelf),			// short name
						sNativeName,			// native name
						sDisplayName, 	// display name
						visibility, format, percent, 			// displayed ? percent ?
						"",						// period (not defined at the moment)
						nbMetrics, objType, partner);
				break;
		}
		metricInc.setDescription(sDescription);
		metricInc.setOrder(order);
		
		this.metricList.add(metricInc);

		// ----------------------------------------------------------------------------
		// if the XML file only provides one type of metric (i.e. exclusive metric),
		// we should create the pair <inclusive, exclusive> manually
		// 	this only happens if the metric is "raw metric"
		// ----------------------------------------------------------------------------
		if (needPartner) {
			// set the exclusive metric
			String sSelfName = String.valueOf(partner);	// I am the partner of the inclusive metric
			// Laks 2009.02.09: bug fix for not reusing the existing inclusive display name
			String sSelfDisplayName = sNativeName + " (E)";
			Metric metricExc = new Metric(
					sSelfName,			// short name
					sDescription,		// metric description
					sSelfDisplayName, 	// display name
					visibility, format, AnnotationType.PERCENT, 		// displayed ? percent ?
					"",					// period (not defined at the moment)
					nbMetrics+1, MetricType.EXCLUSIVE, nbMetrics);
			this.metricList.add(metricExc);
		}
	}


	/*******
	 * handling metric db
	 * @param attributes
	 * @param values
	 */
	private void do_MetricRaw(String[] attributes, String[] values)
	{
		int ID = 0;
		String title = null;
		String db_glob = null;
		int db_id = 0;
		int num_metrics = 0, partner_index = -1;
		MetricType type = MetricType.UNKNOWN;
		
		for (int i=0; i<attributes.length; i++) {
			if (attributes[i].charAt(0) == 'i') {
				ID = Integer.valueOf(values[i]);
			} else if (attributes[i].charAt(0) == 'n') {
				title = values[i];
			} else if (attributes[i].charAt(0) == 't') {
				// type of metric
				type = getMetricType(values[i]);
			} else if (attributes[i].charAt(0) == 'p') {
				// partner of this metric
				partner_index = Integer.valueOf(values[i]);
			} else if (attributes[i].equals("db-glob")) {
				db_glob = values[i];
			} else if (attributes[i].equals("db-id")) {
				db_id = Integer.valueOf(values[i]);
			} else if (attributes[i].equals("db-num-metrics")) {
				num_metrics = Integer.valueOf(values[i]);
			}
		}
		
		MetricRaw metric = new MetricRaw(ID, title, title, db_glob, db_id, 
				partner_index, type, num_metrics);
		this.metricRawList.add(db_id, metric);
	}
	


	/*************************************************************************
	 * finishes processing metric table
	 *************************************************************************/
	private void end_MetricTable() {
		final BaseExperimentWithMetrics exp = (BaseExperimentWithMetrics) experiment;
		
		exp.setMetrics(metricList);
	}

	 	
	/*************************************************************************
	 *	Processes an M (metric value) element.
	 ************************************************************************/

	private void do_M(String[] attributes, String[] values)
	{		
		// m n="abc" v="4.56e7"
		
		final BaseExperimentWithMetrics exp = (BaseExperimentWithMetrics) experiment;
		
		// add a metric value to the current scope
		String internalName = getAttributeByName(ATTRIBUTE_NAME, attributes, values);
		String value = getAttributeByName(ATTRIBUTE_VALUE, attributes, values);
		
		Double dblValue = null;
		try {
			dblValue = Double.valueOf(value);
		} catch (NumberFormatException e) {
			// if the value of metric cannot be determined, we consider as "nan" (not a number)
			dblValue = Double.NaN;
		}
		double actualValue  = dblValue.doubleValue();
		
		BaseMetric metric = exp.getMetric(internalName);
		// get the sample period
		double prd = metric.getSamplePeriod();

		// multiple by sample period 
		actualValue = prd * actualValue;
		MetricValue metricValue = new MetricValue(actualValue);
		Scope objCurrentScope = this.getCurrentScope();
		
		objCurrentScope.setMetricValue(metric.getIndex(), metricValue);

		// update also the self metric value for calling context only
		if (metric.getMetricType() == MetricType.INCLUSIVE) {

			//----------------------------------------------------------------------------
			// Final metric (inherited from Metric) doesn't need partner. It is final.
			//----------------------------------------------------------------------------
			if (!(metric instanceof FinalMetric) && metric instanceof Metric) {
				int partner = ( (Metric) metric).getPartner();
				String selfShortName = String.valueOf(partner);

				BaseMetric selfMetric = exp.getMetric(selfShortName); 
				MetricValue selfMetricValue = new MetricValue(actualValue);
				objCurrentScope.setMetricValue(selfMetric.getIndex(), selfMetricValue);  
			}
		}
	}



	/**
	 * Enumeration of different states of info
	 * @author laksonoadhianto
	 *
	 */
	private enum InfoState { PERIOD, UNIT, FLAG, AGGREGATE, NULL };
	/************************************************************************
	 * Laks: special treatement when NV is called under INFO
	 * @param attributes
	 * @param values
	 ************************************************************************/
	private void do_NV(String[] attributes, String[] values) {
		
		if ( (this.elemInfoState == TokenXML.T_METRIC) || (this.elemInfoState == TokenXML.T_METRIC_FORMULA)){
			InfoState iState = InfoState.NULL;
			// previous state is metric. The attribute should be about periodicity or unit
			for (int i=0; i<attributes.length; i++) {
				
				if (attributes[i].charAt(0) == 'n') {
					// name of the info
					if ( values[i].charAt(0) == 'p' ) // period
						iState = InfoState.PERIOD;
					else if ( values[i].charAt(0) == 'u' ) // unit
						iState = InfoState.UNIT;
					else if ( values[i].charAt(0) == 'f' ) // flag
						iState = InfoState.FLAG;
					else if ( values[i].charAt(0) == 'a' || values[i].charAt(0) == 'c') // aggregate
						iState = InfoState.AGGREGATE;
					
				} else if ( attributes[i].charAt(0) == 'v' ) {
					
					int nbMetrics= this.metricList.size();
					// value of the info
					switch (iState) {
					case PERIOD:
						String sPeriod = values[i];
						if(nbMetrics > 1) {
							// get the current metric (inc)
							BaseMetric metric = this.metricList.get(nbMetrics-1);
							metric.setSamplePeriod(sPeriod);
							// get the current metric (exc)
							metric = this.metricList.get(nbMetrics-2);
							metric.setSamplePeriod(sPeriod);
							
						}
						break;
						
					case UNIT:
						if(nbMetrics > 0) {
							// get the current metric (inc)
							BaseMetric metric = this.metricList.get(nbMetrics-1);
							metric.setUnit( values[i] );
							if (!(metric instanceof AggregateMetric) && (nbMetrics>1)) {
								// get partner metric if the current metric is not aggregate metric
								metric = this.metricList.get(nbMetrics-2);
								metric.setUnit(values[i]);
							}
						}
						break;
						
					case AGGREGATE:
					case FLAG:
						// not used ?
						break;
					default:
						System.err.println("Warning: unrecognize info value state: "+iState);
						break;
					}
					// reinitialize the info state
					iState = InfoState.NULL;
				} else 
					System.err.println("Warning: incorrect XML info format: " + attributes[i]+" "+ values[i]);
			}
		}
		else
		{
			System.err.println("Warning: unknown NV from previous token: " + elemInfoState);
		}
	}
	
	private MetricType getMetricType(String value)
	{
		MetricType type = MetricType.EXCLUSIVE;
		
		if (value.charAt(0) == 'i')
			type = MetricType.INCLUSIVE;

		return type;
	}
	
	@Override
	public void end()
	{
		super.end();
		
		for(DerivedMetric metric: listOfDerivedMetrics) {
			metric.resetMetric((Experiment) experiment, viewRootScope);
		}
	}
	
	//--------------------------------------------------------------------------------
	// raw metric database
	//--------------------------------------------------------------------------------

	/******
	 * begin metric database
	 */
	private void begin_MetricRawTable() 
	{
		this.metricRawList = new ArrayList<MetricRaw>();
	}

	/***
	 * end metric database
	 */
	private void end_MetricRawTable() 
	{
		if (metricRawList != null && metricRawList.size()>0) {
			for (MetricRaw m : metricRawList) {
				int partner_index = m.getPartner();
				if (partner_index >= 0) {
					MetricRaw partner = metricRawList.get(partner_index);
					m.setMetricPartner(partner);
				}
			}
			MetricRaw[] metrics = new MetricRaw[metricRawList.size()];
			metricRawList.toArray( metrics );
			((Experiment) experiment).setMetricRaw( metrics );
		}
	}
}

