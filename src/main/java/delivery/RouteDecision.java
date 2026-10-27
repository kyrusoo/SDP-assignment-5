package delivery;

import java.util.List;

public record RouteDecision(Route route, String strategyName, double score,
                            String explanation, List<String> rejectedRoutes) {
    public RouteDecision {
        rejectedRoutes = List.copyOf(rejectedRoutes);
    }
}
