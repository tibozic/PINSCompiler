fun f(x:integer, y:integer): integer = g(x, y);
fun g(x:integer, y:integer): integer = f(x,y);
fun h(z: integer, y:string): logical = f(z,z)
