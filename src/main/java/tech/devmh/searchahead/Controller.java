package tech.devmh.searchahead;

import org.springframework.cache.Cache;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    	
	Cache cache;

    @RequestMapping("/")
    String search(@RequestParam String searchString) {
        return cache.get(searchString, String.class);
    }
    
    synchronized void setCache(Cache cache) {
    	this.cache = cache;
    }
}
