package org.reactome.server.tools.diagram.exporter.raster.diagram.renderers.common;

import org.reactome.server.tools.diagram.data.graph.EntityNode;
import org.reactome.server.tools.diagram.data.graph.EventNode;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.graph.GraphNode;
import org.reactome.server.tools.diagram.data.layout.*;
import org.reactome.server.tools.diagram.exporter.common.analysis.AnalysisClient;
import org.reactome.server.tools.diagram.exporter.common.analysis.exception.AnalysisException;
import org.reactome.server.tools.diagram.exporter.common.analysis.exception.AnalysisServerError;
import org.reactome.server.tools.diagram.exporter.common.analysis.model.*;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates a DiagramObjectDecorator per Node in the diagram. Computes all the
 * information that modifies each node basic rendering: selection, flag, halo
 * and analysis (enrichments and expressions). This data is not in the Node
 * class.
 *
 * @author Lorente-Arencibia, Pascual (pasculorente@gmail.com)
 */
// TODO: split in a class for analysis, a class for decorators and a class for the index
public class DiagramIndex {

	public static final double MIN_ENRICHMENT = 0.05;
	private final Diagram diagram;
	private final Graph graph;
	private final HashMap<Long, DiagramObjectDecorator> index;
	private RasterArgs args;
	/**
	 * [diagram id] diagramNode.getId()
	 */
	private Map<Long, DiagramObject> diagramIndex;
	/**
	 * [reactome id] diagramNode.getReactomeId(), graphNode.getDbId()
	 */
	private Map<Long, DiagramObject> reactomeIndex;
	/**
	 * [reactome id] graphNode.getDbId()
	 */
	private Map<Long, EntityNode> graphIndex;
	/**
	 * [reactome id] graphEdge.getDbId()
	 */
	private HashSet<Long> reactionIds;

	private AnalysisType analysisType = AnalysisType.NONE;
	/** node used when a legend is displayed */
	private NodeDecorator selected;
	private String analysisName;
	private AnalysisResult result;


	/**
	 * Creates a new DiagramIndex with the information for each node in maps.
	 *
	 * @param diagram diagram with nodes and reactions
	 * @param graph   background graph
	 */
	public DiagramIndex(Diagram diagram, Graph graph, RasterArgs args) throws AnalysisServerError, AnalysisException {
		this.diagram = diagram;
		this.graph = graph;
		this.args = args;
		this.index = new HashMap<>();
		index();
		collect();
	}

	private void index() {
		// Indexes to map layout <-> graph
		diagramIndex = new HashMap<>();
		reactomeIndex = new HashMap<>();
		Stream.of(diagram.getEdges(), diagram.getNodes(), diagram.getLinks())
				.flatMap(Collection::stream)
				.forEach(item -> {
					diagramIndex.put(item.getId(), item);
					reactomeIndex.put(item.getReactomeId(), item);
				});
		graphIndex = new HashMap<>();
		graph.getNodes().forEach(item -> graphIndex.put(item.getDbId(), item));
		reactionIds = new HashSet<>();
		graph.getEdges().forEach(event -> reactionIds.add(event.getDbId()));
		// Add connectors to reactions, so they can be rendered together
		diagram.getNodes().stream()
				.map(Node::getConnectors)
				.flatMap(Collection::stream)
				.forEach(connector -> getEdgeDecorator(connector.getEdgeId()).getConnectors().add(connector));
	}

	private void collect() throws AnalysisException, AnalysisServerError {
		final Set<Long> sel = getSelectedIds();
		final Set<Long> flg = getFlagged();
		decorateNodes(sel, flg);
		decorateReactions(sel, flg);
		addAnalysisData();
	}

