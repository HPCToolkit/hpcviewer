//////////////////////////////////////////////////////////////////////////
//																		//
//	MetricValue.java													//
//																		//
//	experiment.MetricValue -- a value of a metric at some scope			//
//	Last edited: September 14, 2001 at 4:47 pm							//
//																		//
//	(c) Copyright 2001 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment.metric;


import edu.rice.cs.hpc.data.util.*;




//////////////////////////////////////////////////////////////////////////
//	CLASS METRIC-VALUE													//
//////////////////////////////////////////////////////////////////////////

/**
 *
 * A value of a metric at some scope.
 *
 */


public final class MetricValue 
{

	/** The actual value if available. */
	protected float value;

	/** The annotation value if available. */
	protected float annotation;

	protected byte flags;

	protected static final byte VALUE_IS_AVAILABLE = 1;
	protected static final byte ANNOTATION_IS_AVAILABLE = 2;

	/** The distinguished metric value indicating no data. */
	public static final MetricValue NONE = new MetricValue(-1);




	//////////////////////////////////////////////////////////////////////////
	//	INITIALIZATION														//
	//////////////////////////////////////////////////////////////////////////




	/*************************************************************************
	 *	Creates an unavailable MetricValue.
	 ************************************************************************/

	public MetricValue()
	{
		setAvailable(this, false);
		setAnnotationAvailable(this, false);
	}




	/*************************************************************************
	 *	Creates an available MetricValue with a given value and annotation value.
	 ************************************************************************/

	public MetricValue(double value, double annotation)
	{
		setValue(this, value);
		setAnnotationValue(this, ((float)annotation));
	}




	/*************************************************************************
	 *	Creates an available MetricValue with a given value.
	 ************************************************************************/

	public MetricValue(double value)
	{
		setValue(this, value);
		setAvailable(this, true);
		setAnnotationAvailable(this, false);
	}



	//////////////////////////////////////////////////////////////////////////
	//	ACCESS TO VALUE														//
	//////////////////////////////////////////////////////////////////////////

	/*************************************************************************
	 *	Returns whether the actual value is available.
	 ************************************************************************/

	private static boolean getAvailable(MetricValue m)
	{
		boolean available = (m.flags & VALUE_IS_AVAILABLE) == VALUE_IS_AVAILABLE;
		return available;
	}

	private static void setAvailable(MetricValue m, boolean status)
	{
		if (status) {
			m.flags |= VALUE_IS_AVAILABLE;
		} else {
			m.flags &= ~VALUE_IS_AVAILABLE;
		}	
	}


	private static boolean getAnnotationAvailable(MetricValue m)
	{
		boolean available = (m.flags & ANNOTATION_IS_AVAILABLE) == ANNOTATION_IS_AVAILABLE;
		return available;
	}

	public static void setAnnotationAvailable(MetricValue m, boolean status)
	{
		if (status) {
			m.flags |= ANNOTATION_IS_AVAILABLE;
		} else {
			m.flags &= ~ANNOTATION_IS_AVAILABLE;
		}	
	}


	public float getValue()
	{
		boolean available = (flags & VALUE_IS_AVAILABLE) == VALUE_IS_AVAILABLE;
		Dialogs.Assert(available, "MetricValue::getValue");
		return this.value;
	}

	/*************************************************************************
	 *	Returns whether the metric value is available.
	 ************************************************************************/

	public static boolean isAvailable(MetricValue m)
	{
		return ( (m != MetricValue.NONE) && getAvailable(m) && 
				Float.compare(m.value, 0)!=0 );
	}




	/*************************************************************************
	 *	Returns the actual value if available.
	 ************************************************************************/

	public static float getValue(MetricValue m)
	{
		return m.getValue();
	}




	/*************************************************************************
	 *	Makes the given actual value available.
	 ************************************************************************/

	public static void setValue(MetricValue m, double value)
	{
		setAvailable(m, true);
		m.value = (float) value;
	}




	/*************************************************************************
	 *	Returns whether the annotation value is available.
	 ************************************************************************/

	public static boolean isAnnotationAvailable(MetricValue m)
	{
		return (m != MetricValue.NONE) && getAnnotationAvailable(m);
	}




	/*************************************************************************
	 *	Returns the annotation value if available.
	 ************************************************************************/

	public static float getAnnotationValue(MetricValue m)
	{
		return m.annotation;
	}




	/*************************************************************************
	 *	Makes the given annotation value available.
	 ************************************************************************/

	public static void setAnnotationValue(MetricValue m, double annotation)
	{
		setAnnotationAvailable(m, true);
		m.annotation = (float) annotation;
	}


	public static void setAnnotationValue(MetricValue m, float annotation)
	{
		setAnnotationAvailable(m, true);
		m.annotation = annotation;
	}


	public static boolean isZero(MetricValue m) 
	{
		if (m != MetricValue.NONE) {
			return ( Double.compare(0.0, m.value) == 0 );
		}
		return true;
	}

	/*************************************************************************
	 *	Compares the metric value to another one.
	 *
	 *	Unavailable or nonexistent values are treated as less than available values.
	 *	Annotation values are ignored.
	 *
	 ************************************************************************/

	public static int compareTo(MetricValue left, MetricValue right)
	{
		int result;

		boolean availableLeft  = MetricValue.isAvailable(left);
		boolean availableRight = MetricValue.isAvailable(right);
		
		if( availableLeft && availableRight )
		{
			if( left.value > right.value )
				result = +1;
			else if( left.value < right.value )
				result = -1;
			else
				result = 0;
		}
		else if( availableLeft )
			result = +1;
		else if( availableRight )
			result = -1;
		else
			result = 0;

		return result;
	}


	public String toString()
	{
		return value + " ( " + annotation + " ) ";
	}

	/*****
	 * return a new copy of metric value 
	 * 
	 * @return
	 */
	public MetricValue duplicate()
	{
		MetricValue mv = new MetricValue(value, annotation);
		mv.flags = flags;
		return mv;
	}
}
