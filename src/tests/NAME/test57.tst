# program ki presteje stevilo sodih stevil
fun main ( tab : arr[20] integer ) : integer = (
	{for i = 0, 20, 1:
		{if tab[i] % 2 == 0 then
			{stSodih = stSodih + 1}
		}
	}
) {where var stSodih : integer}
!failure:
99


