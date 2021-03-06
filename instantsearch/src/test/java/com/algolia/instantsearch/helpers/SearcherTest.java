package com.algolia.instantsearch.helpers;

import android.support.annotation.NonNull;

import com.algolia.instantsearch.Helpers;
import com.algolia.instantsearch.InstantSearchTest;
import com.algolia.instantsearch.model.AlgoliaResultsListener;
import com.algolia.instantsearch.model.NumericRefinement;
import com.algolia.instantsearch.model.SearchResults;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.Query;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Locale;

public class SearcherTest extends InstantSearchTest {
    @NonNull
    private Searcher initSearcher() {
        final Client client = new Client(Helpers.app_id, Helpers.api_key);
        return new Searcher(client.getIndex(Helpers.safeIndexName("test")));
    }

    @Test
    public void hasHitsFalseIfEmpty() {
        boolean output = Searcher.hasHits(null);
        //noinspection ConstantConditions warning means test passes for IDE ;)
        Assert.assertFalse("hasHits should return false on null input", output);
    }

    @Test
    public void hasHitsFalseIfNoHits() throws JSONException {
        JSONObject input = new JSONObject("{'name':'foo'}");
        boolean output = Searcher.hasHits(input);
        Assert.assertFalse("hasHits should return false on input with no hits", output);
    }

    @Test
    public void hasHitsFalseIfNullHitsArray() throws JSONException {
        JSONObject input = new JSONObject("{'hits':null}");
        boolean output = Searcher.hasHits(input);
        Assert.assertFalse("hasHits should return false on input with null hits array", output);
    }

    @Test
    public void hasHitsFalseIfNullHits() throws JSONException {
        JSONObject input = new JSONObject("{'hits':[null, null]}");
        boolean output = Searcher.hasHits(input);
        Assert.assertFalse("hasHits should return false on input with null hits", output);
    }

    @Test
    public void hasHitsTrueIfHits() throws JSONException {
        JSONObject input = new JSONObject("{'hits':[{'name':'foo'}]}");
        boolean output = Searcher.hasHits(input);
        Assert.assertTrue("hasHits should return true on input with some hits", output);
    }

    @Test
    public void canCancelPendingRequests() {
        Searcher searcher = initSearcher();
        final AlgoliaResultsListener resultsListener = new AlgoliaResultsListener() {
            @Override
            public void onResults(@NonNull SearchResults results, boolean isLoadingMore) {
                Assert.fail("The request should have been cancelled.");
            }

            @Override
            public void onError(Query query, AlgoliaException error) {
                Assert.fail("The request should have been cancelled.");
            }
        };
        searcher.registerListener(resultsListener);
        searcher.search();
        Assert.assertTrue("There should be a pending request", searcher.hasPendingRequests());
        searcher.cancelPendingRequests();
    }

    @Test
    public void numericRefinements() {
        Searcher searcher = initSearcher();
        final NumericRefinement r = new NumericRefinement("attribute", NumericRefinement.OPERATOR_EQ, 42);
        final NumericRefinement r2 = new NumericRefinement("attribute", NumericRefinement.OPERATOR_NE, 42);
        final String formattedValue = String.format(Locale.US, "%f", 42f);

        searcher.addNumericRefinement(r);
        Assert.assertEquals("There should be a numeric refinement for attribute", r, searcher.getNumericRefinement(r.attribute, r.operator));
        Assert.assertEquals("Query numericFilters should represent the refinement", "[\"attribute=" + formattedValue + "\"]", searcher.getQuery().getNumericFilters().toString());

        searcher.removeNumericRefinement(r);
        Assert.assertEquals("This numeric refinement should have been removed.", null, searcher.getNumericRefinement(r.attribute, r.operator));
        Assert.assertEquals("Query numericFilters should be empty after removal", "[]", searcher.getQuery().getNumericFilters().toString());

        searcher.addNumericRefinement(r);
        searcher.addNumericRefinement(r2);
        Assert.assertEquals("Query numericFilters should represent both refinements", "[\"attribute=" + formattedValue + "\",\"attribute!=" + formattedValue + "\"]", searcher.getQuery().getNumericFilters().toString());

        searcher.removeNumericRefinement(r.attribute);
        Assert.assertEquals("Both numeric refinements for this attribute should have been removed", null, searcher.getNumericRefinement(r.attribute, r.operator));
        Assert.assertEquals("Query numericFilters should be empty after removal", "[]", searcher.getQuery().getNumericFilters().toString());

        searcher.addNumericRefinement(r);
        searcher.addNumericRefinement(r2);
        searcher.removeNumericRefinement(r.attribute, r.operator);
        Assert.assertEquals("The numeric refinement for this attribute/operator pair should have been removed", null, searcher.getNumericRefinement(r.attribute, r.operator));
        Assert.assertEquals("The numeric refinement for this attribute but other operator should have been kept", r2, searcher.getNumericRefinement(r2.attribute, r2.operator));
    }

    @SuppressWarnings("deprecation") // deprecated facetFilters are used on purpose for filters managed programmatically
    @Test
    public void facetRefinements() {
        final Searcher searcher = initSearcher();
        searcher.addFacetRefinement("attribute", "foo");
        Assert.assertEquals("facetFilters should represent the refinement", "[\"attribute:foo\"]", searcher.getQuery().getFacetFilters().toString());
        Assert.assertTrue("hasFacetRefinement should return true for attribute/foo", searcher.hasFacetRefinement("attribute", "foo"));

        searcher.removeFacetRefinement("attribute", "foo");
        Assert.assertEquals("facetFilters should not contain the refinement after removeFacetRefinement()", "[]", searcher.getQuery().getFacetFilters().toString());
        Assert.assertFalse("hasFacetRefinement should return false for attribute/foo", searcher.hasFacetRefinement("attribute", "foo"));

        searcher.updateFacetRefinement("attribute", "foo", true);
        Assert.assertEquals("facetFilters should represent again the refinement", "[\"attribute:foo\"]", searcher.getQuery().getFacetFilters().toString());

        searcher.updateFacetRefinement("attribute", "foo", false);
        Assert.assertEquals("facetFilters should not contain the refinement after updateFacetRefinement(false)", "[]", searcher.getQuery().getFacetFilters().toString());

        searcher.addFacetRefinement("attribute", "foo");
        searcher.addFacetRefinement("attribute", "bar");
        Assert.assertTrue("facetFilters should contain the first refinement", searcher.getQuery().getFacetFilters().toString().contains("attribute:foo"));
        Assert.assertTrue("facetFilters should contain the second refinement", searcher.getQuery().getFacetFilters().toString().contains("attribute:bar"));

        searcher.removeFacetRefinement("attribute", "foo");
        Assert.assertFalse("facetFilters should not contain the first refinement anymore", searcher.getQuery().getFacetFilters().toString().contains("attribute:foo"));
        Assert.assertTrue("facetFilters should still contain the second refinement", searcher.getQuery().getFacetFilters().toString().contains("attribute:bar"));

        searcher.addFacetRefinement("attribute", "foo");
        searcher.addFacetRefinement("other", "baz");
        searcher.clearFacetRefinements("attribute");
        Assert.assertFalse("facetFilters should have no more refinements on attribute", searcher.getQuery().getFacetFilters().toString().contains("attribute"));
        Assert.assertTrue("facetFilters should still contain the other attribute's refinement", searcher.getQuery().getFacetFilters().toString().contains("other:baz"));
    }
}
