package delivery;

import java.util.Objects;

public record Location(String name, double latitude, double longitude) {
    public Location {
        Objects.requireNonNull(name, "Location name is required");
        if (name.isBlank() || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new IllegalArgumentException("Invalid location name or coordinates");
        }
    }
}
