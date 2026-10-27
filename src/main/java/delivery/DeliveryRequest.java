package delivery;

import java.util.Objects;

public record DeliveryRequest(DeliveryPriority priority, double maxBudget,
                              int maxDeliveryMinutes, boolean ecoPreferred) {
    public DeliveryRequest {
        Objects.requireNonNull(priority, "Delivery priority is required");
        if (!Double.isFinite(maxBudget) || maxBudget < 0 || maxDeliveryMinutes < 0) {
            throw new IllegalArgumentException("Budget and deadline must be finite and nonnegative");
        }
    }
}
