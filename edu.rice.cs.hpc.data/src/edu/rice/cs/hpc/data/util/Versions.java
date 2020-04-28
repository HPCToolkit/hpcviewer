//////////////////////////////////////////////////////////////////////////
//																		//
//	Versions.java														//
//																		//
//	util.Versions -- version strings for hpcviewer						//
//	Last edited: January 28, 2002 at 3:30 pm							//
//																		//
//	(c) Copyright 2002 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.util;

import javax.swing.JOptionPane;

//////////////////////////////////////////////////////////////////////////
//	CLASS VERSIONS														//
//////////////////////////////////////////////////////////////////////////

/**
 *
 * The version strings for hpcviewer.
 *
 */
 
public class Versions
{


	public static final String HPCVIEWER = "2020.05";


	//////////////////////////////////////////////////////////////
	///
	/// Public methods
	///
	//////////////////////////////////////////////////////////////

	/**
	 * return this viewer version.
	 * @return
	 */
	public String getVersion() {
		return HPCVIEWER;
	}
	
	/****
	 * Check Java version. If the version is not supported,
	 *   display an error message and return false.
	 *   
	 * @return true if Java is supported. False otherwise.
	 */
	static public boolean checkJava() {
		String version = System.getProperty("java.version");

		System.out.println("java version: " + version);

		boolean isCorrect = checkVersion(version);
		if (!isCorrect) {
			String message = "Error: Java " + 
					System.getProperty("java.version") +
					" is not supported.\nOnly Java 8 is supported.";

			System.out.println(message);

			JOptionPane.showMessageDialog(null, message);
		}
		return isCorrect;
	}

	//////////////////////////////////////////////////////////////
	///
	/// Private methods
	///
	//////////////////////////////////////////////////////////////
	

	static private boolean checkVersion(String version) {

		if (version == null)
			return false;

		String verNumber[]  = version.split("\\.", 3);
		String majorVersion = verNumber[0];

		try {
			Integer major = Integer.valueOf(majorVersion);
			if (major == 1) {
				Integer minor = Integer.valueOf(verNumber[1]);
				return minor == 8;
			}
			return major == 8;
		} catch (Exception e) {
			System.err.println("Unknown java version: " + version);
		}
		return false;
	}

}	// end class Versions









