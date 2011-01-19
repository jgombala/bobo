package com.browseengine.bobo.facets.impl;

import java.util.List;
import java.util.NoSuchElementException;

import com.browseengine.bobo.api.FloatFacetIterator;

/**
 * @author "Xiaoyang Gu<xgu@linkedin.com>"
 * 
 */
public class CombinedFloatFacetIterator extends FloatFacetIterator
{

  public float facet;

  private static class FloatIteratorNode
  {
    public FloatFacetIterator _iterator;
    public float _curFacet;
    public int _curFacetCount;
    public int _curFacetScore;

    public FloatIteratorNode(FloatFacetIterator iterator)
    {
      _iterator = iterator;
      _curFacet = -1;
      _curFacetCount = 0;
      _curFacetScore = 0;
    }

    public boolean fetch(int minHits)
    {
      if (minHits > 0)
        minHits = 1;
      if ((_curFacet = _iterator.nextFloat(minHits)) != -1)
      {
        _curFacetCount = _iterator.count;
        _curFacetScore = _iterator.score;
        return true;
      }
      _curFacet = -1;
      _curFacetCount = 0;
      _curFacetScore = 0;
      return false;
    }

    public String peek()// bad
    {
      throw new UnsupportedOperationException();
      // if(_iterator.hasNext())
      // {
      // return _iterator.getFacet();
      // }
      // return null;
    }
  }

  private final FloatFacetPriorityQueue _queue;

  private List<FloatFacetIterator> _iterators;

  private CombinedFloatFacetIterator(final int length)
  {
    _queue = new FloatFacetPriorityQueue();
    _queue.initialize(length);
  }

  public CombinedFloatFacetIterator(final List<FloatFacetIterator> iterators)
  {
    this(iterators.size());
    _iterators = iterators;
    for (FloatFacetIterator iterator : iterators)
    {
      FloatIteratorNode node = new FloatIteratorNode(iterator);
      if (node.fetch(1))
        _queue.add(node);
    }
    facet = -1;
    count = 0;
    score = 0;
  }

