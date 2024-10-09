package com.graphhopper.routing.matrix.solver;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.RouterConfig;
import com.graphhopper.routing.WeightingFactory;
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.matrix.DijkstraOneToManyMatrixCalculator;
import com.graphhopper.routing.matrix.GHMatrixRequest;
import com.graphhopper.routing.matrix.MatrixCalculator;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.weighting.TurnCostProvider;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.routing.weighting.custom.CustomModelParser;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.util.PMap;

import java.util.Map;

public class DijkstraOneToManySolver extends MatrixSolver {

    private final BaseGraph graph;
    private final WeightingFactory weightingFactory;

    public DijkstraOneToManySolver(GHMatrixRequest request, Map<String, Profile> profilesByName,
                                   RouterConfig routerConfig, EncodedValueLookup lookup, BaseGraph graph, WeightingFactory weightingFactory) {
        super(request, profilesByName, routerConfig, lookup);
        this.graph = graph;
        this.weightingFactory = weightingFactory;
    }

    @Override
    protected Weighting createWeighting() {
        BooleanEncodedValue accessEnc = lookup.getBooleanEncodedValue(VehicleAccess.key(profile.getName()));
        DecimalEncodedValue speedEnc = lookup.getDecimalEncodedValue(VehicleSpeed.key(profile.getName()));
        //DecimalEncodedValue priorityEnc = encodingManager.getDecimalEncodedValue(VehiclePriority.key(profile.getName()));

        if (request.getCustomModel() != null) {
            return CustomModelParser.createWeighting(accessEnc, speedEnc, null, lookup, TurnCostProvider.NO_TURN_COST_PROVIDER, request.getCustomModel());
        } else {
            return weightingFactory.createWeighting(getProfile(), new PMap(), false);
        }
    }

    @Override
    public MatrixCalculator createMatrixCalculator(QueryGraph queryGraph) {
        return new DijkstraOneToManyMatrixCalculator(queryGraph, weighting);
    }
}
