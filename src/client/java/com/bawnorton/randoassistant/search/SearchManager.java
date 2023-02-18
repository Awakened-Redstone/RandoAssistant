package com.bawnorton.randoassistant.search;

import com.bawnorton.randoassistant.RandoAssistant;
import com.bawnorton.randoassistant.config.Config;
import com.bawnorton.randoassistant.config.ConfigManager;

import java.util.*;

public class SearchManager<T extends Searchable> {
    private final Map<String, T> searchMap;
    private final List<String> searchList;
    private Config.SearchType searchType;

    public SearchManager(List<T> searchList) {
        this.searchMap = new HashMap<>();
        this.searchList = new ArrayList<>();
        for(T searchable : searchList) {
            String searchableString = filter(searchable.getSearchableString());
            if(searchableString != null) {
                this.searchMap.put(searchableString, searchable);
                this.searchList.add(searchableString);
            }
        }
        this.searchList.sort(String::compareTo);
        this.searchType = Config.getInstance().searchType;
    }

    private String filter(String in) {
        in = in.toLowerCase().replaceAll("\\s+", "");
        if(in.isEmpty()) return null;
        return in;
    }

    public static int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    // binary search, return string that starts with query
    private T binarySearch(String query) {
        int low = 0;
        int high = searchList.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if(searchList.get(mid).startsWith(query)) {
                return searchMap.get(searchList.get(mid));
            } else if(searchList.get(mid).compareTo(query) < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null;
    }

    public Optional<T> getBestMatch(String query) {
        String adjustedQuery = filter(query);
        if(adjustedQuery == null) return Optional.empty();
        if(searchType == Config.SearchType.EXACT) {
            return Optional.ofNullable(binarySearch(adjustedQuery));
        } else if (searchType == Config.SearchType.CONTAINS) {
            for(String match : searchList) {
                if(match.contains(adjustedQuery)) {
                    return Optional.of(searchMap.get(match));
                }
            }
        } else if (searchType == Config.SearchType.FUZZY) {
            int bestDistance = Integer.MAX_VALUE;
            T bestMatch = null;
            for(String match : searchList) {
                int distance = levenshteinDistance(adjustedQuery, match);
                if(distance == 0) return Optional.of(searchMap.get(match));
                if(distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = searchMap.get(match);
                }
            }
            return Optional.ofNullable(bestMatch);
        }
        return Optional.empty();
    }

    public Config.SearchType getSearchType() {
        return searchType;
    }

    public void nextSearchType() {
        searchType = searchType.next();
        Config.getInstance().searchType = searchType;
    }
}
