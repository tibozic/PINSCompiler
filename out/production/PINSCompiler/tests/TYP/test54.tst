fun swap(tab : arr[20] integer, i : integer, j : integer) : integer = (
    {tmp = tab[i]},
    {tab[i] = tab[j]},
    {tab[j] = tmp}
) {where var tmp : integer}
