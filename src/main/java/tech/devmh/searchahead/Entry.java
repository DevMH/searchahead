package tech.devmh.searchahead;

import java.io.Serializable;

import lombok.Data;

@Data
public class Entry implements Serializable, Comparable<Entry> {

	private static final long serialVersionUID = 1L;
	
	private final EntryType type;
	private final String value;
	
	@Override
	public int compareTo(Entry o) {
		int valComp = value.compareTo(o.getValue());
		return valComp == 0 ? type.compareTo(o.getType()): valComp;
	}
}
