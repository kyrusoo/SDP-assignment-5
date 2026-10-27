package delivery;

import java.util.List;
import java.util.Locale;

public class CheapestRouteStrategy implements RouteStrategy {
    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        return RouteRules.choose(RouteRules.filter(routes, request), getStrategyName(),
                route -> route.totalCost(request),
                route -> String.format(Locale.ROOT,
                        "Minimum cost: base %.2f + distance %.2f x 0.50 + toll %.2f + priority %.2f = %.2f",
                        route.baseCost(), route.distanceKm(), route.tollFee(),
                        request.priority() == DeliveryPriority.URGENT ? 10.0 : 0.0,
                        route.totalCost(request)));
    }

    @Override
    public String getStrategyName() {
        return "Cheapest";
    }
}