  public CombinedFloatFacetIterator(final List<FloatFacetIterator> iterators,
      int minHits)
  {
    this(iterators.size());
    _iterators = iterators;
    for (FloatFacetIterator iterator : iterators)
    {
      FloatIteratorNode node = new FloatIteratorNode(iterator);
      if (node.fetch(minHits))
        _queue.add(node);
    }
    facet = -1;
    count = 0;
    score = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.browseengine.bobo.api.FacetIterator#getFacet()
   */
  public String getFacet()
  {
    if (facet == -1) return null;
    return format(facet);
  }

  public String format(float val)
  {
    return _iterators.get(0).format(val);
  }

  public String format(Object val)
  {
    return _iterators.get(0).format(val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
   */
  public int getFacetCount()
  {
    return count;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.browseengine.bobo.api.FacetIterator#next()
   */
  public String next()
  {
    if (!hasNext())
      throw new NoSuchElementException("No more facets in this iteration");

    FloatIteratorNode node = (FloatIteratorNode) _queue.top();

    facet = node._curFacet;
    float next = -1;
    count = 0;
    score = 0;
    while (hasNext())
    {
      node = (FloatIteratorNode) _queue.top();
      next = node._curFacet;
      if ((next != -1) && (next != facet))
      {
        return format(facet);
      }
      count += node._curFacetCount;
      score += node._curFacetScore;
      if (node.fetch(1))
        _queue.updateTop();
      else
        _queue.pop();
    }
    return null;
  }

  /**
   * This version of the next() method applies the minHits from the facet spec
   * before returning the facet and its hitcount
   * 
   * @param minHits
   *          the minHits from the facet spec for CombinedFacetAccessible
   * @return The next facet that obeys the minHits
   */
  public String next(int minHits)
  {
    int qsize = _queue.size();
    if (qsize == 0)
    {
      facet = -1;
      count = 0;
      score = 0;
      return null;
    }

    FloatIteratorNode node = (FloatIteratorNode) _queue.top();
    facet = node._curFacet;
    count = node._curFacetCount;
    score = node._curFacetScore;
    while (true)
    {
      if (node.fetch(minHits))
      {
        node = (FloatIteratorNode) _queue.updateTop();
      } else
      {
        _queue.pop();
        if (--qsize > 0)
        {
          node = (FloatIteratorNode) _queue.top();
        } else
        {
          // we reached the end. check if this facet obeys the minHits
          if (count < minHits)
          {
            facet = -1;
            count = 0;
            score = 0;
            return null;
          }
          break;
        }
      }
      float next = node._curFacet;
      if (next != facet)
      {
        // check if this facet obeys the minHits
        if (count >= minHits)
          break;
        // else, continue iterating to the next facet
        facet = next;
        count = node._curFacetCount;
        score = node._curFacetScore;
      } else
      {
        count += node._curFacetCount;
        score += node._curFacetScore;
      }
    }
    return format(facet);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext()
  {
    return (_queue.size() > 0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  public void remove()
  {
    throw new UnsupportedOperationException(
        "remove() method not supported for Facet Iterators");
  }

  /**
   * Lucene PriorityQueue
   * 
   */
  public static class FloatFacetPriorityQueue
  {
    private int size;
    private int maxSize;
    protected FloatIteratorNode[] heap;

    /** Subclass constructors must call this. */
    protected final void initialize(int maxSize)
    {
      size = 0;
      int heapSize;
      if (0 == maxSize)
        // We allocate 1 extra to avoid if statement in top()
        heapSize = 2;
      else
        heapSize = maxSize + 1;
      heap = new FloatIteratorNode[heapSize];
      this.maxSize = maxSize;
    }

    public final void put(FloatIteratorNode element)
    {
      size++;
      heap[size] = element;
      upHeap();
    }

    public final FloatIteratorNode add(FloatIteratorNode element)
    {
      size++;
      heap[size] = element;
      upHeap();
      return heap[1];
    }

    public boolean insert(FloatIteratorNode element)
    {
      return insertWithOverflow(element) != element;
    }

    public FloatIteratorNode insertWithOverflow(FloatIteratorNode element)
    {
      if (size < maxSize)
      {
        put(element);
        return null;
      } else if (size > 0 && !(element._curFacet < heap[1]._curFacet))
      {
        FloatIteratorNode ret = heap[1];
        heap[1] = element;
        adjustTop();
        return ret;
      } else
      {
        return element;
      }
    }

    /** Returns the least element of the PriorityQueue in constant time. */
    public final FloatIteratorNode top()
    {
      // We don't need to check size here: if maxSize is 0,
      // then heap is length 2 array with both entries null.
      // If size is 0 then heap[1] is already null.
      return heap[1];
    }

    /**
     * Removes and returns the least element of the PriorityQueue in log(size)
     * time.
     */
    public final FloatIteratorNode pop()
    {
      if (size > 0)
      {
        FloatIteratorNode result = heap[1]; // save first value
        heap[1] = heap[size]; // move last to first
        heap[size] = null; // permit GC of objects
        size--;
        downHeap(); // adjust heap
        return result;
      } else
        return null;
    }

    public final void adjustTop()
    {
      downHeap();
    }

    public final FloatIteratorNode updateTop()
    {
      downHeap();
      return heap[1];
    }

    /** Returns the number of elements currently stored in the PriorityQueue. */
    public final int size()
    {
      return size;
    }

    /** Removes all entries from the PriorityQueue. */
    public final void clear()
    {
      for (int i = 0; i <= size; i++)
      {
        heap[i] = null;
      }
      size = 0;
    }

    private final void upHeap()
    {
      int i = size;
      FloatIteratorNode node = heap[i]; // save bottom node
      int j = i >>> 1;
      while (j > 0 && (node._curFacet < heap[j]._curFacet))
      {
        heap[i] = heap[j]; // shift parents down
        i = j;
        j = j >>> 1;
      }
      heap[i] = node; // install saved node
    }

    private final void downHeap()
    {
      int i = 1;
      FloatIteratorNode node = heap[i]; // save top node
      int j = i << 1; // find smaller child
      int k = j + 1;
      if (k <= size && (heap[k]._curFacet < heap[j]._curFacet))
      {
        j = k;
      }
      while (j <= size && (heap[j]._curFacet < node._curFacet))
      {
        heap[i] = heap[j]; // shift up child
        i = j;
        j = i << 1;
        k = j + 1;
        if (k <= size && (heap[k]._curFacet < heap[j]._curFacet))
        {
          j = k;
        }
      }
      heap[i] = node; // install saved node
    }
  }

  @Override
  public float nextFloat()
  {
    if (!hasNext())
      throw new NoSuchElementException("No more facets in this iteration");

    FloatIteratorNode node = (FloatIteratorNode) _queue.top();

    facet = node._curFacet;
    float next = -1;
    count = 0;
    score = 0;
    while (hasNext())
    {
      node = (FloatIteratorNode) _queue.top();
      next = node._curFacet;
      if ((next != -1) && (next != facet))
      {
        return facet;
      }
      count += node._curFacetCount;
      score += node._curFacetScore;
      if (node.fetch(1))
        _queue.updateTop();
      else
        _queue.pop();
    }
    return -1;
  }

  @Override
  public float nextFloat(int minHits)
  {
    int qsize = _queue.size();
    if (qsize == 0)
    {
      facet = -1;
      count = 0;
      score = 0;
      return -1;
    }

    FloatIteratorNode node = (FloatIteratorNode) _queue.top();
    facet = node._curFacet;
    count = node._curFacetCount;
    score = node._curFacetScore;
    while (true)
    {
      if (node.fetch(minHits))
      {
        node = (FloatIteratorNode) _queue.updateTop();
      } else
      {
        _queue.pop();
        if (--qsize > 0)
        {
          node = (FloatIteratorNode) _queue.top();
        } else
        {
          // we reached the end. check if this facet obeys the minHits
          if (count < minHits)
          {
            facet = -1;
            count = 0;
            score = 0;
          }
          break;
        }
      }
      float next = node._curFacet;
      if (next != facet)
      {
        // check if this facet obeys the minHits
        if (count >= minHits)
          break;
        // else, continue iterating to the next facet
        facet = next;
        count = node._curFacetCount;
        score = node._curFacetScore;
      } else
      {
        count += node._curFacetCount;
        score += node._curFacetScore;
      }
    }
    return facet;
  }
}