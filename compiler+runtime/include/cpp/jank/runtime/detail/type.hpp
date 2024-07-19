#pragma once

#include <immer/vector.hpp>
#include <immer/vector_transient.hpp>
#include <immer/map.hpp>
#include <immer/map_transient.hpp>
#include <immer/set.hpp>
#include <immer/set_transient.hpp>
#include <immer/memory_policy.hpp>

#include <bpptree/bpptree.hpp>
#include <bpptree/ordered.hpp>

#include <jank/runtime/object.hpp>

namespace jank::runtime
{
  native_integer compare(object_ptr, object_ptr);

  namespace detail
  {
    native_bool equal(char const lhs, object_ptr const rhs);
    native_bool equal(object_ptr const lhs, object_ptr const rhs);

    struct object_ptr_equal
    {
      static native_bool equal(object_ptr const l, object_ptr const r)
      {
        return detail::equal(l, r);
      }

      inline native_bool operator()(object_ptr const l, object_ptr const r) const
      {
        return detail::equal(l, r);
      }
    };

    struct object_ptr_compare
    {
      inline native_bool operator()(object_ptr const l, object_ptr const r) const
      {
        return runtime::compare(l, r) < 0;
      }
    };

    using native_persistent_vector = immer::vector<object_ptr, memory_policy>;
    using native_transient_vector = native_persistent_vector::transient_type;

    using native_persistent_hash_set
      = immer::set<object_ptr, std::hash<object_ptr>, object_ptr_equal, memory_policy>;
    using native_transient_hash_set = native_persistent_hash_set::transient_type;

    using native_persistent_sorted_set
      = bpptree::BppTreeSet<object_ptr, object_ptr_compare>::Persistent;
    using native_transient_sorted_set
      = bpptree::BppTreeSet<object_ptr, object_ptr_compare>::Transient;

    using native_persistent_hash_map = immer::
      map<object_ptr, object_ptr, std::hash<object_ptr>, object_ptr_equal, jank::memory_policy>;
    using native_transient_hash_map = native_persistent_hash_map::transient_type;

    using native_persistent_sorted_map
      = bpptree::BppTreeMap<object_ptr, object_ptr, object_ptr_compare>::Persistent;
    using native_transient_sorted_map
      = bpptree::BppTreeMap<object_ptr, object_ptr, object_ptr_compare>::Transient;
  }
}
