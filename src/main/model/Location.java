package model;

import java.util.Objects;

public class Location implements Comparable<Location> {
    private final String locationId;
    private final int priorityScore;

    public Location(String locationId, int priorityScore) {
        this.locationId = locationId;
        this.priorityScore = priorityScore;
    }

    public String getLocationId() {
        return locationId;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    @Override
    public int compareTo(Location other) {
        // Descending by priorityScore, ascending by locationId on tie
        int scoreCmp = Integer.compare(other.priorityScore, this.priorityScore);
        if (scoreCmp != 0) {
            return scoreCmp;
        }
        return this.locationId.compareTo(other.locationId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location location = (Location) o;
        return priorityScore == location.priorityScore
                && locationId.equals(location.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId, priorityScore);
    }

    @Override
    public String toString() {
        return locationId + " (priority: " + priorityScore + ")";
    }
}
