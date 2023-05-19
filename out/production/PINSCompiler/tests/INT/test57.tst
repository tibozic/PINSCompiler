fun main(x: integer): integer = (
    { if larger_than_10(5) then
        print_str('5 is larger than 10')
      else
        print_str('5 is not larger than 10')
    },
    { if larger_than_10(20) then
        print_str('20 is larger than 10')
      else
        print_str('20 is not larger than 10')
    },
    print_log(f(5, 10)),
    print_log(f(10, 5)),
    0
) { where
    typ boolean: logical;
    fun f(x1: integer, x2: integer): boolean = (
        { if x1 < x2 then
            print_str('x1 < x2')
        else
            print_str('x1 >= x2')
        },
        true
    );
    fun larger_than_10(x1: integer): boolean = (
        { larger = false },
        { if x1 > 10 then
            { larger = true }
        },
        larger
    ) { where
        var larger: logical
    }
}
