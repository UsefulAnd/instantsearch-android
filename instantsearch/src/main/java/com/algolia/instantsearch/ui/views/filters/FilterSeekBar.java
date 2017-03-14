package com.algolia.instantsearch.ui.views.filters;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.algolia.instantsearch.R;
import com.algolia.instantsearch.helpers.Searcher;
import com.algolia.instantsearch.model.Errors;
import com.algolia.instantsearch.model.NumericRefinement;
import com.algolia.instantsearch.model.SearchResults;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Query;

public class FilterSeekBar extends AppCompatSeekBar implements AlgoliaFacetFilter {
    private final String attributeName;
    private final boolean autoBounds;
    private final double boundMin;
    private final double boundMax;

    private final int[] lastProgressValue = new int[1];

    public FilterSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray styledAttributesFilter = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Filter, 0, 0);
        final TypedArray styledAttributesSeekBar = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FilterSeekBar, 0, 0);
        try {
            attributeName = styledAttributesFilter.getString(R.styleable.Filter_attributeName);
            if (attributeName == null) {
                throw new IllegalStateException(Errors.FILTER_MISSING_ATTRIBUTE);
            }
            autoBounds = styledAttributesFilter.getBoolean(R.styleable.FilterSeekBar_autoBounds, false);
            boundMin = styledAttributesFilter.getFloat(R.styleable.FilterSeekBar_boundMin, 0f);
            boundMax = styledAttributesFilter.getFloat(R.styleable.FilterSeekBar_boundMax, 100f);
        } finally {
            styledAttributesFilter.recycle();
            styledAttributesSeekBar.recycle();
        }
    }

    @NonNull @Override public String getAttributeName() {
        return attributeName;
    }

    @Override public void initWithSearcher(@NonNull final Searcher searcher) {
        setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onUpdate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onUpdate();
            }

            private void onUpdate() {
                int newProgressValue = getProgress(); // avoid double search on ProgressChanged + StopTrackingTouch
                if (newProgressValue != lastProgressValue[0]) {
                    final double value = boundMin + getProgress() * (boundMax - boundMin) / getMax();
                    if (newProgressValue == 0) {
                        searcher.removeNumericRefinement(new NumericRefinement(attributeName, NumericRefinement.OPERATOR_GT, value))
                                .search();
                    } else {
                        searcher.addNumericRefinement(new NumericRefinement(attributeName, NumericRefinement.OPERATOR_GT, value))
                                .search();
                    }
                    lastProgressValue[0] = newProgressValue;
                }
            }
        });
    }

    @Override public void onReset() {
    }

    @Override public void onResults(SearchResults results, boolean isLoadingMore) {

    }

    @Override public void onError(Query query, AlgoliaException error) {

    }
}