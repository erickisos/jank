#pragma once

#include <jank/runtime/object.hpp>

namespace jank::runtime
{
  namespace obj
  {
    using map = static_object<object_type::map>;
    using map_ptr = native_box<map>;
  }

  template <>
  struct static_object<object_type::persistent_map_sequence> : gc
  {
    static constexpr bool pointer_free{ false };

    using iterator_type = runtime::detail::persistent_map::const_iterator;

    static_object() = default;
    static_object(static_object &&) = default;
    static_object(static_object const &) = default;
    static_object(object &&base);
    static_object(object_ptr c, iterator_type const &b, iterator_type const &e);

    /* behavior::objectable */
    native_bool equal(object const &) const;
    void to_string_impl(fmt::memory_buffer &buff) const;
    void to_string(fmt::memory_buffer &buff) const;
    native_string to_string() const;
    native_integer to_hash() const;

    /* behavior::countable */
    size_t count() const;

    /* behavior::seqable */
    native_box<static_object> seq();
    native_box<static_object> fresh_seq() const;

    /* behavior::sequenceable */
    object_ptr first() const;
    native_box<static_object> next() const;
    native_box<static_object> next_in_place();
    object_ptr next_in_place_first();
    obj::cons_ptr cons(object_ptr head);

    object base{ object_type::persistent_vector_sequence };
    object_ptr coll{};
    iterator_type begin{}, end{};
  };

  namespace obj
  {
    using persistent_map_sequence = static_object<object_type::persistent_map_sequence>;
    using persistent_map_sequence_ptr = native_box<persistent_map_sequence>;
  }
}
