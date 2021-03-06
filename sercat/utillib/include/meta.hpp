#pragma once
#include <type_traits>

/*
 * Metaprogramming stuff
 * 
 */

namespace jab{
namespace meta{


//A function that accepts zero or more arguments and does nothing
//Provides a context for expanding parameter packs
template<typename... Args>
void null_function(Args... args){}

//Demand a specific type. Useful for variadic arguments
template<typename T, typename U>
using permit_type = typename std::enable_if<std::is_same<T, U>::value, T>::type;

//Deterimne whether T is the same type as any in Args...
template<typename T, typename... Args>
struct is_any;

template<typename T, typename U>
struct is_any<T, U>:public std::integral_constant<bool, std::is_same<T, U>::value>{};

template<typename T, typename U, typename... Args>
struct is_any<T, U, Args...>:public std::integral_constant<bool, std::is_same<T, U>::value || is_any<T, Args...>::value>{
};

template<typename T, typename... Args>
using permit_types = typename std::enable_if<is_any<T, Args...>::value, T>::type;

template<typename T, typename U>
using const_optional = permit_types<T, U, const U>;

template<typename T, typename U>
using permit_if_converts_to = typename std::enable_if<std::is_convertible<T, U>::value, T>::type;

}
}
