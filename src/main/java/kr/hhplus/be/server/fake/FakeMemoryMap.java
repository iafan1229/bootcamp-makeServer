package kr.hhplus.be.server.fake;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeMemoryMap {

    private final Map<String, Integer> memoryMap = new ConcurrentHashMap<>();

    public void increment(String keyword) {
        increment(keyword, 1);
    }

    public void increment(String keyword, int count) {
        memoryMap.merge(keyword, count, Integer::sum);
    }

    public int getCount(String keyword) {
        return memoryMap.getOrDefault(keyword, 0);
    }

    public Map<String, Integer> getAllData() {
        return Map.copyOf(memoryMap);
    }

    public void clear() {
        memoryMap.clear();
    }

    public boolean isEmpty() {
        return memoryMap.isEmpty();
    }

    public int size() {
        return memoryMap.size();
    }
}