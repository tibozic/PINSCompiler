var b1: logical;
var b2: logical;
var b3: logical;
var b4: logical;
var b5: logical;
var b6: logical;

fun main(x: integer): integer = (
    { b1 = true },
    { b2 = false },
    { b3 = b1 == b2 },
    { b4 = b1 & b2 },
    { b5 = b1 | b2 },
    { b6 = !b1 },
    print_log(b3),
    print_log(b4),
    print_log(b5),
    print_log(b6),
    print_log(b1 == !b2),
    f1(b1, !b2),
    x
);

fun f1(b1: logical, b2: logical): logical = (
    { b3 = b1 == b2 },
    { b4 = b1 & b2 },
    { b5 = b1 | b2 },
    { b6 = !b1 },
    print_log(b3),
    print_log(b4),
    print_log(b5),
    print_log(b6),
    b1
) { where
    var b3: logical;
    var b4: logical;
    var b5: logical;
    var b6: logical
}
