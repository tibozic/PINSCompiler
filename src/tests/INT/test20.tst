fun main(_: integer): integer = (
	{ zunanja = 15 },
	gnezdeno(0),
	0
) { where var zunanja: integer;
		fun gnezdeno(_: integer): integer = print_int(zunanja)
	}
