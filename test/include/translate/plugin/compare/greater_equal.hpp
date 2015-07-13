#pragma once

#include <jest/jest.hpp>

#include "common/interpret.hpp"

namespace jank
{
  struct greater_equal_test
  { };
  using greater_equal_group = jest::group<greater_equal_test>;
  static greater_equal_group const greater_equal_obj{ "comparison >=" };
}

namespace jest
{
  template <> template <>
  void jank::greater_equal_group::test<0>()
  { jank::common::interpret("translate/plugin/compare/greater_equal/pass_integer.jank"); }

  template <> template <>
  void jank::greater_equal_group::test<1>()
  { jank::common::interpret("translate/plugin/compare/greater_equal/pass_real.jank"); }

  template <> template <>
  void jank::greater_equal_group::test<2>()
  { jank::common::interpret("translate/plugin/compare/greater_equal/pass_string.jank"); }
}
