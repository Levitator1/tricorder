#pragma once

/*
 * Various loosely typed predicates
 */

#include <algorithm>

namespace jab{
namespace func{

//
// predicates for arbitrary types
// assuming inputs are const
//
struct logical_or{
    template<typename Lhs, typename Rhs>
    auto operator()(const Lhs &lhs, const Rhs &rhs) const{
        return lhs || rhs;
    }
};

struct logical_and{
    template<typename Lhs, typename Rhs>
    auto operator()(const Lhs &lhs, const Rhs &rhs ) const{
        return lhs && rhs;
    }
};

struct add{
    template<typename Lhs, typename Rhs>
    auto operator()(const Lhs &lhs, const Rhs &rhs) const{
        return lhs + rhs;
    }
};

struct subtract{
    template<typename Lhs, typename Rhs>
    auto operator()(const Lhs &lhs, const Rhs &rhs) const{
        return lhs - rhs;
    }
};

//base for the two binary logical operators, both 'and' and 'or'
namespace impl{
template< typename OP >
class logical_binary_anything{
    
    static constexpr OP op={};
    
public:
    template<typename Lhs, typename Rhs>
    constexpr auto operator()(const Lhs &lhs, const Rhs &rhs) const{
        return op(lhs, rhs);
    }
    
    //For 1 argument return v || v, or v && v
    template<typename T>
    constexpr auto operator()(const T &v) const{
        return op(v, v);
    }
    
    //For zero arguments, return false
    constexpr bool operator()() const{ return false; }
};
}    

//Logical-or of 0-2 things
struct logical_or_anything:public impl::logical_binary_anything<logical_or>{};

//Logical-and 0-2 things
struct logical_and_anything:public impl::logical_binary_anything<logical_and>{};

//Both +/-
// What's a general word for both addition and subtraction?
namespace impl{
    
template<class OP>
struct sum_anything_base{
    
    static constexpr OP op = {};
    
    template<typename Lhs, typename Rhs>
    auto operator()(const Lhs &lhs, const Rhs &rhs) const{
        return op(lhs, rhs);
    }
    
    template<typename T>
    auto operator()(const T &v) const{
        return op(v, 0); //v +/- 0;
    }
    
    //Zero arguments yield 0
    constexpr int operator()() const{ return 0; }
};
}

//Sum of 0-2 things
struct add_anything:public impl::sum_anything_base<add>{};

//Sum of 0-2 things
struct subtract_anything:public impl::sum_anything_base<subtract>{};

namespace impl{

}

//Return the greater of any 1-2 things
//No 0-item case. If you want a default, then add 1 argument
struct max_anything{
    
    //Return by-value the larger of two disparate things
    template<typename T, typename U>
    constexpr auto operator()(const T &lhs, const U &rhs) const{
        return lhs >= rhs ? lhs : rhs;
    }
    
    //Return by-reference the larger of two like things
    template<typename T>
    constexpr const T &operator()(const T &lhs, const T &rhs) const{
        return std::max(lhs, rhs);
    }
    
    template<typename T>
    constexpr const T &operator()(const T &v) const{ return v; }
    
};

}    
}
