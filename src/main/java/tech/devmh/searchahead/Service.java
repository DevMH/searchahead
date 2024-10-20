package tech.devmh.searchahead;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;

@org.springframework.stereotype.Service
@AllArgsConstructor
public class Service {

	@Autowired
	Repository repository;
	
	@Transactional(readOnly=true)
	Page<Entry> entries(EntryType type, Pageable pageable) {
		return repository.entries(type, pageable);
	}

}
