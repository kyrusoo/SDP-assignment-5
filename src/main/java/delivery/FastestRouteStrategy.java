package delivery;

import java.util.List;

public class FastestRouteStrategy implements RouteStrategy {
    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        return RouteRules.choose(RouteRules.filter(routes, request), getStrategyName(),
                Route::travelMinutes,
                route -> "Minimum time: " + route.estimatedMinutes() + " + "
                        + route.trafficMinutes() + " traffic = " + route.travelMinutes() + " min");
    }

    @Override
    public String getStrategyName() {
        return "Fastest";
    }
}
