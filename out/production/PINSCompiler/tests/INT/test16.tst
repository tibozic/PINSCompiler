var test: integer;

fun main(_: integer): integer = odstej(5);

fun odstej(a: integer): integer = (
	{ b = a },
	{ if b == 0 then print_str('Done')
		else (print_int(b), odstej(b-1))
	},
	b
) { where var b: integer }
