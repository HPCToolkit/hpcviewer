package edu.rice.cs.hpc.data.util;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import java.util.Properties;
import java.util.Set;

public class JavaValidator 
{
	// minimum Java supported
	//private final static int JavaMinVersion = 5;
	private final static int JavaVersionSupported = 8;
	
	static public void main(String []args) {
		System.out.println();
		
		if (isGCJ()) {
			System.out.println("Unsupported JVM: GNU GCJ");
		} else {
			if (isCorrectJavaVersion())
				System.out.println("Valid JVM");
			else
				System.out.println("Invalid JVM: Needs to be higher or equal than " 
						+ JavaVersionSupported);
		}
	}
	
	
	/***
	 * print all the JVM properties
	 */
	static public void printProperties() {
		Properties p = System.getProperties();
		Set<Entry<Object, Object>> set = p.entrySet();
		Iterator<Entry<Object, Object>> iterator = set.iterator();
		
		while(iterator.hasNext()) {
			Entry<Object, Object> entry = iterator.next();
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
	}
	
	/****
	 * check if JVM is from GNU
	 * @return
	 */
	static public boolean isGCJ() {
		// GNU JVM has "Free Software Foundation" as the vendor
		return getJavaVendor().indexOf("Free")>=0;
	}
	
	
	/****
	 * retrieve the vendor (Sun, IBM, Free Foundation, ....)
	 * @return
	 */
	static public String getJavaVendor() {
		return System.getProperty("java.vendor");
	}
	
	/*****
	 * retrieve Java version
	 * @return
	 */
	static public String getJavaVersion() {
		return System.getProperty("java.version");
	}
	
	/****
	 * Check Java version. If the version is not supported,
	 *   display an error message and return false.
	 *   
	 * @return true if Java is supported. False otherwise.
	 */
	static public boolean isCorrectJavaVersion() {
		String version = getJavaVersion();

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
				return minor == JavaVersionSupported;
			}
			return major == JavaVersionSupported;
		} catch (Exception e) {
			System.err.println("Unknown java version: " + version);
		}
		return false;
	}

}
