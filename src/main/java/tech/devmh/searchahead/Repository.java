package tech.devmh.searchahead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@org.springframework.stereotype.Repository
public class Repository {
	
	Page<Entry> entries(EntryType type, Pageable pageable) {
		return Page.empty(pageable);
	}

}
