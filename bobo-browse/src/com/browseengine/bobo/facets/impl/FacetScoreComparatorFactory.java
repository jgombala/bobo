package com.browseengine.bobo.facets.impl;

import java.util.Comparator;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.util.IntBoundedPriorityQueue.IntComparator;

public class FacetScoreComparatorFactory implements ComparatorFactory {
  public IntComparator newComparator(FieldValueAccessor valueList, final int[] count,
      final int[] score) {
    return new IntComparator(){

      public int compare(Integer f1, Integer f2) {
        int val = score[f1] - score[f2];
        if (val==0)
        {
          val=f2-f1;
        }
        return val;
      }

      // use ploymorphism to avoid auto-boxing
      public int compare(int f1, int f2)
      {
        int val = score[f1] - score[f2];
        if (val==0)
        {
          val=f2-f1;
        }
        return val;
      }

    };
  }

  public static final Comparator<BrowseFacet> FACET_SCORE_COMPARATOR = new Comparator<BrowseFacet>()
  {
    public int compare(BrowseFacet f1, BrowseFacet f2) {
      int val = f2.getFacetValueScore() - f1.getFacetValueScore();
      if (val==0)
      {
        val=f1.getValue().compareTo(f2.getValue());
      }
      return val;
    }		
  };

  public Comparator<BrowseFacet> newComparator() {
    return FACET_SCORE_COMPARATOR;
  }
}
