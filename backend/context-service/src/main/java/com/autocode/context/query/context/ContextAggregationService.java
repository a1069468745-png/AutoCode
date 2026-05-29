package com.autocode.context.query.context;

import com.autocode.context.query.QueryIntent;
import com.autocode.context.query.adapter.QuerySourceType;
import com.autocode.context.query.adapter.StandardQueryHit;
import com.autocode.context.query.adapter.StandardQueryResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContextAggregationService {
    public StructuredContextBundle aggregate(QueryIntent intent, StandardQueryResult result) {
        List<String> facts = new ArrayList<>();
        List<String> relatedNodes = new ArrayList<>();
        List<String> historyFacts = new ArrayList<>();
        List<String> documentFacts = new ArrayList<>();

        // Convert heterogeneous adapter hits into a stable, intent-oriented context package.
        for (StandardQueryHit hit : result.hits()) {
            facts.add(hit.title() + ": " + hit.snippet());
            if (hit.sourceType() == QuerySourceType.CODE_GRAPH) {
                relatedNodes.add(hit.id());
            } else if (hit.sourceType() == QuerySourceType.HISTORY) {
                historyFacts.add(hit.snippet());
            } else if (hit.sourceType() == QuerySourceType.DOCUMENT) {
                documentFacts.add(hit.title());
            }
        }

        String historySummary = historyFacts.isEmpty()
                ? "no history summary"
                : String.join("; ", historyFacts);
        String documentSummary = documentFacts.isEmpty()
                ? "no document summary"
                : String.join("; ", documentFacts);

        // Keep intent in this layer for future intent-specific aggregation strategy extension.
        if (intent == QueryIntent.HISTORY_TRACE && historyFacts.isEmpty()) {
            historySummary = "history intent requested but no history records found";
        }
        if (intent == QueryIntent.DOCUMENT_TRACE && documentFacts.isEmpty()) {
            documentSummary = "document intent requested but no document records found";
        }

        return new StructuredContextBundle(facts, relatedNodes, historySummary, documentSummary);
    }
}
