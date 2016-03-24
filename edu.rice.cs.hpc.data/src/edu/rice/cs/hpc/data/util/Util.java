//////////////////////////////////////////////////////////////////////////
//																		//
//	Util.java															//
//																		//
//	util.Util -- miscellaneous useful operations						//
//	Last edited: September 18, 2001 at 6:55 pm							//
//																		//
//	(c) Copyright 2001 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.util;


import java.io.File;
import java.io.FilenameFilter;
import java.text.NumberFormat;
import java.text.DecimalFormat;



//////////////////////////////////////////////////////////////////////////
//	CLASS UTIL															//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 * Miscellaneous useful operations.
 *
 */


public class Util
{




//////////////////////////////////////////////////////////////////////////
//	PRIVATE CONSTANTS													//
//////////////////////////////////////////////////////////////////////////




/** An array of space characters used to left-pad the output of <code>DecimalFormat</code>. */
protected static final String SPACES = "                                        ";




//////////////////////////////////////////////////////////////////////////
//	STRING CONVERSION													//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns the <code>boolean</code> corresponding to a given string.
 ************************************************************************/
	
public static boolean booleanValue(String s)
{
	return Boolean.valueOf(s).booleanValue();
}




//////////////////////////////////////////////////////////////////////////
//	TEXT FORMATTING														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns a <code>String</code> of a given number of space characters.
 ************************************************************************/
	
public static String spaces(int count)
{
	Dialogs.Assert(count <= SPACES.length(), "request too long Util::spaces");
	
	return SPACES.substring(0, count);
}




/*************************************************************************
 *	Fits a string into a field of given width, right adjusted.
 ************************************************************************/
	
public static String rightJustifiedField(String s, int fieldWidth)
{
	String field;
	
	int padLeft = fieldWidth - s.length();
	if( padLeft > 0 )
		field = Util.spaces(padLeft) + s;
	else
		field = s.substring(0, fieldWidth);

	return field;
}




/*************************************************************************
 *	Returns a new <code>DecimalFormat</code> object with a given pattern.
 ************************************************************************/
	
public static DecimalFormat makeDecimalFormatter(String pattern)
{
	// make a formatter, checking that the locale allows this
	NumberFormat nf = NumberFormat.getInstance();
	Dialogs.Assert( nf instanceof DecimalFormat, "bad arg to Util::makeDecimalFormatter");
	DecimalFormat df = (DecimalFormat) nf;
	
	// apply the given pattern
	df.applyPattern(pattern);
	
	return df;
}




/*************************************************************************
 *	Formats an <code>int</code> in a given format and field width.
 *
 *	TODO: It might be possible to improve this method's implementation by
 *	using class <code>java.text.FieldPosition</code>.
 *
 ************************************************************************/
	
public static String formatInt(int n, DecimalFormat formatter, int fieldWidth)
{
	return Util.rightJustifiedField(formatter.format(n), fieldWidth);
}




/*************************************************************************
 *	Formats a <code>double</code> in a given format and field width.
 *
 *	TODO: It might be possible to improve this method's implementation by
 *	using class <code>java.text.FieldPosition</code>.
 *
 ************************************************************************/
	
public static String formatDouble(double d, DecimalFormat formatter, int fieldWidth)
{
	return Util.rightJustifiedField(formatter.format(d), fieldWidth);
}


/**
 * Class to filter the list of files in a directory and return only XML files 
 * The filter is basically very simple: if the last 3 letters has "xml" substring
 * then we consider it as XML file.
 * TODO: we need to have a more sophisticated approach to filter only the real XML files
 *
 */
public static class FileXMLFilter implements FilenameFilter {
	public boolean accept(File pathname, String sName) {
		int iLength = sName.length();
		if (iLength <4) // the file should contain at least four letters: ".xml"
			return false;
		String sExtension = (sName.substring(iLength-3, iLength)).toLowerCase();
		return (pathname.canRead() && sExtension.endsWith("xml"));
	}
}


/**
 *  File filter to find a file with a glob-style pattern
 *  It is primarily used by File.listdir method.
 */
public static class FileThreadsMetricFilter implements FilenameFilter {
	private String db_glob;
	
	public FileThreadsMetricFilter(String pattern) {
		db_glob = pattern.replace("*", ".*");
	}
	
	public boolean accept(File dir, String name) {
		
		boolean b = name.matches(db_glob);
		return b;
	}
	
}

public static File[] getListOfXMLFiles(String sDir) 
{
	// find XML files in this directory
	File files = new File(sDir);
	// for debugging purpose, let have separate variable
	File filesXML[] = files.listFiles(new FileXMLFilter());
	return filesXML;
}


static public String getObjectID(Object o) {
	return Integer.toHexString(System.identityHashCode(o));
}

/**
 * Converts a standard POSIX Shell globbing pattern into a regular expression
 * pattern. The result can be used with the standard {@link java.util.regex} API to
 * recognize strings which match the glob pattern.
 * <p/>
 * See also, the POSIX Shell language:
 * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
 * 
 * @param pattern A glob pattern.
 * @return A regex pattern to recognize the given glob pattern.
 */
public static final String convertGlobToRegex(String pattern) {
    StringBuilder sb = new StringBuilder(pattern.length());
    int inGroup = 0;
    int inClass = 0;
    int firstIndexInClass = -1;
    char[] arr = pattern.toCharArray();
    for (int i = 0; i < arr.length; i++) {
        char ch = arr[i];
        switch (ch) {
            case '\\':
                if (++i >= arr.length) {
                    sb.append('\\');
                } else {
                    char next = arr[i];
                    switch (next) {
                        case ',':
                            // escape not needed
                            break;
                        case 'Q':
                        case 'E':
                            // extra escape needed
                            sb.append('\\');
                        default:
                            sb.append('\\');
                    }
                    sb.append(next);
                }
                break;
            case '*':
                if (inClass == 0)
                    sb.append(".*");
                else
                    sb.append('*');
                break;
            case '?':
                if (inClass == 0)
                    sb.append('.');
                else
                    sb.append('?');
                break;
            case '[':
                inClass++;
                firstIndexInClass = i+1;
                sb.append('[');
                break;
            case ']':
                inClass--;
                sb.append(']');
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                    sb.append('\\');
                sb.append(ch);
                break;
            case '!':
                if (firstIndexInClass == i)
                    sb.append('^');
                else
                    sb.append('!');
                break;
            case '{':
                inGroup++;
                sb.append('(');
                break;
            case '}':
                inGroup--;
                sb.append(')');
                break;
            case ',':
                if (inGroup > 0)
                    sb.append('|');
                else
                    sb.append(',');
                break;
            default:
                sb.append(ch);
        }
    }
    return sb.toString();
}

}








