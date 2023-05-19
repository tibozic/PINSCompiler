
fun main(_: integer): integer = ({ a = 10 }, { while a > 0: (print_int(a), {a = a - 1}) }, 0)
	{ where var a: integer }
