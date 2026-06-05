package com.vetautet.application.cache;

public final class TripCacheKeys {

    public static final String HTTP_TRIPS_PREFIX = "http:trips:";
    public static final String TRIPS_ALL = "trips:all";
    public static final String TRIP_DETAIL_PREFIX = "trip:";
    public static final String HTTP_TRIPS_ALL = "http:trips:all";
    public static final String HTTP_TRIP_DETAIL_PREFIX = "http:trip:";
    public static final String HTTP_TRIPS_CATEGORIES = "http:trips:categories";
    public static final String HTTP_TRIPS_POPULAR_PREFIX = "http:trips:popular:";
    public static final String HTTP_TRIPS_POPULAR_ROUTES_PREFIX = "http:trips:popular-routes:";
    public static final String HTTP_TRIPS_POPULAR_DESTINATIONS_PREFIX = "http:trips:popular-destinations:";
    public static final String HTTP_TRIPS_UPCOMING_PREFIX = "http:trips:upcoming:";
    public static final String HTTP_TRIPS_SEARCH_PREFIX = "http:trips:search:";
    public static final String HTTP_TRIPS_SCHEDULES_PREFIX = "http:trips:schedules:";

    private TripCacheKeys() {
    }

    public static String trip(Long tripId) {
        return TRIP_DETAIL_PREFIX + tripId;
    }

    public static String httpTrip(Long tripId) {
        return HTTP_TRIP_DETAIL_PREFIX + tripId;
    }

    public static String httpPopular(int limit) {
        return HTTP_TRIPS_POPULAR_PREFIX + limit;
    }

    public static String httpPopularRoutes(int limit) {
        return HTTP_TRIPS_POPULAR_ROUTES_PREFIX + limit;
    }

    public static String httpPopularDestinations(int limit) {
        return HTTP_TRIPS_POPULAR_DESTINATIONS_PREFIX + limit;
    }

    public static String httpUpcoming(int limit) {
        return HTTP_TRIPS_UPCOMING_PREFIX + limit;
    }

    public static String httpSearch(String queryHash) {
        return HTTP_TRIPS_SEARCH_PREFIX + queryHash;
    }

    public static String httpSchedules(String queryHash) {
        return HTTP_TRIPS_SCHEDULES_PREFIX + queryHash;
    }
}
