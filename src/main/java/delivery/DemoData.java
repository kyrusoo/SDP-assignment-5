package delivery;

import java.util.List;

public final class DemoData {
    private DemoData() {
    }

    public static List<Route> routes() {
        Location warehouse = new Location("Warehouse", 43.238, 76.945);
        Location customer = new Location("Customer", 43.260, 76.920);
        return List.of(
                new Route("R1-City", warehouse, customer, 18, 15, 25, 30, 8, 8, true),
                new Route("R2-Highway", warehouse, customer, 25, 18, 2, 45, 10, 9, true),
                new Route("R3-Economy", warehouse, customer, 20, 38, 4, 8, 0, 5, true),
                new Route("R4-Electric", warehouse, customer, 16, 30, 2, 20, 0, 0.8, true),
                new Route("R5-Short", warehouse, customer, 10, 40, 5, 18, 0, 3, true),
                new Route("R6-Balanced", warehouse, customer, 14, 24, 3, 17, 1, 2, true),
                new Route("R7-Closed", warehouse, customer, 8, 8, 0, 1, 0, 0.1, false),
                new Route("R8-Premium", warehouse, customer, 22, 17, 1, 95, 20, 6, true));
    }

    public static List<RouteStrategy> strategies() {
        return List.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(0.40, 0.30, 0.10, 0.20));
    }
}
