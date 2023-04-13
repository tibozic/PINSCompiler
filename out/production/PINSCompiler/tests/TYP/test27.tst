var int: integer;
var log: logical;
var str: string;
fun neki(a: integer, b: string, c: logical): integer = 5;
fun druga(_: logical): integer = neki(int, str, log)
