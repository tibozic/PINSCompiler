fun f(x: integer) : integer = (
    {g(1) = g(1) + h(1,2,3)}
){where var r: integer; var c: integer};


fun g(x: integer) : integer = 1 + 2 + 3;

fun h(x: integer, y: integer, z: integer) : integer = 1 + 2 + 3
