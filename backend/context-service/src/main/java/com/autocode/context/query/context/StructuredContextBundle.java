package com.autocode.context.query.context;

import java.util.List;

public record StructuredContextBundle(
        List<String> facts,
        List<String> relatedNodes,
        String historySummary,
        String documentSummary
) {
}
