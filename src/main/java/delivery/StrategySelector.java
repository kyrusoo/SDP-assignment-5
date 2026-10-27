package delivery;

import java.util.Map;
import java.util.Objects;

public class StrategySelector {
    private final Map<DeliveryPriority, RouteStrategy> strategies;

    public StrategySelector() {
        this(Map.of(
                DeliveryPriority.URGENT, new FastestRouteStrategy(),
                DeliveryPriority.ECONOMY, new CheapestRouteStrategy(),
                DeliveryPriority.ECO, new EcoFriendlyRouteStrategy(),
                DeliveryPriority.STANDARD, new BalancedRouteStrategy(0.40, 0.30, 0.10, 0.20)));
    }

    public StrategySelector(Map<DeliveryPriority, RouteStrategy> strategies) {
        this.strategies = Map.copyOf(strategies);
    }

    public RouteStrategy select(DeliveryRequest request) {
        Objects.requireNonNull(request, "Delivery request is required");
        // Urgent deliveries keep time priority; other requests may opt into eco delivery.
        DeliveryPriority key = request.ecoPreferred() && request.priority() != DeliveryPriority.URGENT
                ? DeliveryPriority.ECO : request.priority();
        RouteStrategy strategy = strategies.get(key);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy registered for " + key);
        }
        return strategy;
    }
}
