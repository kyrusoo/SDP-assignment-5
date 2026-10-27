package delivery;

import java.util.List;
import java.util.Locale;

public class EcoFriendlyRouteStrategy implements RouteStrategy {
    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        return RouteRules.choose(RouteRules.filter(routes, request), getStrategyName(),
                this::ecoScore,
                route -> String.format(Locale.ROOT,
                        "Minimum eco score: %.2f kg CO2 x 0.80 + %.2f km x 0.20 = %.3f",
                        route.carbonEmission(), route.distanceKm(), ecoScore(route)));
    }

    private double ecoScore(Route route) {
        return route.carbonEmission() * 0.80 + route.distanceKm() * 0.20;
    }

    @Override
    public String getStrategyName() {
        return "Eco friendly";
    }
}
