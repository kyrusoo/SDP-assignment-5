package delivery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

final class RouteRules {
    private RouteRules() {
    }

    record Candidates(List<Route> validRoutes, List<String> rejections) {
    }

    static Candidates filter(List<Route> routes, DeliveryRequest request) {
        Objects.requireNonNull(routes, "Route list is required");
        Objects.requireNonNull(request, "Delivery request is required");
        if (routes.isEmpty()) {
            throw new NoSuitableRouteException("No candidate routes supplied");
        }
        List<Route> valid = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (Route route : routes) {
            Objects.requireNonNull(route, "Route must not be null");
            List<String> reasons = new ArrayList<>();
            if (!route.available()) {
                reasons.add("unavailable");
            }
            if (route.totalCost(request) > request.maxBudget()) {
                reasons.add("budget exceeded");
            }
            if (route.travelMinutes() > request.maxDeliveryMinutes()) {
                reasons.add("deadline exceeded");
            }
            if (reasons.isEmpty()) {
                valid.add(route);
            } else {
                rejected.add(route.id() + ": " + String.join(", ", reasons));
            }
        }
        if (valid.isEmpty()) {
            throw new NoSuitableRouteException("No suitable route: " + String.join("; ", rejected));
        }
        return new Candidates(List.copyOf(valid), List.copyOf(rejected));
    }

    static RouteDecision choose(Candidates candidates, String strategyName,
                                ToDoubleFunction<Route> score, Function<Route, String> explain) {
        Route best = candidates.validRoutes().stream()
                .min(Comparator.comparingDouble(score).thenComparing(Route::id))
                .orElseThrow(() -> new NoSuitableRouteException("No suitable route"));
        return new RouteDecision(best, strategyName, score.applyAsDouble(best),
                explain.apply(best), candidates.rejections());
    }
}
