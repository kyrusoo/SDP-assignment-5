package delivery;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

public class BalancedRouteStrategy implements RouteStrategy {
    private final double timeWeight;
    private final double costWeight;
    private final double distanceWeight;
    private final double emissionWeight;

    public BalancedRouteStrategy(double timeWeight, double costWeight,
                                 double distanceWeight, double emissionWeight) {
        double sum = timeWeight + costWeight + distanceWeight + emissionWeight;
        if (DoubleStream.of(timeWeight, costWeight, distanceWeight, emissionWeight)
                .anyMatch(weight -> !Double.isFinite(weight) || weight < 0 || weight > 1)
                || Math.abs(sum - 1.0) > 1e-9) {
            throw new IllegalArgumentException("Weights must be finite, nonnegative and sum to 1.0");
        }
        this.timeWeight = timeWeight;
        this.costWeight = costWeight;
        this.distanceWeight = distanceWeight;
        this.emissionWeight = emissionWeight;
    }

    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        RouteRules.Candidates candidates = RouteRules.filter(routes, request);
        List<Route> valid = candidates.validRoutes();
        ToDoubleFunction<Route> time = normalize(valid, Route::travelMinutes);
        ToDoubleFunction<Route> cost = normalize(valid, route -> route.totalCost(request));
        ToDoubleFunction<Route> distance = normalize(valid, Route::distanceKm);
        ToDoubleFunction<Route> emission = normalize(valid, Route::carbonEmission);
        ToDoubleFunction<Route> score = route -> timeWeight * time.applyAsDouble(route)
                + costWeight * cost.applyAsDouble(route)
                + distanceWeight * distance.applyAsDouble(route)
                + emissionWeight * emission.applyAsDouble(route);
        return RouteRules.choose(candidates, getStrategyName(), score,
                route -> String.format(Locale.ROOT,
                        "Minimum balanced score: %.3f x %.2f time + %.3f x %.2f cost"
                                + " + %.3f x %.2f distance + %.3f x %.2f CO2 = %.3f",
                        time.applyAsDouble(route), timeWeight, cost.applyAsDouble(route), costWeight,
                        distance.applyAsDouble(route), distanceWeight,
                        emission.applyAsDouble(route), emissionWeight, score.applyAsDouble(route)));
    }

    private ToDoubleFunction<Route> normalize(List<Route> routes, ToDoubleFunction<Route> metric) {
        double min = routes.stream().mapToDouble(metric).min().orElseThrow();
        double max = routes.stream().mapToDouble(metric).max().orElseThrow();
        // An identical metric gives every candidate zero penalty, including a single candidate.
        return route -> max == min ? 0 : (metric.applyAsDouble(route) - min) / (max - min);
    }

    @Override
    public String getStrategyName() {
        return "Balanced";
    }
}
