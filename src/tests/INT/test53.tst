typ int: integer;

fun main(x: integer): integer = (
    { i = 0 },
    { while i < 15: (
        print_int(i),
        { i = increment(i) }
    )},
    i
) { where 
    var i: int;
    fun increment(x: integer): integer = (
        { x = x + 1 },
        x
    )
}
