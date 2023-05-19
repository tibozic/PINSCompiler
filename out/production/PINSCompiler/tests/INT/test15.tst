fun main(_: integer): integer = odstej(5);

fun odstej(a: integer): integer = (
	{ if a == 0 then print_str('Done')
		else (print_int(a), odstej(a-1))
	},
	a
)
