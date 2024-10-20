package tech.devmh.searchahead;

public enum EntryType {
	
	ManyBroad(200000,30),
	ManyModerate(200000,15),
	ManySlim(200000,5),
	MediumBroad(10000,30),
	MediumModerate(10000,15),
	MediumSlim(10000,5),
	FewBroad(100,30),
	FewModerate(100,15),
	FewSlim(100,5);
	
	private final int count;
	private final int length;
	
	private EntryType(int count, int length) {
		this.count = count;
		this.length = length;
	}

	public int getCount() {
		return count;
	}

	public int getLength() {
		return length;
	}
	
}
