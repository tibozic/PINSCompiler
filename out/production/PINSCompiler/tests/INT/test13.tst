var sez: arr[10] integer;
var i: integer;
fun main(_: integer): integer = (
	{ for i = 0, 10, 1: (
		{sez[i] = i},
		print_int(sez[i])
	)},
	0
)
