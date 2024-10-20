package tech.devmh.searchahead;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class WorkerTest {

	private Worker classUnderTest;
	@Mock private Controller mockController;
	@Mock private Service mockService;
	private static RandomStringUtils util = RandomStringUtils.secure();
	private StopWatch watch;
	private ObjectMapper mapper;
	
	private static final String VALID_CHARS = "_0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int MAX_ENTRIES_PER_TYPE = 50;
	
	@BeforeEach
	void init() {
		watch = new StopWatch();
		mapper = new ObjectMapper();
		classUnderTest = new Worker(mockController, mockService);
		classUnderTest.minGramSize = 1;
		classUnderTest.maxGramSize = 3;
		classUnderTest.pageSize = 1000;
		classUnderTest.maxEntriesPerType = MAX_ENTRIES_PER_TYPE;
		when(mockService.entries(any(EntryType.class), any(Pageable.class)))
			.thenAnswer(i -> randomEntries((EntryType)i.getArguments()[0], (Pageable)i.getArguments()[1]));
	}
	
	@Test
	void canBuildFullMap() throws Exception {
		// given
		List<String> substrings = List.of("ABC","ABC","AB","A");
		// when
		Map<EntryType, Map<String, SortedSet<Entry>>> fullMap = classUnderTest.getMap();
		// then
		then(fullMap).isNotNull();
		for(String substring : (Iterable<String>) substrings::iterator) {
			watch.reset();
			String json = jsonFromMapWithObjectMapper(substring,fullMap);
			System.out.println(json.length());
			then(json).isNotBlank();
		}
		for(String substring : (Iterable<String>) substrings::iterator) {
			watch.reset();
			String json = jsonFromMapWithStringBuilder(substring,fullMap);
			System.out.println(json.length());
			then(json).isNotBlank();
		}
	}
	
	@Test
	void canBuildFullList() throws Exception {
		// given
		List<String> substrings = List.of("ABC","ABC","AB","A");
		// when
		List<Entry> fullList = classUnderTest.getList();
		// then
		then(fullList).isNotNull();
		for(String substring : (Iterable<String>) substrings::iterator) {
			watch.reset();
			String json = jsonFromListWithObjectMapper(substring,fullList);
			then(json).isNotBlank();
		}
	}
	
	@Test
	void canBuildTypeMap() throws Exception {
		// given
		EntryType type = EntryType.FewSlim;
		// when
		Map<String,SortedSet<Entry>> typeMap = classUnderTest.getMapForType(type);
		// then
		then(typeMap).isNotNull();
	}
	
	@Test
	void canBuildTypeList() throws Exception {
		// given
		EntryType type = EntryType.FewSlim;
		// when
		List<Entry> typeList = classUnderTest.getListForType(type);
		// then
		then(typeList.size()).isEqualTo(type.getCount());
	}
	
	private String jsonFromMapWithObjectMapper(String substring, Map<EntryType, Map<String, SortedSet<Entry>>> map) throws Exception {
		watch.reset();
		watch.start();
		SortedSet<Entry> combined = new TreeSet<>();
		for(EntryType type : EntryType.values()) {
			Map<String, SortedSet<Entry>> entries = map.get(type);
			if(null != entries) {
				SortedSet<Entry> entriesForType = entries.get(substring);
				if(null != entriesForType) {
					combined.addAll(entriesForType);
				}
			}
		}
		String json = mapper.writeValueAsString(combined);
		timeCheck(true, "jsonFromMapWithObjectMapper() with search string " + substring);
		return json;
	}
	
	private String jsonFromMapWithStringBuilder(String substring, Map<EntryType, Map<String, SortedSet<Entry>>> map) throws Exception {
		watch.reset();
		watch.start();
		StringBuilder builder = new StringBuilder("[ ");
		for(EntryType type : EntryType.values()) {
			Map<String, SortedSet<Entry>> entries = map.get(type);
			if(null != entries) {
				SortedSet<Entry> entriesForType = entries.get(substring);
				if(null != entriesForType) {
					entriesForType.forEach(entry -> {
						builder.append("{\"type\":\"" + entry.getType() + "\",\"value\":\"" + entry.getValue() + "\"},");
					});
				}
			}
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("]");
		String json = builder.toString();
		timeCheck(true, "jsonFromMapWithStringBuilder() with search string " + substring);
		return json;
	}
	
	private String jsonFromListWithObjectMapper(String substring, List<Entry> list) throws Exception {
		watch.reset();
		watch.start();
		SortedSet<Entry> combined = new Worker.BoundedTreeSet<>(MAX_ENTRIES_PER_TYPE * EntryType.values().length);
		for(Entry entry : list) {
			if(entry.getValue().contains(substring)) {
				combined.add(entry);
			}
		}
		String json = mapper.writeValueAsString(combined);
		timeCheck(true, "jsonFromListWithObjectMapper() with search string " + substring);
		return json;
	}
	
	private Duration timeCheck(boolean log, String logMessage) {
		watch.split();
		Duration dur = watch.getSplitDuration();
		watch.unsplit();
		if(log) {
			System.out.println(dur.toMillis() + "ms elapsed : " + logMessage);
		}
		return dur;
	}
	
	private static Page<Entry> randomEntries(EntryType type, Pageable pageable) {
		int offset = pageable.getPageNumber() * pageable.getPageSize();
		if(offset < type.getCount()) {
			int pageCount = Math.min(pageable.getPageSize(), type.getCount() - offset);
			List<Entry> pageEntries = new ArrayList<>();
			for (int i = 0; i < pageCount; i++) {
				pageEntries.add(randomEntry(type));
			}
			return new PageImpl<>(pageEntries, pageable, type.getCount());
		} else {
			return Page.empty(pageable);
		}
	}
	
	private static Entry randomEntry(EntryType type) {
		String value = util.next(type.getLength(),VALID_CHARS);
		return new Entry(type,value);
	}
}
