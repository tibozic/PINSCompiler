fun main(_: integer): integer = (
	{a = 5},
	{ until a==0: ({a = a - 1}, print_int(a)) },
	0
) { where var a: integer }
