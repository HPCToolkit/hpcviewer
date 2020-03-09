package edu.rice.cs.hpc.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import edu.rice.cs.hpc.data.util.IUserData;


/***
 * 
 * Mapping a name to another alias
 * This is useful when we want to change a name of a procedure X to Y (for display only) 
 *
 */
public abstract class AliasMap<K,V> implements IUserData<K, V> {
	
	protected HashMap<K, V> data;
	
	
	/***
	 * read map or create file if it doesn't exist
	 */
	public AliasMap() {

	}
		
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.util.IUserData#get(java.lang.String)
	 */
	public V get(K key) {
		
		checkData();
		return data.get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.util.IUserData#put(java.lang.String, java.lang.String)
	 */
	public void put(K key, V val) {
		
		checkData();
		data.put(key, val);
	}

	/***
	 * Removes the mapping for the specified key from this map if present.
	 * @param key
	 * @return the value if the key exists
	 */
	public V remove(K key) {
		checkData();
		V oldClass = data.remove(key);
		
		return oldClass;
	}

	/****
	 * Removes all of the mappings from this map. The map will be empty after this call 
	 */
	public void clear() {
		if (data != null)
			data.clear();
	}

	/***
	 * clear and remove resources
	 */
	public void dispose() {
		clear();
		data = null;
	}
	
	/***
	 * save the changes permanently into the storage
	 * Note: once it is stored, there is no way to restore back
	 */
	public void save() {
		
		final String filename = getFilename();
		final File file = new File(filename);
		
		try {
			ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream(file.getAbsoluteFile()) );
			out.writeObject(data);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	/***
	 * check if we have loaded the data or not. If not, we need to read the file,
	 * and initialize the data. If the file doesn't exist, it will be created and
	 * initialized by the default data (implemented by the children)
	 */
	protected void checkData() {
		if (data == null) {
			data = new HashMap<K, V>();
			final String filename = getFilename();
			File file = new File( filename );
			
			if (file.canRead()) {
				if (! readData(file.getAbsolutePath()) ) {
					// old format, we need to remove the file
					if ( file.delete() ) {
						// initialize the data
						initDefault();
						readData(file.getAbsolutePath());
					}
				}
			} else if (!file.exists()) {
				// file doesn't exist, but we can create
				
				// init data
				initDefault();
				
				save();
			}
		}
	}

	/***
	 * read data from a given file
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private boolean readData(String filename) {
		boolean result = false;
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(filename));
			Object o = in.readObject();
			if (o instanceof HashMap<?,?>) {
				data = (HashMap<K, V>) o;
				result =  true;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	abstract public String getFilename();
	abstract public void initDefault();

}
