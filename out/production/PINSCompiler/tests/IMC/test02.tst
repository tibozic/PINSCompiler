
fun sum(sez: arr[10] integer) : integer =
	( { for i = 0, 10, 1 : (sez[i] + sum(sez)) }, a )
		{ where var i: integer; var a: integer }
