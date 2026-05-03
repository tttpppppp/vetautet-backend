package com.vetautet.application.cache;

public final class TripCacheKeys {

    public static final String TRIPS_ALL = "trips:all";
    public static final String HTTP_TRIPS_ALL = "http:trips:all";
    public static final String HTTP_TRIPS_SEARCH_PREFIX = "http:trips:search:";

    private TripCacheKeys() {
    }

    public static String trip(Long tripId) {
        return "trip:" + tripId;
    }

    public static String httpTrip(Long tripId) {
        return "http:trip:" + tripId;
    }

    public static String httpSearch(String queryHash) {
        return HTTP_TRIPS_SEARCH_PREFIX + queryHash;
    }
}
