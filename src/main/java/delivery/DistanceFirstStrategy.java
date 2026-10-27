package delivery;

import java.util.List;

public class DistanceFirstStrategy implements RouteStrategy {
    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        return RouteRules.choose(RouteRules.filter(routes, request), getStrategyName(),
                Route::distanceKm,
                route -> "Minimum distance among eligible routes: " + route.distanceKm() + " km");
    }

    @Override
    public String getStrategyName() {
        return "Distance first";
    }
}
