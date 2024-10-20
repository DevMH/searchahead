package tech.devmh.searchahead;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.cache.Cache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Worker {

	private final Controller controller;
	
	private final Service service;
	
	int minGramSize;
	
	int maxGramSize;
	
	int pageSize;
	
	int maxEntriesPerType;
	
	Cache loadCache() {
		Cache cache = null;
		controller.setCache(cache);
		return cache;
	}
	
	List<Entry> getList() {
		List<Entry> fullList = new ArrayList<>();
		for(EntryType type : EntryType.values()) {
			fullList.addAll(getListForType(type));
		}
		return fullList;
	}
	
	Map<EntryType, Map<String,SortedSet<Entry>>> getMap() throws IOException {
		Map<EntryType, Map<String,SortedSet<Entry>>> fullMap = new HashMap<>();
		for(EntryType type : EntryType.values()) {
			Map<String,SortedSet<Entry>> typeMap = getMapForType(type);
			fullMap.put(type, typeMap);
		}
		return fullMap;
	}
	
	List<Entry> getListForType(EntryType type) {
		List<Entry> list = new ArrayList<>();
		Page<Entry> page = null;
		int pageNum = 0;
		do {
			Pageable p = PageRequest.of(pageNum, pageSize);
			page = service.entries(type, p);
			List<Entry> pageEntries = page.getContent();
			for(Entry entry : (Iterable<Entry>) pageEntries::iterator) {
				list.add(entry);
			}
			pageNum++;
		} while(!page.isEmpty());
		return list;
	}
	
	Map<String,SortedSet<Entry>> getMapForType(EntryType type) throws IOException {
		Map<String,SortedSet<Entry>> map = new HashMap<>();
		Page<Entry> page = null;
		int pageNum = 0;
		do {
			Pageable p = PageRequest.of(pageNum, pageSize);
			page = service.entries(type, p);
			List<Entry> pageEntries = page.getContent();
			for(Entry entry : (Iterable<Entry>) pageEntries::iterator) {
				Collection<String> ngrams = tokenize(entry.getValue());
				ngrams.forEach(ngram -> {
					SortedSet<Entry> set = map.get(ngram);
					if(null == set) {
						set = new BoundedTreeSet<>(maxEntriesPerType);
					}
					set.add(entry);
					map.put(ngram, set);
				});
			}
			pageNum++;
		} while(!page.isEmpty());
		return map;
	}
	
	Collection<String> tokenize(String input) throws IOException {
		input = input.toUpperCase();
		Set<String> tokens = new HashSet<>();
		try (NGramTokenizer tokenizer = new NGramTokenizer(minGramSize,maxGramSize)) {
			tokenizer.setReader(new StringReader(input));
			CharTermAttribute ctAttr = tokenizer.addAttribute(CharTermAttribute.class);
			tokenizer.reset();
			while(tokenizer.incrementToken()) {
			    tokens.add(ctAttr.toString());
			}
		}
		return tokens;
	}
	
	public static class BoundedTreeSet<T> extends TreeSet<T> {

		private static final long serialVersionUID = 1L;
		
		private final int bound;
		
		BoundedTreeSet(int bound) {
			this.bound = bound;
		}
		
		@Override
		public boolean add(T t) {
			super.add(t);
			trim();
			return contains(t);
		}
		
		@Override
		public boolean addAll(Collection<? extends T> c) {
			super.addAll(c);
			trim();
			return containsAll(c);
		}
		
		private void trim() {
			while(size() > bound) {
				remove(last());
			}
		}
	}
}
