package delivery;

import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--compare"))) {
            throw new IllegalArgumentException("Usage: java -jar delivery-strategy-1.0.0.jar [--compare]");
        }
        List<Route> routes = DemoData.routes();
        List<RouteStrategy> strategies = DemoData.strategies();
        DeliveryRequest standard = new DeliveryRequest(DeliveryPriority.STANDARD, 100, 60, false);

        System.out.println("SMART DELIVERY - " + routes.size() + " candidate routes");
        System.out.println("Comparison: STANDARD, budget 100.00, deadline 60 min");
        new StrategyComparison(strategies).compare(routes, standard)
                .forEach(decision -> print(decision, standard));
        if (args.length == 1) {
            return;
        }

        System.out.println("\nRuntime switching on the same planner:");
        RoutePlanner planner = new RoutePlanner(strategies.get(0));
        System.out.println("Start -> " + planner.plan(routes, standard).strategyName());
        for (RouteStrategy strategy : strategies.subList(1, strategies.size())) {
            planner.setStrategy(strategy);
            RouteDecision decision = planner.plan(routes, standard);
            System.out.println("Switch -> " + decision.strategyName() + ": " + decision.route().id());
        }

        System.out.println("\nAutomatic selection for different requests:");
        StrategySelector selector = new StrategySelector();
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            DeliveryRequest request = new DeliveryRequest(priority, 100, 60, false);
            planner.setStrategy(selector.select(request));
            System.out.println(priority + " -> " + planner.plan(routes, request).route().id());
        }
        DeliveryRequest ecoPreferred = new DeliveryRequest(DeliveryPriority.STANDARD, 100, 60, true);
        planner.setStrategy(selector.select(ecoPreferred));
        System.out.println("STANDARD + eco preference -> " + planner.plan(routes, ecoPreferred).route().id());

        planner.setStrategy(new FastestRouteStrategy());
        DeliveryRequest tightBudget = new DeliveryRequest(DeliveryPriority.STANDARD, 25, 60, false);
        System.out.println("\nSame fastest strategy, budget 100 -> " + planner.plan(routes, standard).route().id());
        System.out.println("Same fastest strategy, budget 25 -> " + planner.plan(routes, tightBudget).route().id());

        System.out.println("\nHandled failures:");
        showFailure("Empty routes", () -> planner.plan(List.of(), standard));
        showFailure("No valid route", () -> planner.plan(routes,
                new DeliveryRequest(DeliveryPriority.STANDARD, 1, 1, false)));
        showFailure("Invalid weights", () -> new BalancedRouteStrategy(1, 1, 1, 1));
    }

    private static void print(RouteDecision decision, DeliveryRequest request) {
        Route route = decision.route();
        System.out.printf(Locale.ROOT,
                "%n%s -> %s | time %d min | cost %.2f | distance %.1f km | CO2 %.1f kg | score %.3f%n",
                decision.strategyName(), route.id(), route.travelMinutes(), route.totalCost(request),
                route.distanceKm(), route.carbonEmission(), decision.score());
        System.out.println("Reason: " + decision.explanation());
        decision.rejectedRoutes().forEach(reason -> System.out.println("Rejected " + reason));
    }

    private static void showFailure(String label, Runnable action) {
        try {
            action.run();
        } catch (NoSuitableRouteException | IllegalArgumentException exception) {
            System.out.println(label + ": " + exception.getMessage());
        }
    }
}