	private Set<Long> getSelectedIds() {
		if (args.getSelected() == null)
			return Collections.emptySet();
		return args.getSelected().stream()
				.map(this::getDiagramObjectId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private Set<Long> getFlagged() {
		if (args.getFlags() == null)
			return Collections.emptySet();
		return args.getFlags().stream()
				.map(this::getDiagramObjectId)
				.filter(Objects::nonNull)
				.flatMap(this::streamMeAndAncestors)
				.collect(Collectors.toSet());
	}

	private Stream<Long> streamMeAndAncestors(Long id) {
		final Set<Long> ids = new HashSet<>();
		ids.add(id);
		final EntityNode node = graphIndex.get(id);
		if (node == null)
			return ids.stream();
		if (node.getParents() != null)
			node.getParents().forEach(parentId -> streamMeAndAncestors(parentId).forEach(ids::add));
		return ids.stream();
	}

	private Long getDiagramObjectId(String string) {
		// dbId, this is faster because dbId is indexed
		try {
			final long dbId = Long.parseLong(string);
			if (graphIndex.containsKey(dbId)
					|| reactionIds.contains(dbId))
				return dbId;
		} catch (NumberFormatException ignored) {
			// ignored, not a dbId
		}
		// TODO: would it be faster if index of stIds, dbIds, identifier and geneNames?
		// Pros: avoid iterating through the list of nodes and edges
		// Cons: index the list of nodes and edges just for a few selected of flag items
		// Nodes
		for (EntityNode node : graph.getNodes()) {
			// stId
			if (string.equalsIgnoreCase(node.getStId()))
				return node.getDbId();
			// identifier
			if (string.equalsIgnoreCase(node.getIdentifier()))
				return node.getDbId();
			// geneNames
			if (node.getGeneNames() != null && node.getGeneNames().contains(string))
				return node.getDbId();
		}
		// Reactions
		for (EventNode eventNode : graph.getEdges())
			if (eventNode.getStId().equalsIgnoreCase(string))
				return eventNode.getDbId();
		// Bad luck, not found
		return null;
	}

	private void decorateNodes(Collection<Long> selected, Collection<Long> flags) {
		if (selected.isEmpty() && flags.isEmpty())
			return;
		diagram.getNodes().forEach((Node node) -> {
			if (selected.contains(node.getReactomeId())) {
				final NodeDecorator decorator = getNodeDecorator(node.getId());
				decorator.setSelected(true);
				decorator.setHalo(true);
				// Only one node should be selected. If there is more than one
				// node selected, then take the last one.
				this.selected = decorator;
				node.getConnectors().forEach(connector -> {
					final Edge reaction = (Edge) diagramIndex.get(connector.getEdgeId());
					// When a node is selected, the nodes in the same reaction
					// are haloed
					getEdgeDecorator(reaction.getId()).setHalo(true);
					haloEdgeParticipants(reaction);
				});
			}
			if (flags.contains(node.getReactomeId()))
				getNodeDecorator(node.getId()).setFlag(true);
		});
	}

	private void decorateReactions(Collection<Long> selected, Collection<Long> flags) {
		diagram.getEdges().forEach(reaction -> {
			if (selected.contains(reaction.getReactomeId())) {
				final EdgeDecorator decorator = getEdgeDecorator(reaction.getId());
				decorator.setSelected(true);
				decorator.setHalo(true);
				haloEdgeParticipants(reaction);
			}
			if (flags.contains(reaction.getReactomeId()))
				getEdgeDecorator(reaction.getId()).setFlag(true);
		});
	}

	/**
	 * Adds the reaction to the haloReaction set, participating nodes to
	 * haloNodes and participating connectors to haloConnectors
	 *
	 * @param reaction reaction to halo
	 */
	private void haloEdgeParticipants(Edge reaction) {
		streamParticipants(reaction)
				.filter(node -> node.getIsFadeOut() == null || !node.getIsFadeOut())
				.forEach(node -> getNodeDecorator(node.getId()).setHalo(true));
	}

	/**
	 * Very convenient method to iterate over all of the activators, catalysts,
	 * inhibitors, inputs and outputs in a unique stream as diagram Nodes
	 *
	 * @param edge the chosen one
	 *
	 * @return a stream with all participant nodes in the reaction
	 */
	private Stream<Node> streamParticipants(Edge edge) {
		return Stream.of(edge.getActivators(), edge.getCatalysts(),
				edge.getInhibitors(), edge.getInputs(), edge.getOutputs())
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.map(part -> diagramIndex.get(part.getId()))
				.map(Node.class::cast);
	}

	/**
	 * Extracts analysis information and attaches it to each diagram node.
	 */
	private void addAnalysisData() throws AnalysisServerError, AnalysisException {
		if (args.getToken() == null || args.getToken().isEmpty())
			return;
		final String stId = graph.getStId();
		result = AnalysisClient.getAnalysisResult(args.getToken());
		final List<ResourceSummary> summaryList = result.getResourceSummary();
		final ResourceSummary resourceSummary = summaryList.size() == 2
				? summaryList.get(1)
				: summaryList.get(0);
		// result.getSummary().getFileName() seems to be null
		analysisName = result.getSummary().getSampleName();
		String resource = args.getResource() == null
				? resourceSummary.getResource()
				: args.getResource();
		analysisType = AnalysisType.getType(result.getSummary().getType());
		// Get subpathways (green boxes) % of analysis area
		subPathways(args.getToken(), resource);

		final FoundElements foundElements;
		try {
			foundElements = AnalysisClient.getFoundElements(stId, args.getToken(), resource);
			if (foundElements == null) return;
			if (analysisType == AnalysisType.EXPRESSION) {
				expression(foundElements);
			} else if (analysisType == AnalysisType.OVERREPRESENTATION ||
					analysisType == AnalysisType.SPECIES_COMPARISON)
				enrichment(foundElements);
		} catch (AnalysisException e) {
			// token is valid, but this pathway has no analysis
		}
	}

	/**
	 * Adds to the analysis index the subPathways present in the diagram (as
	 * ProcessNodes). So for each it takes its PathwaySummary and divides
	 * entities.getFound() / entities.getTotal() to compute the percentage of
	 * the fill area.
	 */
	private void subPathways(String token, String resource) throws AnalysisServerError, AnalysisException {
		// 1 extract list of dbIds for ProcessNodes
		final List<String> subPathways = diagram.getNodes().stream()
				.filter(node -> node.getRenderableClass().equals("ProcessNode"))
				.map(DiagramObject::getReactomeId)
				.map(String::valueOf)
				.collect(Collectors.toList());
		if (subPathways.isEmpty())
			return;
		// 2 get subPathways summary
		final PathwaySummary[] pathwaysSummary = AnalysisClient.getPathwaysSummary(subPathways, token, resource);
		// extract %
		for (PathwaySummary summary : pathwaysSummary) {
			final DiagramObject diagramNode = reactomeIndex.get(summary.getDbId());
			if (diagramNode == null) continue;
			final EntityStatistics entities = summary.getEntities();
			if (entities == null) continue;
			int found = entities.getFound();
			int total = entities.getTotal();
			double percentage = (double) found / total;
			if (percentage < MIN_ENRICHMENT && percentage > 0)
				percentage = MIN_ENRICHMENT;
			getNodeDecorator(diagramNode.getId()).setEnrichment(percentage);
		}
	}

	/**
	 * Computes the list of expressions of components. For each diagram object,
	 * except ProcessNodes, you get a list of lists of doubles
	 */
	private void expression(FoundElements foundElements) {
		// analysis -> graph: analysis.mapsTo.id.contains(graph.identifier)
		// graph -> layout:   layout.reactomeId == graph.dbId
		// Index analysis nodes via mapsTo
		final Map<String, FoundEntity> analysisIndex = new HashMap<>();
		foundElements.getEntities().forEach(analysisNode ->
				analysisNode.getMapsTo().stream()
						.map(IdentifierMap::getIds)
						.flatMap(Collection::stream)
						.forEach(id -> analysisIndex.put(id, analysisNode)));

		diagram.getNodes().forEach(diagramNode -> {
			final EntityNode graphNode = graphIndex.get(diagramNode.getReactomeId());
			if (graphNode == null) return;
			final List<FoundEntity> leaves = getLeaves(graphNode).stream()
					.map(leafId -> analysisIndex.get(graphIndex.get(leafId).getIdentifier()))
					.collect(Collectors.toList());
			final NodeDecorator decorator = getNodeDecorator(diagramNode.getId());
			decorator.setHitExpressions(leaves);
		});
	}

	/** Computes only the relation of hit found components and found component */
	private void enrichment(FoundElements foundElements) {
		// analysis -> graph: analysis.mapsTo.id.contains(graph.identifier)
		final Set<String> identifiers = foundElements.getEntities().stream()
				.map(FoundEntity::getMapsTo)
				.flatMap(Collection::stream)
				.map(IdentifierMap::getIds)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
		// graphNode.getIdentifier() -> graphNode.getDbId()
		final Set<Long> graphNodeHit = graph.getNodes().stream()
				.filter(entityNode -> identifiers.contains(entityNode.getIdentifier()))
				.map(GraphNode::getDbId)
				.collect(Collectors.toSet());
		// run through the diagram nodes and compute the enrichment level for
		// its associated graph node.
		diagram.getNodes().stream()
				.filter(node -> !node.getRenderableClass().equals("ProcessNode"))
				.forEach(diagramNode -> {
					final EntityNode graphNode = graphIndex.get(diagramNode.getReactomeId());
					if (graphNode != null) {
						double percentage = getPercentage(graphNodeHit, graphNode);
						getNodeDecorator(diagramNode.getId()).setEnrichment(percentage);
					}
				});
	}

	private double getPercentage(Set<Long> hitIds, EntityNode graphNode) {
		final Set<Long> leaves = getLeaves(graphNode);
		final int total = leaves.size();
		final long count = leaves.stream().filter(hitIds::contains).count();
		double percentage = (double) count / total;
		if (percentage > 0 && percentage < MIN_ENRICHMENT)
			percentage = MIN_ENRICHMENT;
		return percentage;
	}

	private Set<Long> getLeaves(EntityNode node) {
		if (node.getChildren() == null) {
			return Collections.singleton(node.getDbId());
		} else {
			return node.getChildren().stream()
					.map(graphIndex::get)
					.filter(Objects::nonNull)
					.map(this::getLeaves)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet());
		}
	}

	NodeDecorator getNodeDecorator(long id) {
		return (NodeDecorator) index.computeIfAbsent(id, k -> new NodeDecorator());
	}

	EdgeDecorator getEdgeDecorator(long id) {
		return (EdgeDecorator) index.computeIfAbsent(id, k -> new EdgeDecorator());
	}

	public AnalysisType getAnalysisType() {
		return analysisType;
	}

	public NodeDecorator getSelected() {
		return selected;
	}

	public String getAnalysisName() {
		return analysisName;
	}

	public AnalysisResult getResult() {
		return result;
	}

	/**
	 * Contains decorator information for a DiagramObject: flag, selection and
	 * halo.
	 */
	public abstract class DiagramObjectDecorator {
		private boolean flag = false;
		private boolean selected = false;
		private boolean halo = false;

		public boolean isFlag() {
			return flag;
		}

		void setFlag(boolean flag) {
			this.flag = flag;
		}

		public boolean isSelected() {
			return selected;
		}

		void setSelected(boolean selected) {
			this.selected = selected;
		}

		public boolean isHalo() {
			return halo;
		}

		void setHalo(boolean halo) {
			this.halo = halo;
		}
	}

	/**
	 * Contains extra rendering information for an edge: decorators plus
	 * connectors.
	 */
	public class EdgeDecorator extends DiagramObjectDecorator {
		private List<Connector> connectors = new LinkedList<>();

		public List<Connector> getConnectors() {
			return connectors;
		}
	}

	/**
	 * Contains extra rendering data for a Node: decorators plus expression
	 * values or enrichment value.
	 */
	public class NodeDecorator extends DiagramObjectDecorator {
		private List<FoundEntity> hitExpressions = null;
		private Double enrichment = null;
		private Integer totalExpressions;

		public Double getEnrichment() {
			return enrichment;
		}

		void setEnrichment(Double enrichment) {
			this.enrichment = enrichment;
		}

		public List<FoundEntity> getHitExpressions() {
			return hitExpressions;
		}

		void setHitExpressions(List<FoundEntity> hitExpressions) {
			this.hitExpressions = hitExpressions.stream()
					.filter(Objects::nonNull)
					.sorted((Comparator.comparing(IdentifierSummary::getId)))
					.collect(Collectors.toList());
			this.totalExpressions = hitExpressions.size();
		}

		public Integer getTotalExpressions() {
			return totalExpressions;
		}
	}
}