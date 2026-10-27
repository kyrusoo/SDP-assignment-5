package delivery;

import java.util.Objects;
import java.util.stream.DoubleStream;

public record Route(String id, Location origin, Location destination,
                    double distanceKm, int estimatedMinutes, int trafficMinutes,
                    double baseCost, double tollFee, double carbonEmission, boolean available) {
    public Route {
        Objects.requireNonNull(id, "Route id is required");
        Objects.requireNonNull(origin, "Origin is required");
        Objects.requireNonNull(destination, "Destination is required");
        if (id.isBlank() || estimatedMinutes < 0 || trafficMinutes < 0
                || (long) estimatedMinutes + trafficMinutes > Integer.MAX_VALUE
                || DoubleStream.of(distanceKm, baseCost, tollFee, carbonEmission)
                .anyMatch(value -> !Double.isFinite(value) || value < 0)) {
            throw new IllegalArgumentException("Route metrics must be finite and nonnegative");
        }
    }

    public int travelMinutes() {
        return estimatedMinutes + trafficMinutes;
    }

    public double totalCost(DeliveryRequest request) {
        double prioritySurcharge = request.priority() == DeliveryPriority.URGENT ? 10 : 0;
        return baseCost + distanceKm * 0.50 + tollFee + prioritySurcharge;
    }
}
