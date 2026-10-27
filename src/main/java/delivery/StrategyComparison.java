package delivery;

import java.util.List;

public class StrategyComparison {
    private final List<RouteStrategy> strategies;

    public StrategyComparison(List<RouteStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
        if (strategies.isEmpty()) {
            throw new IllegalArgumentException("At least one strategy is required for comparison");
        }
    }

    public List<RouteDecision> compare(List<Route> routes, DeliveryRequest request) {
        return strategies.stream().map(strategy -> strategy.selectRoute(routes, request)).toList();
    }
}
