
fun main(_: integer): integer = ({ a = 0 }, { while a < 10: (print_int(a), {a = a + 1}) }, 0)
	{ where var a: integer }
