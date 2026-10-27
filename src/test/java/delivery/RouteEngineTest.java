package delivery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RouteEngineTest {
    private static final Location ORIGIN = new Location("Warehouse", 43, 76);
    private static final Location DESTINATION = new Location("Customer", 44, 77);
    private static final DeliveryRequest STANDARD = request(100, 60);

    private static DeliveryRequest request(double budget, int deadline) {
        return new DeliveryRequest(DeliveryPriority.STANDARD, budget, deadline, false);
    }

    private static Route route(String id, double distance, int minutes, int traffic,
                               double baseCost, double toll, double emission, boolean available) {
        return new Route(id, ORIGIN, DESTINATION, distance, minutes, traffic,
                baseCost, toll, emission, available);
    }

    static Stream<RouteStrategy> strategies() {
        return DemoData.strategies().stream();
    }

    @Test
    void fastestAccountsForTrafficRatherThanOnlyEstimatedTime() {
        Route congested = route("A", 10, 10, 30, 10, 0, 2, true);
        Route clear = route("B", 15, 20, 0, 10, 0, 3, true);
        RouteDecision result = new FastestRouteStrategy().selectRoute(List.of(congested, clear), STANDARD);
        assertEquals(clear, result.route());
        assertEquals(20, result.score());
        assertTrue(result.explanation().contains("traffic"));
    }

    @Test
    void cheapestUsesDistanceAndFeesInsteadOfBaseCostAlone() {
        Route lowBase = route("A", 50, 30, 0, 1, 20, 2, true);
        Route lowTotal = route("B", 10, 30, 0, 10, 0, 2, true);
        RouteDecision result = new CheapestRouteStrategy().selectRoute(List.of(lowBase, lowTotal), STANDARD);
        assertEquals(lowTotal, result.route());
        assertEquals(15, result.score());
        assertTrue(result.explanation().contains("toll"));
    }

    @Test
    void prioritySurchargeIsIncludedInCostAndBudgetChecks() {
        Route candidate = route("A", 10, 20, 0, 10, 2, 1, true);
        DeliveryRequest urgent = new DeliveryRequest(DeliveryPriority.URGENT, 26, 60, false);
        assertEquals(17, candidate.totalCost(STANDARD));
        assertEquals(27, candidate.totalCost(urgent));
        assertThrows(NoSuitableRouteException.class,
                () -> new CheapestRouteStrategy().selectRoute(List.of(candidate), urgent));
    }

    @Test
    void ecoConsidersDistanceAsWellAsEmissions() {
        Route longLowEmission = route("A", 30, 20, 0, 5, 0, 1, true);
        Route shortRoute = route("B", 10, 20, 0, 5, 0, 2, true);
        RouteDecision result = new EcoFriendlyRouteStrategy()
                .selectRoute(List.of(longLowEmission, shortRoute), STANDARD);
        assertEquals(shortRoute, result.route());
        assertEquals(3.6, result.score(), 1e-9);
    }

    private static List<Route> balancedCandidates() {
        return List.of(
                route("A", 30, 10, 0, 45, 0, 9, true),
                route("B", 20, 20, 0, 30, 0, 5, true),
                route("C", 10, 30, 0, 15, 0, 1, true));
    }

    @Test
    void balancedUsesNormalizedMetricsAndAllFourWeights() {
        RouteDecision result = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2)
                .selectRoute(balancedCandidates(), STANDARD);
        assertEquals("C", result.route().id());
        // C has normalized values (1, 0, 0, 0), so its weighted score is 0.4.
        assertEquals(0.4, result.score(), 1e-9);
        assertTrue(result.explanation().contains("distance"));
    }

    @Test
    void changingBalancedWeightsChangesTheWinner() {
        RouteDecision timeOnly = new BalancedRouteStrategy(1, 0, 0, 0)
                .selectRoute(balancedCandidates(), STANDARD);
        RouteDecision costOnly = new BalancedRouteStrategy(0, 1, 0, 0)
                .selectRoute(balancedCandidates(), STANDARD);
        assertEquals("A", timeOnly.route().id());
        assertEquals("C", costOnly.route().id());
    }

    @Test
    void balancedHandlesEqualMetricsAndBreaksTiesById() {
        Route a = route("A", 10, 20, 0, 10, 0, 2, true);
        Route b = route("B", 10, 20, 0, 10, 0, 2, true);
        RouteDecision result = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2)
                .selectRoute(List.of(b, a), STANDARD);
        assertEquals(a, result.route());
        assertEquals(0, result.score());
    }

    @Test
    void balancedNormalizesOnlyEligibleRoutes() {
        Route outlier = route("D", 1000, 1000, 0, 10000, 0, 10000, false);
        List<Route> routes = Stream.concat(balancedCandidates().stream(), Stream.of(outlier)).toList();
        RouteDecision result = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2)
                .selectRoute(routes, STANDARD);
        assertEquals("C", result.route().id());
        assertEquals(0.4, result.score(), 1e-9);
    }

    @Test
    void samePlannerSupportsThreeRuntimeSwitches() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        List<Route> routes = DemoData.routes();
        assertEquals("R2-Highway", planner.plan(routes, STANDARD).route().id());
        planner.setStrategy(new CheapestRouteStrategy());
        assertEquals("R3-Economy", planner.plan(routes, STANDARD).route().id());
        planner.setStrategy(new EcoFriendlyRouteStrategy());
        assertEquals("R4-Electric", planner.plan(routes, STANDARD).route().id());
        planner.setStrategy(new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2));
        assertEquals("R6-Balanced", planner.plan(routes, STANDARD).route().id());
    }

    @ParameterizedTest
    @EnumSource(DeliveryPriority.class)
    void selectorMapsEachPriorityToTheExpectedAlgorithm(DeliveryPriority priority) {
        Map<DeliveryPriority, String> expected = Map.of(
                DeliveryPriority.URGENT, "Fastest", DeliveryPriority.ECONOMY, "Cheapest",
                DeliveryPriority.ECO, "Eco friendly", DeliveryPriority.STANDARD, "Balanced");
        DeliveryRequest request = new DeliveryRequest(priority, 100, 60, false);
        assertEquals(expected.get(priority), new StrategySelector().select(request).getStrategyName());
    }

    @Test
    void ecoPreferenceOverridesStandardButNotUrgentPriority() {
        StrategySelector selector = new StrategySelector();
        assertInstanceOf(EcoFriendlyRouteStrategy.class,
                selector.select(new DeliveryRequest(DeliveryPriority.STANDARD, 100, 60, true)));
        assertInstanceOf(FastestRouteStrategy.class,
                selector.select(new DeliveryRequest(DeliveryPriority.URGENT, 100, 60, true)));
    }

    @Test
    void selectorAcceptsCustomMappingsAndReportsMissingMappings() {
        RouteStrategy custom = new CheapestRouteStrategy();
        StrategySelector selector = new StrategySelector(Map.of(DeliveryPriority.STANDARD, custom));
        assertSame(custom, selector.select(STANDARD));
        assertThrows(IllegalArgumentException.class,
                () -> selector.select(new DeliveryRequest(DeliveryPriority.ECO, 100, 60, false)));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void everyStrategyFiltersAllHardConstraintsAndExplainsRejections(RouteStrategy strategy) {
        Route closed = route("closed", 1, 1, 0, 0, 0, 0, false);
        Route expensive = route("expensive", 1, 1, 0, 100, 0, 0, true);
        Route late = route("late", 1, 10, 20, 0, 0, 0, true);
        Route valid = route("valid", 10, 10, 0, 10, 0, 2, true);
        RouteDecision result = strategy.selectRoute(List.of(closed, expensive, late, valid), request(20, 20));
        assertEquals(valid, result.route());
        assertEquals(List.of("closed: unavailable", "expensive: budget exceeded", "late: deadline exceeded"),
                result.rejectedRoutes());
        assertFalse(result.explanation().isBlank());
    }

    @Test
    void budgetChangeCanChangeTheSameStrategysWinner() {
        RouteStrategy fastest = new FastestRouteStrategy();
        assertEquals("R2-Highway", fastest.selectRoute(DemoData.routes(), STANDARD).route().id());
        assertEquals("R6-Balanced", fastest.selectRoute(DemoData.routes(), request(25, 60)).route().id());
    }

    @Test
    void deadlineFiltersCheapestRouteUsingTimeIncludingTraffic() {
        RouteDecision result = new CheapestRouteStrategy().selectRoute(DemoData.routes(), request(100, 30));
        assertEquals("R6-Balanced", result.route().id());
        assertTrue(result.rejectedRoutes().contains("R3-Economy: deadline exceeded"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void exactBudgetAndDeadlineAreAllowedWithSingleCandidate(RouteStrategy strategy) {
        Route exact = route("exact", 10, 10, 5, 10, 5, 0, true);
        assertEquals(exact, strategy.selectRoute(List.of(exact), request(20, 15)).route());
    }

    @Test
    void noSuitableRouteThrowsWithRejectionReasons() {
        NoSuitableRouteException exception = assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(DemoData.routes(), request(1, 1)));
        assertTrue(exception.getMessage().contains("budget exceeded"));
        assertTrue(exception.getMessage().contains("deadline exceeded"));
    }

    @Test
    void emptyCandidateListIsAnExplicitFailure() {
        NoSuitableRouteException exception = assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(), STANDARD));
        assertEquals("No candidate routes supplied", exception.getMessage());
    }

    @Test
    void invalidBalancedWeightsAreRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(1, 1, 1, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(-0.1, 0.5, 0.3, 0.3)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.NaN, 0, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.POSITIVE_INFINITY, 0, 0, 0)));
    }

    @Test
    void invalidDomainMetricsAreRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> route("bad", -1, 10, 0, 10, 0, 1, true)),
                () -> assertThrows(IllegalArgumentException.class, () -> route("bad", 1, 10, 0, -10, 0, 1, true)),
                () -> assertThrows(IllegalArgumentException.class, () -> route("bad", 1, 10, 0, 10, 0, Double.NaN, true)),
                () -> assertThrows(IllegalArgumentException.class, () -> request(-1, 10)),
                () -> assertThrows(IllegalArgumentException.class, () -> request(10, -1)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Location("bad", 91, 0)));
    }

    @Test
    void missingInputsAndStrategyAreRejected() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new FastestRouteStrategy().selectRoute(DemoData.routes(), null)),
                () -> assertThrows(NullPointerException.class, () -> new FastestRouteStrategy().selectRoute(null, STANDARD)),
                () -> assertThrows(NullPointerException.class, () -> new DeliveryRequest(null, 100, 60, false)),
                () -> assertThrows(NullPointerException.class, () -> new RoutePlanner(null)),
                () -> assertThrows(NullPointerException.class, () -> new RoutePlanner(new FastestRouteStrategy()).setStrategy(null)));
    }

    @Test
    void fifthStrategySelectsShortestRouteThroughTheExistingPlanner() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        planner.setStrategy(new DistanceFirstStrategy());
        RouteDecision result = planner.plan(DemoData.routes(), STANDARD);
        assertEquals("R5-Short", result.route().id());
        assertEquals(10, result.score());
        assertTrue(result.rejectedRoutes().contains("R7-Closed: unavailable"));
    }

    @Test
    void comparisonRunsAllStrategiesForTheSameRequest() {
        List<RouteDecision> results = new StrategyComparison(DemoData.strategies()).compare(DemoData.routes(), STANDARD);
        assertEquals(List.of("R2-Highway", "R3-Economy", "R4-Electric", "R6-Balanced", "R5-Short"),
                results.stream().map(result -> result.route().id()).toList());
    }
}
