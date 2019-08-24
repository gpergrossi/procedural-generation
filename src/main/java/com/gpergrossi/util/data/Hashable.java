package com.gpergrossi.util.data;

/**
 * Explicitly marks classes as having a valid hash implementation
 */
public interface Hashable {

   @Override
   int hashCode();
   
   @Override
   boolean equals(Object obj);
   
}
