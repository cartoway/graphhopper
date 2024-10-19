package com.graphhopper.routing.matrix;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.Router;
import com.graphhopper.routing.RouterConfig;
import com.graphhopper.routing.ViaRouting;
import com.graphhopper.routing.WeightingFactory;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.matrix.solver.CHMatrixSolver;
import com.graphhopper.routing.matrix.solver.DijkstraOneToManySolver;
import com.graphhopper.routing.matrix.solver.MatrixSolver;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.DirectedEdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.details.PathDetailsBuilderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouterMatrix extends Router {
    public RouterMatrix(BaseGraph graph, EncodingManager encodingManager, LocationIndex locationIndex, Map<String, Profile> profilesByName, PathDetailsBuilderFactory pathDetailsBuilderFactory, TranslationMap translationMap, RouterConfig routerConfig, WeightingFactory weightingFactory, Map<String, RoutingCHGraph> chGraphs, Map<String, LandmarkStorage> landmarks) {
        super(graph, encodingManager, locationIndex, profilesByName, pathDetailsBuilderFactory, translationMap, routerConfig, weightingFactory, chGraphs, landmarks);
    }

    public GHMatrixResponse matrix(GHMatrixRequest request) {

        if (!request.getOriginCurbsides().isEmpty() && !request.getDestinationCurbsides().isEmpty())
            throw new IllegalArgumentException("Curbsides are not supported for Matrix");

        Profile profile = profilesByName.get(request.getProfile());

        MatrixSolver solver = createMatrixSolver(request);
        solver.checkRequest();
        solver.init();

        GHMatrixResponse ghMtxRsp = new GHMatrixResponse();

        DirectedEdgeFilter directedEdgeFilter = solver.createDirectedEdgeFilter();
        // For the usage of the Matrix use case, we don't need neither pointHints, SnapPreventions or Headings.
        List<Double> headings = new ArrayList<>();
        List<String> originPointHints = request.getOriginPointHints();
        List<String> destinationPointHints = request.getDestinationPointHints();
        List<String> snapPreventions = request.getSnapPreventions();

        MatrixSnapResult originsResult = ViaRouting.lookupMatrix(request.isFailFast(), encodingManager, request.getOrigins(), solver.createSnapFilter(), locationIndex,
                snapPreventions, originPointHints, directedEdgeFilter, headings);
        MatrixSnapResult destinationsResult = ViaRouting.lookupMatrix(request.isFailFast(), encodingManager, request.getDestinations(), solver.createSnapFilter(), locationIndex,
                snapPreventions, destinationPointHints, directedEdgeFilter, headings);

        List<Snap> allCorrectSnaps = new ArrayList<>(originsResult.snaps);
        allCorrectSnaps.addAll(destinationsResult.snaps);

        QueryGraph queryGraph = QueryGraph.create(graph, allCorrectSnaps);

        MatrixCalculator matrixCalculator = solver.createMatrixCalculator(queryGraph);
        DistanceMatrix matrix = matrixCalculator.calcMatrix(originsResult, destinationsResult);
        ghMtxRsp.setMatrix(matrix);

        return ghMtxRsp;
    }

    protected MatrixSolver createMatrixSolver(GHMatrixRequest request) {
        if  (request.getProfile() != null || !chGraphs.containsKey(request.getProfile()) || !request.getSnapPreventions().isEmpty() || !request.getOriginPointHints().isEmpty() || !request.getDestinationPointHints().isEmpty()) {
            return new DijkstraOneToManySolver(request, profilesByName, routerConfig, encodingManager, graph, weightingFactory);
        } else {
            return new CHMatrixSolver(request, profilesByName, routerConfig, encodingManager, chGraphs);
        }
    }
}
