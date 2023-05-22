fun main(_: integer): integer = (
	{a = 0},
	{b = 10},
	{ until a>=b: ({a = a + 1}, print_int(a)) },
	0
) { where var a: integer; var b: integer }
