package edu.rice.cs.hpc.viewer.util;

/*******
 * 
 * data structure to display metric column status:
 * <ul>
 * <li>label: the name of the metric (or column title)
 * <li>checked: if the column is showed (true) or not (false)
 * <li>enabled: if the column status can be changed (true) or not (false)
 * </ul>
 */
public class FilterDataItem 
{
	public String  label   = null;
	public boolean checked = false;
	public boolean enabled = true;
	
	/** data associated with this item.
	 *  it shouldn't be modified by any class except the owner (caller).
	 * **/
	public Object  data    = null;

	public FilterDataItem() {}
	
	public FilterDataItem(String label, boolean checked, boolean enabled) {
		this.label   = label;
		this.checked = checked;
		this.enabled = enabled;
	}
	
	public Object getData() {
		return data;
	}
	
	public void setData(Object data) {
		this.data = data;
	}
}
